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
import java.util.HashMap;
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
import software.amazon.awscdk.services.lambda.Version;
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

    private static final String PRIME_TYPE_ON_DEMAND = "1_ON_DEMAND";
    private static final String PRIME_TYPE_NO_PRIMING = "2_SnapStart_NO_PRIMING";
    private static final String PRIME_TYPE_INVOKE_PRIMING = "3_SnapStart_INVOKE_PRIMING";
    private static final String PRIME_TYPE_CLASS_PRIMING = "4_SnapStart_CLASS_PRIMING";
    private static final String DB_LOADER = "5_DB_LOADER";
    private static final String DB_LOADER_FUNCTION_CODE_PATH = "../software/setup/";
    private static final String PRIMING_FUNCTION_CODE_PATH = "../software/priming/";
    private static final String DB_LOADER_FUNCTION_JAR_NAME = "software-setup-0.1.jar";
    private static final String PRIMING_FUNCTION_JAR_NAME = "software-priming-0.1.jar";
    private static final String COPY_FROM_PATH = "/asset-input/target/";
    private static final String COPY_TO_PATH = "/asset-output/";

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

        var buildOptions = configBuildOptions();

        createFunctions(vpc, buildOptions, databaseUrl, databaseUsername, databasePassword);
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
                PostgresInstanceEngineProps.builder().version(PostgresEngineVersion.VER_16_4).build());

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

    private BundlingOptions.Builder configBuildOptions() {
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

    private void createFunctions(IVpc vpc, BundlingOptions.Builder buildOptions,
            String databaseUrl,
            String databaseUsername,
            String databasePassword) {

        var dbLoaderCode = createCodePackage(buildOptions,
                DB_LOADER_FUNCTION_CODE_PATH,
                DB_LOADER_FUNCTION_JAR_NAME,
                COPY_FROM_PATH,
                COPY_TO_PATH);

        var primingCode = createCodePackage(buildOptions,
                PRIMING_FUNCTION_CODE_PATH,
                PRIMING_FUNCTION_JAR_NAME,
                COPY_FROM_PATH,
                COPY_TO_PATH);

        // Function for initial DB data Loader
        createFunction(vpc, dbLoaderCode, DB_LOADER,
                "software.amazon.awscdk.examples.unicorn.SetupHandler",
                null,
                databaseUrl,
                databaseUsername,
                databasePassword,
                DB_LOADER_FUNCTION_CODE_PATH);

        // Function for ON_DEMAND without enabling SnapStart
        createFunction(vpc, primingCode, PRIME_TYPE_ON_DEMAND,
                "software.amazon.awscdk.examples.unicorn.handler.NoPriming",
                null,
                databaseUrl,
                null,
                databasePassword,
                PRIMING_FUNCTION_CODE_PATH);

        // Function for SnapStart without priming
        createFunction(vpc, primingCode, PRIME_TYPE_NO_PRIMING,
                "software.amazon.awscdk.examples.unicorn.handler.NoPriming",
                SnapStartConf.ON_PUBLISHED_VERSIONS,
                databaseUrl,
                null,
                databasePassword,
                PRIMING_FUNCTION_CODE_PATH);

        // Function for SnapStart with INVOKE priming
        createFunction(vpc, primingCode, PRIME_TYPE_INVOKE_PRIMING,
                "software.amazon.awscdk.examples.unicorn.handler.InvokePriming",
                SnapStartConf.ON_PUBLISHED_VERSIONS,
                databaseUrl,
                null,
                databasePassword,
                PRIMING_FUNCTION_CODE_PATH);

        // Function for SnapStart with CLASS priming
        createFunction(vpc, primingCode, PRIME_TYPE_CLASS_PRIMING,
                "software.amazon.awscdk.examples.unicorn.handler.ClassPriming",
                SnapStartConf.ON_PUBLISHED_VERSIONS,
                databaseUrl,
                null,
                databasePassword,
                PRIMING_FUNCTION_CODE_PATH);

    }

    private void createFunction(IVpc vpc, Code code, String primeType, String handler,
            SnapStartConf snapStartConf,
            String databaseUrl,
            String databaseUsername,
            String databasePassword,
            String functionCodePath) {
        Map<String, String> environmentVariables = new HashMap<>();
        environmentVariables.put("SPRING_DATASOURCE_URL", databaseUrl);
        environmentVariables.put("SPRING_DATASOURCE_PASSWORD", databasePassword);
        if (databaseUsername != null) {
            environmentVariables.put("SPRING_DATABASE_USERNAME", databaseUsername);
        }
        environmentVariables.put("JAVA_TOOL_OPTIONS", "");

        var logGroup = LogGroup.Builder.create(this, "PrimingLogGroup-%s".formatted(primeType))
                .retention(RetentionDays.THREE_DAYS)
                .logGroupName("/aws/lambda/%s".formatted("PrimingLogGroup-%s".formatted(primeType)))
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        var function = Function.Builder.create(this, "PrimingJavaLambdaFunction-%s".formatted(primeType))
                .functionName("PrimingJavaLambdaFunction-%s".formatted(primeType))
                .code(code)
                .handler(handler)
                .snapStart(snapStartConf)
                .architecture(Architecture.ARM_64)
                .memorySize(2048)
                .timeout(Duration.seconds(29))
                .environment(environmentVariables)
                .runtime(Runtime.JAVA_21)
                .vpc(vpc)
                .logGroup(logGroup)
                .build();

        var functionCurrentVersion = function.getCurrentVersion();
        createLambdaRestApiIntegration("PrimingJavaRestApi-%s".formatted(primeType), functionCurrentVersion);
    }

    private void createLambdaRestApiIntegration(String restApiName, Version version) {
        LambdaRestApi.Builder.create(this, restApiName)
                .restApiName(restApiName)
                .handler(version)
                .build();
    }

    private Code createCodePackage(BundlingOptions.Builder buildOptions, String codePath, String jarName,
            String copyFrom, String copyTo) {
        var command = Arrays.asList(
                "/bin/sh",
                "-c",
                String.format("mvn clean install package && cp %s%s %s",
                        copyFrom, jarName, copyTo));

        return Code.fromAsset(codePath, AssetOptions.builder()
                .bundling(buildOptions
                        .command(command)
                        .build())
                .build());
    }

}
