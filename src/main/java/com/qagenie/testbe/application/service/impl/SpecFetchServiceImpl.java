package com.qagenie.testbe.application.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qagenie.testbe.application.service.SpecFetchService;
import com.qagenie.testbe.common.exception.BusinessException;
import com.qagenie.testbe.project.entity.Project;
import com.qagenie.testbe.project.tls.TlsAuthConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyStore;
import java.time.Duration;
import java.util.Base64;

/**
 * Real outbound fetch used by ApplicationServiceImpl#fetchSpecFromUrl.
 * Builds a fresh HttpClient per call, scoped to the owning PROJECT's
 * keystore/truststore when specAuthType=MUTUAL_TLS - shared by every
 * Application under that Project, so onboarding a second application
 * under the same Project never requires re-uploading certificates.
 */
@Service
public class SpecFetchServiceImpl implements SpecFetchService {

    private static final Logger log = LoggerFactory.getLogger(SpecFetchServiceImpl.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Duration TIMEOUT = Duration.ofSeconds(20);

    @Override
    public String fetchSpecContent(String url, Project project) {
        if (url == null || url.isBlank()) {
            throw new BusinessException("No URL resolved to fetch the spec from", "SPEC_URL_MISSING");
        }

        TlsAuthConfig authConfig = parseAuthConfig(project.getSpecAuthConfigJson());
        String authType = project.getSpecAuthType() == null ? "NONE" : project.getSpecAuthType();

        HttpClient client = buildHttpClient(authType, authConfig, project.getName());

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(TIMEOUT)
                .GET();

        applyAuthHeaders(requestBuilder, authType, authConfig);

        try {
            log.info("Fetching spec: url={}, project={}, authType={}", url, project.getName(), authType);

            HttpResponse<String> response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                throw new BusinessException(
                        "Spec endpoint returned HTTP " + response.statusCode() + " for " + url,
                        "SPEC_FETCH_HTTP_ERROR");
            }
            return response.body();

        } catch (SSLHandshakeException e) {
            log.warn("TLS handshake failed fetching spec url={}: {}", url, e.getMessage());
            throw new BusinessException(
                    "TLS handshake failed reaching " + url + ". This endpoint likely requires a client " +
                    "certificate (mutual TLS) or is signed by an internal CA not in the default trust store. " +
                    "Configure a keystore/truststore for project '" + project.getName() + "' via " +
                    "POST /api/v1/projects/{id}/tls-config with authType=MUTUAL_TLS, then retry.",
                    "TLS_REQUIRED");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Spec fetch failed for url={}", url, e);
            throw new BusinessException("Unable to fetch spec: " + e.getMessage(), "SPEC_FETCH_ERROR");
        }
    }

    private HttpClient buildHttpClient(String authType, TlsAuthConfig authConfig, String projectName) {
        HttpClient.Builder builder = HttpClient.newBuilder().connectTimeout(TIMEOUT);

        if ("MUTUAL_TLS".equals(authType)) {
            if (authConfig == null || authConfig.keystorePath == null) {
                throw new BusinessException(
                        "Project '" + projectName + "' is set to MUTUAL_TLS but has no keystore configured. " +
                        "Upload one via POST /api/v1/projects/{id}/tls-config first.",
                        "TLS_CONFIG_MISSING");
            }
            builder.sslContext(buildSslContext(authConfig, projectName));
        }
        return builder.build();
    }

    private SSLContext buildSslContext(TlsAuthConfig config, String projectName) {
        try {
            KeyManagerFactory kmf = null;
            if (config.keystorePath != null) {
                KeyStore keyStore = KeyStore.getInstance(config.keystoreType);
                try (FileInputStream fis = new FileInputStream(config.keystorePath)) {
                    keyStore.load(fis, config.keystorePassword.toCharArray());
                }
                kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                kmf.init(keyStore, config.keystorePassword.toCharArray());
            }

            TrustManagerFactory tmf = null;
            if (config.truststorePath != null) {
                KeyStore trustStore = KeyStore.getInstance(config.truststoreType);
                try (FileInputStream fis = new FileInputStream(config.truststorePath)) {
                    trustStore.load(fis, config.truststorePassword.toCharArray());
                }
                tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(trustStore);
            }

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(
                    kmf != null ? kmf.getKeyManagers() : null,
                    tmf != null ? tmf.getTrustManagers() : null,
                    new java.security.SecureRandom()
            );
            return sslContext;
        } catch (Exception e) {
            throw new BusinessException(
                    "Unable to build SSL context for project '" + projectName + "' from configured keystore/truststore: " + e.getMessage(),
                    "TLS_CONFIG_INVALID");
        }
    }

    private void applyAuthHeaders(HttpRequest.Builder requestBuilder, String authType, TlsAuthConfig config) {
        if (config == null) return;
        switch (authType) {
            case "BASIC" -> {
                String creds = Base64.getEncoder().encodeToString((config.username + ":" + config.password).getBytes());
                requestBuilder.header("Authorization", "Basic " + creds);
            }
            case "BEARER" -> requestBuilder.header("Authorization", "Bearer " + config.bearerToken);
            case "API_KEY" -> requestBuilder.header(config.apiKeyHeaderName, config.apiKeyValue);
            default -> { /* NONE / MUTUAL_TLS need no extra header */ }
        }
    }

    private TlsAuthConfig parseAuthConfig(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return MAPPER.readValue(json, TlsAuthConfig.class);
        } catch (Exception e) {
            throw new BusinessException("Stored spec auth config is corrupt: " + e.getMessage(), "TLS_CONFIG_INVALID");
        }
    }
}
