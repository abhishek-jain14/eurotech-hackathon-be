package com.qagenie.testbe.scenario.dto;

import com.qagenie.testbe.application.dto.ApiEndpoint;

import java.util.Map;

public class ApiTestData {
    private ApiEndpoint endpoint;
    private Map<String, String> headers;
    private Map<String, Object> pathOrQueryParams;
    private Map<String, Object> requestBodyValues;
    private int expectedStatusCode;
    private String expectedResponseBody;

    public ApiTestData() {}

    public ApiTestData(ApiEndpoint endpoint, Map<String, String> headers,
                       Map<String, Object> pathOrQueryParams,
                       Map<String, Object> requestBodyValues,
                       int expectedStatusCode, String expectedResponseBody) {
        this.endpoint = endpoint;
        this.headers = headers;
        this.pathOrQueryParams = pathOrQueryParams;
        this.requestBodyValues = requestBodyValues;
        this.expectedStatusCode = expectedStatusCode;
        this.expectedResponseBody = expectedResponseBody;
    }

    // Getters and Setters
    public ApiEndpoint getEndpoint() { return endpoint; }
    public void setEndpoint(ApiEndpoint endpoint) { this.endpoint = endpoint; }

    public Map<String, String> getHeaders() { return headers; }
    public void setHeaders(Map<String, String> headers) { this.headers = headers; }

    public Map<String, Object> getPathOrQueryParams() { return pathOrQueryParams; }
    public void setPathOrQueryParams(Map<String, Object> pathOrQueryParams) { this.pathOrQueryParams = pathOrQueryParams; }

    public Map<String, Object> getRequestBodyValues() { return requestBodyValues; }
    public void setRequestBodyValues(Map<String, Object> requestBodyValues) { this.requestBodyValues = requestBodyValues; }

    public int getExpectedStatusCode() { return expectedStatusCode; }
    public void setExpectedStatusCode(int expectedStatusCode) { this.expectedStatusCode = expectedStatusCode; }

    public String getExpectedResponseBody() { return expectedResponseBody; }
    public void setExpectedResponseBody(String expectedResponseBody) { this.expectedResponseBody = expectedResponseBody; }
}