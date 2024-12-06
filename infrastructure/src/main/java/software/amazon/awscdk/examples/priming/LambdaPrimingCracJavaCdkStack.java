/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: MIT-0
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package software.amazon.awscdk.examples.priming;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import software.amazon.awscdk.BundlingOptions;
import software.amazon.awscdk.BundlingOutput;
import software.amazon.awscdk.DockerVolume;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigateway.LambdaRestApi;
import software.amazon.awscdk.services.ec2.IVpc;
import software.amazon.awscdk.services.ec2.InstanceClass;
import software.amazon.awscdk.services.ec2.InstanceSize;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ec2.Peer;
import software.amazon.awscdk.services.ec2.Port;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.lambda.Architecture;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.SnapStartConf;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.rds.Credentials;
import software.amazon.awscdk.services.rds.DatabaseInstance;
import software.amazon.awscdk.services.rds.DatabaseInstanceEngine;
import software.amazon.awscdk.services.rds.DatabaseProxy;
import software.amazon.awscdk.services.rds.DatabaseSecret;
import software.amazon.awscdk.services.rds.PostgresEngineVersion;
import software.amazon.awscdk.services.rds.PostgresInstanceEngineProps;
import software.amazon.awscdk.services.rds.ProxyTarget;
import software.amazon.awscdk.services.s3.assets.AssetOptions;
import software.constructs.Construct;

public class LambdaPrimingCracJavaCdkStack extends Stack {

        private static final String PRIME_TYPE_ON_DEMAND = "ON_DEMAND";
        private static final String PRIME_TYPE_NO_PRIMING = "NO_PRIMING";
        private static final String PRIME_TYPE_MANUAL_PRIMING = "MANUAL_PRIMING";
        private static final String PRIME_TYPE_AUTOMATIC_PRIMING = "AUTOMATIC_PRIMING";

        public LambdaPrimingCracJavaCdkStack(final Construct scope, final String id) {
                this(scope, id, null);
        }

        public LambdaPrimingCracJavaCdkStack(final Construct scope, final String id, final StackProps props) {
                super(scope, id, props);

                var vpc = createVpc();
                var securityGroupDatabase = createSecurityGroupDatabase(vpc);
                var databaseSecret = createDatabaseSecret();
                var databaseInstance = createDatabaseInstance(vpc, securityGroupDatabase, databaseSecret);
                var databaseProxy = createDatabaseProxy(vpc, databaseSecret, securityGroupDatabase, databaseInstance);

                var databaseUsername = databaseSecret.secretValueFromJson("username").unsafeUnwrap();

                var databaseUrl = "jdbc:postgresql://%s:%s/%s".formatted(
                                databaseProxy.getEndpoint(),
                                databaseInstance.getDbInstanceEndpointPort(),
                                databaseUsername);

                var databasePassword = databaseSecret.secretValueFromJson("password")
                                .unsafeUnwrap()
                                .toString();

                var bundlingOptionsBuilder = createBundlingOptionsBuilder();

                createFunctionSetup(vpc, bundlingOptionsBuilder, databaseUrl, databaseUsername, databasePassword);
                createFunctionPrimingAll(vpc, bundlingOptionsBuilder, databaseUrl, databasePassword);
        }

        private Vpc createVpc() {
                return Vpc.Builder.create(this, "PrimingCracJavaVpc")
                                .natGateways(0)
                                .build();
        }

        private SecurityGroup createSecurityGroupDatabase(IVpc vpc) {
                var securityGroup = SecurityGroup.Builder.create(this, "PrimingCracJavaSGDatabase")
                                .securityGroupName("PrimingCracJavaSGDatabase")
                                .allowAllOutbound(Boolean.FALSE)
                                .vpc(vpc)
                                .build();

                securityGroup.addIngressRule(
                                Peer.ipv4("10.0.0.0/16"),
                                Port.tcp(5432),
                                "Allow database traffic from local network");

                return securityGroup;
        }

        private DatabaseSecret createDatabaseSecret() {
                return DatabaseSecret.Builder
                                .create(this, "PrimingCracJavaDatabaseSecret")
                                .secretName("PrimingCracJavaDatabaseSecret")
                                .username("postgres").build();
        }

        private DatabaseInstance createDatabaseInstance(IVpc vpc, SecurityGroup securityGroup,
                        DatabaseSecret databaseSecret) {
                var engine = DatabaseInstanceEngine.postgres(
                                PostgresInstanceEngineProps.builder().version(PostgresEngineVersion.VER_16_2).build());

                var databaseInstance = DatabaseInstance.Builder.create(this, "PrimingCracJavaDB")
                                .engine(engine)
                                .vpc(vpc)
                                .allowMajorVersionUpgrade(true)
                                .backupRetention(Duration.days(0))
                                .databaseName("priming_crac_java_db")
                                .instanceIdentifier("PrimingCracJavaDBInstance")
                                .instanceType(InstanceType.of(InstanceClass.BURSTABLE3, InstanceSize.MEDIUM))
                                .vpcSubnets(SubnetSelection.builder()
                                                .subnetType(SubnetType.PRIVATE_ISOLATED)
                                                .build())
                                .securityGroups(List.of(securityGroup))
                                .credentials(Credentials.fromSecret(databaseSecret))
                                .build();

                return databaseInstance;
        }

        private DatabaseProxy createDatabaseProxy(IVpc vpc, DatabaseSecret databaseSecret, SecurityGroup securityGroup,
                        DatabaseInstance databaseInstance) {
                return DatabaseProxy.Builder.create(this, "PrimingCracJavaDBProxy")
                                .vpc(vpc)
                                .secrets(List.of(databaseSecret))
                                .securityGroups(List.of(securityGroup))
                                .iamAuth(Boolean.FALSE)
                                .proxyTarget(ProxyTarget.fromInstance(databaseInstance))
                                .build();
        }

        private BundlingOptions.Builder createBundlingOptionsBuilder() {
                return BundlingOptions.builder()
                                .image(Runtime.JAVA_21.getBundlingImage())
                                .volumes(Collections.singletonList(
                                                DockerVolume.builder()
                                                                .hostPath("%s/.m2/".formatted(
                                                                                System.getProperty("user.home")))
                                                                .containerPath("/root/.m2/")
                                                                .build()))
                                .user("root")
                                .outputType(BundlingOutput.ARCHIVED);
        }

        private Code createCodeSetup(BundlingOptions.Builder bundlingOptionsBuilder) {
                var command = Arrays.asList(
                                "/bin/sh",
                                "-c",
                                "mvn clean install package && cp /asset-input/target/software-setup-0.1.jar /asset-output/");

                return Code.fromAsset("../software/setup/", AssetOptions.builder()
                                .bundling(bundlingOptionsBuilder
                                                .command(command)
                                                .build())
                                .build());
        }

        private void createFunctionSetup(IVpc vpc, BundlingOptions.Builder bundlingOptionsBuilder, String databaseUrl,
                        String databaseUsername,
                        String databasePassword) {
                var logGroup = LogGroup.Builder.create(this, "PrimingCracJavaLogGroupSetup")
                                .retention(RetentionDays.THREE_DAYS)
                                .logGroupName("/aws/lambda/%s".formatted("PrimingCracJavaLogGroupSetup"))
                                .removalPolicy(RemovalPolicy.DESTROY)
                                .build();

                var code = createCodeSetup(bundlingOptionsBuilder);

                var function = Function.Builder.create(this, "PrimingCracJavaFunctionSetup")
                                .code(code)
                                .handler("software.amazon.awscdk.examples.priming.SetupHandler")
                                .architecture(Architecture.ARM_64)
                                .memorySize(1024)
                                .timeout(Duration.seconds(900))
                                .environment(
                                                Map.of(
                                                                "DATABASE_URL", databaseUrl, "DATABASE_USERNAME",
                                                                databaseUsername, "DATABASE_PASSWORD",
                                                                databasePassword))
                                .runtime(Runtime.JAVA_21)
                                .vpc(vpc)
                                .logGroup(logGroup)
                                .build();

                createLambdaRestApi("PrimingCracJavaRestApiSetup", function);
        }

        private Code createCodePriming(BundlingOptions.Builder bundlingOptionsBuilder) {
                var command = Arrays.asList(
                                "/bin/sh",
                                "-c",
                                "mvn clean install package && cp /asset-input/target/software-priming-0.1.jar /asset-output/");

                return Code.fromAsset("../software/priming/", AssetOptions.builder()
                                .bundling(bundlingOptionsBuilder
                                                .command(command)
                                                .build())
                                .build());
        }

        private void createFunctionPrimingAll(IVpc vpc, BundlingOptions.Builder bundlingOptionsBuilder,
                        String databaseUrl,
                        String databasePassword) {
                var code = createCodePriming(bundlingOptionsBuilder);

                createFunctionPriming(vpc, code, PRIME_TYPE_ON_DEMAND,
                                "software.amazon.awscdk.examples.priming.handler.NoPriming", null, databaseUrl,
                                databasePassword);

                createFunctionPriming(vpc, code, PRIME_TYPE_NO_PRIMING,
                                "software.amazon.awscdk.examples.priming.handler.NoPriming",
                                SnapStartConf.ON_PUBLISHED_VERSIONS,
                                databaseUrl,
                                databasePassword);

                createFunctionPriming(vpc, code, PRIME_TYPE_MANUAL_PRIMING,
                                "software.amazon.awscdk.examples.priming.handler.ManualPriming",
                                SnapStartConf.ON_PUBLISHED_VERSIONS,
                                databaseUrl,
                                databasePassword);

                createFunctionPriming(vpc, code, PRIME_TYPE_AUTOMATIC_PRIMING,
                                "software.amazon.awscdk.examples.priming.handler.AutomaticPriming",
                                SnapStartConf.ON_PUBLISHED_VERSIONS,
                                databaseUrl,
                                databasePassword);
        }

        private void createFunctionPriming(IVpc vpc, Code code, String primeType, String handler,
                        SnapStartConf snapStartConf, String databaseUrl,
                        String databasePassword) {
                var logGroup = LogGroup.Builder.create(this, "PrimingCracJavaLogGroupPriming-%s".formatted(primeType))
                                .retention(RetentionDays.THREE_DAYS)
                                .logGroupName("/aws/lambda/%s-%s".formatted("PrimingCracJavaLogGroupPriming",
                                                primeType))
                                .removalPolicy(RemovalPolicy.DESTROY)
                                .build();

                var function = Function.Builder.create(this, "PrimingCracJavaFunctionPriming-%s".formatted(primeType))
                                .code(code)
                                .handler(handler)
                                .snapStart(snapStartConf)
                                .architecture(Architecture.ARM_64)
                                .memorySize(2048)
                                .timeout(Duration.seconds(29))
                                .environment(
                                                Map.of(
                                                                "SPRING_DATASOURCE_URL", databaseUrl,
                                                                "SPRING_DATASOURCE_PASSWORD",
                                                                databasePassword))
                                .runtime(Runtime.JAVA_21)
                                .vpc(vpc)
                                .logGroup(logGroup)
                                .build();

                createLambdaRestApi("PrimingCracJavaRestApiPriming-%s".formatted(primeType), function);
        }

        private void createLambdaRestApi(String restApiName, Function function) {
                LambdaRestApi.Builder.create(this, restApiName)
                                .restApiName(restApiName)
                                .handler(function.getCurrentVersion())
                                .build();
        }

}
