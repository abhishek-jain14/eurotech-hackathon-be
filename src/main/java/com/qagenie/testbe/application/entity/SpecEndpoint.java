package com.qagenie.testbe.application.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * Cached, per-endpoint breakdown of a SPEC_VERSION's parsed content - computed
 * once when the version is ingested (uploaded/fetched), so fetch-endpoints and
 * scenario generation don't need to re-parse the raw spec JSON on every call.
 */
@Getter
@Setter
@Entity
@Table(name = "SPEC_ENDPOINT")
public class SpecEndpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SPEC_ENDPOINT_ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "SPEC_VERSION_ID", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private SpecVersion specVersion;

    @Column(name = "PATH", length = 500)
    private String path;

    @Column(name = "HTTP_METHOD", length = 10)
    private String httpMethod;

    @Column(name = "SUMMARY", length = 500)
    private String summary;

    /** JSON array of {fieldName, type, mandatory} - parameters located in the header. */
    @Lob
    @Column(name = "HEADER_JSON")
    private String headerJson;

    /** JSON array of {fieldName, type, mandatory} - path parameters. */
    @Lob
    @Column(name = "PATH_PARAM_JSON")
    private String pathParamJson;

    /** JSON array of {fieldName, type, mandatory} - query parameters. */
    @Lob
    @Column(name = "REQUEST_PARAM_JSON")
    private String requestParamJson;

    /** Raw requestBody schema, cached only so fetch-endpoints keeps full parity with live parsing. */
    @Lob
    @Column(name = "REQUEST_BODY_JSON")
    private String requestBodyJson;
}
