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
package software.amazon.awscdk.examples.priming.handler;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.Gson;

import software.amazon.awscdk.examples.priming.PrimingApplication;
import software.amazon.awscdk.examples.priming.dto.PrimingDto;
import software.amazon.awscdk.examples.priming.model.Priming;
import software.amazon.awscdk.examples.priming.model.PrimingRequest;
import software.amazon.awscdk.examples.priming.model.PrimingResponse;
import software.amazon.awscdk.examples.priming.service.PrimingService;

public class NoPriming implements RequestHandler<PrimingRequest, PrimingResponse> {

    private static final Logger log = LoggerFactory.getLogger(NoPriming.class);

    private final PrimingService primingService;

    private final Gson gson;

    public NoPriming() {
        log.info("NoPriming->started");

        ConfigurableApplicationContext configurableApplicationContext = SpringApplication.run(PrimingApplication.class,
                new String[] {});

        this.primingService = configurableApplicationContext.getBean(PrimingService.class);
        this.gson = configurableApplicationContext.getBean(Gson.class);

        log.info("NoPriming->finished");
    }

    @Override
    public PrimingResponse handleRequest(PrimingRequest request, Context context) {
        log.info("handleRequest->started");

        var awsLambdaInitializationType = System.getenv("AWS_LAMBDA_INITIALIZATION_TYPE");
        log.info("awsLambdaInitializationType: {}", awsLambdaInitializationType);

        var primingResponse = new PrimingResponse();
        primingResponse.setBody(gson.toJson(getPrimingDtos()));
        primingResponse.setStatusCode(200);

        log.info("handleRequest->finished");

        return primingResponse;
    }

    public List<PrimingDto> getPrimingDtos() {
        log.info("getPrimingDtos->started");

        List<Priming> primings = primingService.read();

        List<PrimingDto> primingDtos = primings.stream()
                .map(priming -> new PrimingDto(priming.id(), priming.name(),
                        priming.type()))
                .toList();

        log.info("getPrimingDtos->finished");

        return primingDtos;
    }

}
