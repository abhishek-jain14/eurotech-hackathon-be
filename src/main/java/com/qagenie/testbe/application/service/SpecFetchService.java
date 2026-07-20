package com.qagenie.testbe.application.service;

import com.qagenie.testbe.project.entity.Project;

/**
 * Performs the actual outbound HTTP(S) call to retrieve a spec (OpenAPI/
 * Swagger/GraphQL SDL, or a DOM snapshot for frontend onboarding) from a
 * resolved URL, honoring the owning Project's specAuthType/
 * specAuthConfigJson (including mutual TLS via the Project's shared
 * keystore/truststore).
 */
public interface SpecFetchService {
    String fetchSpecContent(String url, Project project);
}
