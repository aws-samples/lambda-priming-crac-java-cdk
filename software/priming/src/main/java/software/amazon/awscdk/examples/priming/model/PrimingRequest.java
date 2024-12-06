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
package software.amazon.awscdk.examples.priming.model;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.amazonaws.serverless.proxy.model.AwsProxyRequestContext;
import com.amazonaws.serverless.proxy.model.Headers;
import com.amazonaws.serverless.proxy.model.MultiValuedTreeMap;
import com.amazonaws.serverless.proxy.model.RequestSource;
import com.amazonaws.serverless.proxy.model.SingleValueHeaders;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class PrimingRequest {

    @Expose(serialize = true, deserialize = true)
    private String body;

    @Expose(serialize = true, deserialize = true)
    private String version;

    @Expose(serialize = true, deserialize = true)
    private String resource;

    @Expose(serialize = true, deserialize = true)
    private AwsProxyRequestContext requestContext;

    @Expose(serialize = true, deserialize = true)
    private MultiValuedTreeMap<String, String> multiValueQueryStringParameters = new MultiValuedTreeMap();

    @Expose(serialize = true, deserialize = true)
    private Map<String, String> queryStringParameters;

    @Expose(serialize = true, deserialize = true)
    private Headers multiValueHeaders = new Headers();

    @Expose(serialize = true, deserialize = true)
    private SingleValueHeaders headers;

    @Expose(serialize = true, deserialize = true)
    private Map<String, String> pathParameters = new HashMap();

    @Expose(serialize = true, deserialize = true)
    private String httpMethod;

    @Expose(serialize = true, deserialize = true)
    private Map<String, String> stageVariables = new HashMap();

    @Expose(serialize = true, deserialize = true)
    private String path;

    @Expose(serialize = true, deserialize = true)
    private boolean isBase64Encoded;

    public PrimingRequest() {
    }

    public String getQueryString() {
        StringBuilder params = new StringBuilder("");
        if (this.getMultiValueQueryStringParameters() == null) {
            return "";
        } else {
            Iterator var2 = this.getMultiValueQueryStringParameters().keySet().iterator();

            while (var2.hasNext()) {
                String key = (String) var2.next();
                Iterator var4 = this.getMultiValueQueryStringParameters().get(key).iterator();

                while (var4.hasNext()) {
                    String val = (String) var4.next();
                    String separator = params.length() == 0 ? "?" : "&";
                    params.append(separator + key + "=" + val);
                }
            }

            return params.toString();
        }
    }

    public RequestSource getRequestSource() {
        return this.getRequestContext() != null && this.getRequestContext().getElb() != null ? RequestSource.ALB
                : RequestSource.API_GATEWAY;
    }

    public String getBody() {
        return this.body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getResource() {
        return this.resource;
    }

    public String getVersion() {
        return this.version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public AwsProxyRequestContext getRequestContext() {
        return this.requestContext;
    }

    public void setRequestContext(AwsProxyRequestContext requestContext) {
        this.requestContext = requestContext;
    }

    public MultiValuedTreeMap<String, String> getMultiValueQueryStringParameters() {
        return this.multiValueQueryStringParameters;
    }

    public void setMultiValueQueryStringParameters(MultiValuedTreeMap<String, String> multiValueQueryStringParameters) {
        this.multiValueQueryStringParameters = multiValueQueryStringParameters;
    }

    public Map<String, String> getQueryStringParameters() {
        return this.queryStringParameters;
    }

    public void setQueryStringParameters(Map<String, String> queryStringParameters) {
        this.queryStringParameters = queryStringParameters;
    }

    public Headers getMultiValueHeaders() {
        return this.multiValueHeaders;
    }

    public void setMultiValueHeaders(Headers multiValueHeaders) {
        this.multiValueHeaders = multiValueHeaders;
    }

    public SingleValueHeaders getHeaders() {
        return this.headers;
    }

    public void setHeaders(SingleValueHeaders headers) {
        this.headers = headers;
    }

    public Map<String, String> getPathParameters() {
        return this.pathParameters;
    }

    public void setPathParameters(Map<String, String> pathParameters) {
        this.pathParameters = pathParameters;
    }

    public String getHttpMethod() {
        return this.httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public Map<String, String> getStageVariables() {
        return this.stageVariables;
    }

    public void setStageVariables(Map<String, String> stageVariables) {
        this.stageVariables = stageVariables;
    }

    public String getPath() {
        return this.path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @SerializedName("isBase64Encoded")
    public boolean isBase64Encoded() {
        return this.isBase64Encoded;
    }

    public void setIsBase64Encoded(boolean base64Encoded) {
        this.isBase64Encoded = base64Encoded;
    }
}
