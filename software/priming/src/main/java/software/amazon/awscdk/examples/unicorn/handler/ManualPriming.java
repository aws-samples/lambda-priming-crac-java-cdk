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
import com.google.gson.Gson;

import software.amazon.awscdk.examples.unicorn.UnicornApplication;
import software.amazon.awscdk.examples.unicorn.dto.UnicornDto;
import software.amazon.awscdk.examples.unicorn.model.Unicorn;
import software.amazon.awscdk.examples.unicorn.model.UnicornRequest;
import software.amazon.awscdk.examples.unicorn.model.UnicornResponse;
import software.amazon.awscdk.examples.unicorn.service.UnicornService;

public class ManualPriming implements RequestHandler<UnicornRequest, UnicornResponse>, Resource {

    private static final Logger log = LoggerFactory.getLogger(ManualPriming.class);

    private final UnicornService unicornService;

    private final Gson gson;

    public ManualPriming() {
        log.info("ManualPriming->started");

        ConfigurableApplicationContext configurableApplicationContext = SpringApplication.run(UnicornApplication.class,
                new String[] {});

        this.unicornService = configurableApplicationContext.getBean(UnicornService.class);
        this.gson = configurableApplicationContext.getBean(Gson.class);

        Core.getGlobalContext().register(this);

        log.info("ManualPriming->finished");
    }

    @Override
    public UnicornResponse handleRequest(UnicornRequest request, Context context) {
        log.info("handleRequest->started");

        var awsLambdaInitializationType = System.getenv("AWS_LAMBDA_INITIALIZATION_TYPE");
        log.info("awsLambdaInitializationType: {}", awsLambdaInitializationType);

        var primingResponse = new UnicornResponse();
        primingResponse.setBody(gson.toJson(getPrimingDtos()));
        primingResponse.setStatusCode(200);

        log.info("handleRequest->finished");

        return primingResponse;
    }

    @Override
    public void beforeCheckpoint(org.crac.Context<? extends Resource> context)
            throws Exception {
        log.info("beforeCheckpoint->started");

        UnicornRequest unicornRequest = new UnicornRequest();
        unicornRequest.setHttpMethod("GET");
        unicornRequest.setPath("/priming");

        handleRequest(unicornRequest, null);

        log.info("beforeCheckpoint->finished");
    }

    @Override
    public void afterRestore(org.crac.Context<? extends Resource> context) throws Exception {
        log.info("afterRestore->started");
        log.info("afterRestore->finished");
    }

    public List<UnicornDto> getPrimingDtos() {
        log.info("getPrimingDtos->started");

        List<Unicorn> unicorns = unicornService.read();

        List<UnicornDto> unicornDtos = unicorns.stream()
                .map(priming -> new UnicornDto(priming.id(), priming.name(),
                        priming.type()))
                .toList();

        log.info("getPrimingDtos->finished");

        return unicornDtos;
    }

}
