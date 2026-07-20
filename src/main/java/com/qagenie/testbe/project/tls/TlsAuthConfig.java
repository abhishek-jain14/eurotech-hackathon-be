package com.qagenie.testbe.project.tls;

/**
 * Deserialized shape of Project.specAuthConfigJson - shared by every
 * Environment/Application under that Project when fetching a spec.
 *  - NONE:        nothing
 *  - BASIC:       username, password
 *  - BEARER:      bearerToken
 *  - API_KEY:     apiKeyHeaderName, apiKeyValue
 *  - MUTUAL_TLS:  keystorePath, keystorePassword, keystoreType,
 *                 truststorePath, truststorePassword, truststoreType
 *
 * NOTE: passwords/tokens here are placeholders for a scaffold. In a real
 * deployment, replace the raw password fields with references into a
 * secrets manager (Vault/AWS Secrets Manager/KMS) and resolve them at
 * fetch time instead of persisting plaintext in this JSON blob.
 */
public class TlsAuthConfig {

    public String username;
    public String password;

    public String bearerToken;

    public String apiKeyHeaderName;
    public String apiKeyValue;

    public String keystorePath;
    public String keystorePassword;
    public String keystoreType = "PKCS12";

    public String truststorePath;
    public String truststorePassword;
    public String truststoreType = "PKCS12";

    public TlsAuthConfig() {
        // Jackson default constructor
    }
}
