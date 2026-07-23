package com.qagenie.testbe.application.dto;

import java.util.ArrayList;
import java.util.List;

public class ApiSpec {
    private String title;
    private String version;
    private List<ApiEndpoint> endpoints = new ArrayList<>();

    public ApiSpec() {}

    public ApiSpec(String title, String version, List<ApiEndpoint> endpoints) {
        this.title = title;
        this.version = version;
        this.endpoints = endpoints;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public List<ApiEndpoint> getEndpoints() { return endpoints; }
    public void setEndpoints(List<ApiEndpoint> endpoints) { this.endpoints = endpoints; }
}
