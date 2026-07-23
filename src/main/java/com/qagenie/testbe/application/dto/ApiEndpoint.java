package com.qagenie.testbe.application.dto;

import java.util.List;
import java.util.Map;

public class ApiEndpoint {
    private String path;
    private String httpMethod;
    private String summary;
    private List<ApiParameter> parameters;
    private Map<String, Object> requestBody;
    private Map<String, Object> responses;

    public ApiEndpoint() {}

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getHttpMethod() { return httpMethod; }
    public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public List<ApiParameter> getParameters() { return parameters; }
    public void setParameters(List<ApiParameter> parameters) { this.parameters = parameters; }

    public Map<String, Object> getRequestBody() { return requestBody; }
    public void setRequestBody(Map<String, Object> requestBody) { this.requestBody = requestBody; }

    public Map<String, Object> getResponses() { return responses; }
    public void setResponses(Map<String, Object> responses) { this.responses = responses; }
}