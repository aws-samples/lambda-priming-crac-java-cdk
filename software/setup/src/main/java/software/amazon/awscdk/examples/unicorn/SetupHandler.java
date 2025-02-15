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
package software.amazon.awscdk.examples.unicorn;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SetupHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private static final Logger log = LoggerFactory.getLogger(SetupHandler.class);

    private final String url;

    private final String username;

    private final String password;

    private final Gson gson;

    public SetupHandler() {
        log.info("SetupHandler->started");

        this.url = System.getenv("DATABASE_URL");
        this.username = System.getenv("DATABASE_USERNAME");
        this.password = System.getenv("DATABASE_PASSWORD");

        this.gson = new Gson();

        log.info("SetupHandler->finished");
    }

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {
        log.info("handleRequest->started");

        String message;

        try (var connection = DriverManager.getConnection(url, username, password)) {
            try (var statement = connection.createStatement()) {
                try (var schemaSql = getClass().getClassLoader().getResourceAsStream("schema.sql")) {
                    statement.executeUpdate(IOUtils.toString(schemaSql, Charset.defaultCharset()));
                }

                try (var dataSql = getClass().getClassLoader().getResourceAsStream("data.sql")) {
                    statement.executeUpdate(IOUtils.toString(dataSql, Charset.defaultCharset()));
                }

                message = "Database schema initialized and data loaded";
            }
        } catch (SQLException | IOException exception) {
            log.error("Exception on executeUpdate", exception);

            message = "Error on database schema initialization or data loading";
        }

        log.info("handleRequest->finished");

        JsonObject body = new JsonObject();
        body.addProperty("message", message);

        return APIGatewayV2HTTPResponse.builder().withStatusCode(200).withBody(gson.toJson(body)).build();
    }

}
