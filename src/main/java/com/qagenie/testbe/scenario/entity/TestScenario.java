package com.qagenie.testbe.scenario.entity;

import com.qagenie.testbe.common.audit.AuditableEntity;
import com.qagenie.testbe.application.entity.Application;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Setter
@Entity
@Table(name = "TEST_SCENARIO")
public class TestScenario extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SCENARIO_ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "APPLICATION_ID", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Application application;

    /**
     * The spec version this scenario was generated against, if any (null
     * for manually authored scenarios with no generation source, or
     * scenarios created before this feature existed). Lets the platform
     * answer "which scenarios does this new pending spec version affect?"
     * precisely instead of by guesswork.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SPEC_VERSION_ID")
    private com.qagenie.testbe.application.entity.SpecVersion specVersion;

    @Column(name = "NAME", nullable = false, length = 300)
    private String name;

    @Column(name = "HTTP_METHOD", length = 10)
    private String httpMethod;

    @Column(name = "ENDPOINT", length = 500)
    private String endpoint;

    @Enumerated(EnumType.STRING)
    @Column(name = "SCENARIO_TYPE", nullable = false, length = 20)
    private ScenarioType scenarioType;

    @Enumerated(EnumType.STRING)
    @Column(name = "SOURCE", nullable = false, length = 20)
    private ScenarioSource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "RISK_LEVEL", length = 20)
    private RiskLevel riskLevel;

    @Lob
    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "IS_ACTIVE", nullable = false)
    private boolean active = true;

    /**
     * Set by spec healing when a field this scenario references had a TYPE_CHANGED
     * (not RENAMED/DELETED/ADDED - those are auto-healed in place). A value's type
     * can't be safely auto-converted, so this flags the scenario for a human to check.
     */
    @Column(name = "NEEDS_REVIEW", nullable = false)
    private boolean needsReview = false;

    /** JSON array of {fieldName, type, mandatory} - header parameters, captured at generation time. */
    @Lob
    @Column(name = "HEADER_JSON")
    private String headerJson;

    /** JSON array of {fieldName, type, mandatory} - path parameters, captured at generation time. */
    @Lob
    @Column(name = "PATH_PARAM_JSON")
    private String pathParamJson;

    /** JSON array of {fieldName, type, mandatory} - query parameters, captured at generation time. */
    @Lob
    @Column(name = "REQUEST_PARAM_JSON")
    private String requestParamJson;
}
