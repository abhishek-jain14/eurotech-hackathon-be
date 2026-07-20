package com.qagenie.testbe.project.entity;

import com.qagenie.testbe.common.audit.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Top-level grouping above Application. A Project holds the shared
 * keystore/truststore (and any basic/bearer/api-key credential) used to
 * FETCH specs for every Application onboarded under it, plus the
 * Environments (Dev/Staging/Prod) those Applications derive their
 * swagger/DOM URL from.
 *
 * Application onboarding flow: pick a Project (gives you its TLS
 * material) -> pick one of its Environments (gives you a base URL) ->
 * the Application's swagger URL is derived as baseUrl + "/" + appName +
 * specPathSuffix, unless the user overwrites it with a custom URL.
 */
@Getter
@Setter
@Entity
@Table(name = "PROJECT")
public class Project extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PROJECT_ID")
    private Long id;

    @Column(name = "NAME", nullable = false, unique = true, length = 150)
    private String name;

    @Column(name = "DESCRIPTION", length = 1000)
    private String description;

    @Column(name = "JIRA_URL", length = 500)
    private String jiraUrl;

    /**
     * Optional fixed suffix appended after the application name when
     * deriving a spec URL, e.g. "/v3/api-docs" or "/swagger.json".
     * Left blank if the convention at this project is just baseUrl/appName
     * with no further suffix.
     */
    @Column(name = "SPEC_PATH_SUFFIX", length = 200)
    private String specPathSuffix;

    /**
     * How the spec-fetch call for ANY application under this project
     * authenticates/secures itself: NONE | BASIC | BEARER | API_KEY | MUTUAL_TLS
     */
    @Column(name = "SPEC_AUTH_TYPE", length = 30)
    private String specAuthType = "NONE";

    /**
     * Opaque reference blob - see project.tls.TlsAuthConfig for shape.
     * Keystore/truststore paths (never raw bytes) plus credential refs.
     */
    @Lob
    @Column(name = "SPEC_AUTH_CONFIG_JSON")
    private String specAuthConfigJson;
}
