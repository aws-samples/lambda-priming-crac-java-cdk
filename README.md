# Priming Lambda functions using CRac runtime hooks for Java

The content of this repository showcases ways to prime and improve the latency of your Java Lambda functions using [runtime hooks](https://docs.aws.amazon.com/lambda/latest/dg/snapstart-runtime-hooks.html) supported by [AWS Lambda](https://aws.amazon.com/lambda/).
Runtime hooks are available as part of the open source CRaC (Coordinated Restore at Checkpoint) project.
You can use the **"beforeCheckpoint()"** hook to run code immediately before a snapshot is taken, and use the **"afterRestore()"** hook to run code immediately after restoring a snapshot.

Priming is a mechanism to further optimize and enhance the performance of Lambda function performance, especially for use cases requiring consistent low latency, such as APIs and real-time data processing services.

## Requirements

* [Create an AWS account](https://portal.aws.amazon.com/gp/aws/developer/registration/index.html) if you do not already have one and log in. The IAM user that you use must have sufficient permissions to make necessary AWS service calls and manage AWS resources.
* [AWS CLI](https://docs.aws.amazon.com/cli/latest/userguide/install-cliv2.html) installed and configured
* [Git Installed](https://git-scm.com/book/en/v2/Getting-Started-Installing-Git)
* [AWS Cloud Development Kit](https://docs.aws.amazon.com/cdk/v2/guide/getting_started.html) (AWS CDK) installed
* [Java 21](https://aws.amazon.com/corretto/)
* [Maven](https://maven.apache.org/)
* [curl](https://curl.se/)

## Deployment Instructions

1. Create a new directory, navigate to that directory in a terminal and clone the GitHub repository:
    ``` 
    git clone https://github.com/aws-samples/lambda-priming-crac-java-cdk
    ```

2. Change directory to the infrastructure directory:
    ```
   cd lambda-priming-crac-java-cdk/infrastructure
   ```

3. Deploy the stack:
    ```
   cdk deploy --require-approval never --all 2>&1 | tee cdk_output.txt
   ```

## Testing

1. Extract URLs:
    ```
    AUTOMATICPRIMING_URL=$(grep -oE 'https://[a-zA-Z0-9.-]+\.execute-api\.[a-zA-Z0-9-]+\.amazonaws\.com/prod/' "cdk_output.txt" | head -n 1) \
    MANUALPRIMING_URL=$(grep -oE 'https://[a-zA-Z0-9.-]+\.execute-api\.[a-zA-Z0-9-]+\.amazonaws\.com/prod/' "cdk_output.txt" | head -n 2 | tail -n 1) \
    NOPRIMING_URL=$(grep -oE 'https://[a-zA-Z0-9.-]+\.execute-api\.[a-zA-Z0-9-]+\.amazonaws\.com/prod/' "cdk_output.txt" | head -n 3 | tail -n 1) \
    ONDEMAND_URL=$(grep -oE 'https://[a-zA-Z0-9.-]+\.execute-api\.[a-zA-Z0-9-]+\.amazonaws\.com/prod/' "cdk_output.txt" | head -n 4 | tail -n 1) \
    SETUP_URL=$(grep -oE 'https://[a-zA-Z0-9.-]+\.execute-api\.[a-zA-Z0-9-]+\.amazonaws\.com/prod/' "cdk_output.txt" | head -n 5 | tail -n 1)
   ```

2. Initialize de database:
   ```
   curl -X GET "$SETUP_URL"
   ```
   
3. It should return:
   ```
   {"message":"Database schema initialized and data loaded"}
   ```
4. Run load test:
   ```
   artillery run -t "$ONDEMAND_URL" -v '{ "url": "/unicorn" }' ./loadtest.yaml && \
   artillery run -t "$NOPRIMING_URL" -v '{ "url": "/unicorn" }' ./loadtest.yaml && \
   artillery run -t "$MANUALPRIMING_URL" -v '{ "url": "/unicorn" }' ./loadtest.yaml && \
   artillery run -t "$AUTOMATICPRIMING_URL" -v '{ "url": "/unicorn" }' ./loadtest.yaml
   ```

## Measuring the results

You can use the following AWS CloudWatch Insights query to measure the duration your SnapStart Lambda function.

```
filter @type = "REPORT"
  | parse @log /\d+:\/aws\/lambda\/(?<function>.*)/
  | parse @message /Restore Duration: (?<restoreDuration>.*?) ms/
  | stats
count(*) as invocations,
pct(@duration+coalesce(@initDuration,0)+coalesce(restoreDuration,0), 50) as p50,
pct(@duration+coalesce(@initDuration,0)+coalesce(restoreDuration,0), 90) as p90,
pct(@duration+coalesce(@initDuration,0)+coalesce(restoreDuration,0), 99) as p99,
pct(@duration+coalesce(@initDuration,0)+coalesce(restoreDuration,0), 99.9) as p99.9
group by function, (ispresent(@initDuration) or ispresent(restoreDuration)) as coldstart
  | sort by function asc | sort by coldstart desc
```

Select the log groups bellow:

```
/aws/lambda/PrimingCracJavaLogGroupPriming-ON_DEMAND
/aws/lambda/PrimingCracJavaLogGroupPriming-NO_PRIMING
/aws/lambda/PrimingCracJavaLogGroupPriming-MANUAL_PRIMING
/aws/lambda/PrimingCracJavaLogGroupPriming-AUTOMATIC_PRIMING
```

## Clean-up

```
cdk destroy --require-approval never --all
```