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
package software.amazon.awscdk.examples.unicorn.model;

import java.util.Map;

import com.amazonaws.serverless.proxy.model.Headers;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class UnicornResponse {

    @Expose(serialize = true, deserialize = true)
    private int statusCode;

    @Expose(serialize = true, deserialize = true)
    private String statusDescription;

    @Expose(serialize = true, deserialize = true)
    private Map<String, String> headers;

    @Expose(serialize = true, deserialize = true)
    private Headers multiValueHeaders;

    @Expose(serialize = true, deserialize = true)
    private String body;

    @Expose(serialize = true, deserialize = true)
    private boolean isBase64Encoded;

    public UnicornResponse() {
    }

    public UnicornResponse(int statusCode) {
        this(statusCode, (Headers) null);
    }

    public UnicornResponse(int statusCode, Headers headers) {
        this(statusCode, headers, (String) null);
    }

    public UnicornResponse(int statusCode, Headers headers, String body) {
        this.statusCode = statusCode;
        this.multiValueHeaders = headers;
        this.body = body;
    }

    public void addHeader(String key, String value) {
        if (this.multiValueHeaders == null) {
            this.multiValueHeaders = new Headers();
        }

        this.multiValueHeaders.add(key, value);
    }

    public int getStatusCode() {
        return this.statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public Map<String, String> getHeaders() {
        return this.headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public Headers getMultiValueHeaders() {
        return this.multiValueHeaders;
    }

    public void setMultiValueHeaders(Headers multiValueHeaders) {
        this.multiValueHeaders = multiValueHeaders;
    }

    public String getBody() {
        return this.body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    @SerializedName("isBase64Encoded")
    public boolean isBase64Encoded() {
        return this.isBase64Encoded;
    }

    public void setBase64Encoded(boolean base64Encoded) {
        this.isBase64Encoded = base64Encoded;
    }

    public String getStatusDescription() {
        return this.statusDescription;
    }

    public void setStatusDescription(String statusDescription) {
        this.statusDescription = statusDescription;
    }
}
