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
package software.amazon.awscdk.examples.unicorn.handler;

import java.util.List;

import org.crac.Core;
import org.crac.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.google.gson.Gson;

import software.amazon.awscdk.examples.unicorn.UnicornApplication;
import software.amazon.awscdk.examples.unicorn.model.Unicorn;
import software.amazon.awscdk.examples.unicorn.service.UnicornService;

public class InvokePriming implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse>, Resource {

    private static final Logger log = LoggerFactory.getLogger(InvokePriming.class);

    private final UnicornService unicornService;

    private final Gson gson;

    public InvokePriming() {
        log.info("InvokePriming->started");

        ConfigurableApplicationContext configurableApplicationContext = SpringApplication.run(UnicornApplication.class,
                new String[] {});

        this.unicornService = configurableApplicationContext.getBean(UnicornService.class);
        this.gson = configurableApplicationContext.getBean(Gson.class);

        Core.getGlobalContext().register(this);

        log.info("InvokePriming->finished");
    }

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {
        log.info("handleRequest->started");

        var awsLambdaInitializationType = System.getenv("AWS_LAMBDA_INITIALIZATION_TYPE");
        log.info("awsLambdaInitializationType: {}", awsLambdaInitializationType);

        var unicorns = getUnicorns();
        var body = gson.toJson(unicorns);

        log.info("handleRequest->finished");

        return APIGatewayV2HTTPResponse.builder().withStatusCode(200).withBody(body).build();
    }

    @Override
    public void beforeCheckpoint(org.crac.Context<? extends Resource> context)
            throws Exception {
        log.info("beforeCheckpoint->started");

        var event = APIGatewayV2HTTPEvent.builder().build();

        handleRequest(event, null);

        log.info("beforeCheckpoint->finished");
    }

    @Override
    public void afterRestore(org.crac.Context<? extends Resource> context) throws Exception {
        log.info("afterRestore->started");
        log.info("afterRestore->finished");
    }

    public List<Unicorn> getUnicorns() {
        log.info("getUnicorns->started");

        List<Unicorn> unicorns = unicornService.read();

        log.info("getUnicorns->finished");

        return unicorns;
    }

}
