package com.qagenie.testbe.testdata.entity;

import com.qagenie.testbe.common.audit.AuditableEntity;
import com.qagenie.testbe.application.entity.Application;
import com.qagenie.testbe.scenario.entity.TestScenario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Setter
@Entity
@Table(name = "TEST_DATA")
public class TestData extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TEST_DATA_ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "APPLICATION_ID", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Application application;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "SCENARIO_ID", nullable = false)
    private TestScenario testScenario;

    @Column(name = "RECORD_NAME", nullable = false, length = 150)
    private String recordName;

    @Enumerated(EnumType.STRING)
    @Column(name = "MODE", nullable = false, length = 20)
    private TestDataMode mode;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private TestDataStatus status = TestDataStatus.VALID;

    @Lob
    @Column(name = "FIELDS_JSON")
    private String fieldsJson;

    @Column(name = "SERVICE_NAME", length = 150)
    private String serviceName;

    @Column(name = "END_POINT", length = 500)
    private String endPoint;

    @Column(name = "ENVIRONMENT", length = 100)
    private String environment;

    @Column(name = "HTTP_STATUS_CODE")
    private Integer httpStatusCode;

    @Column(name = "ERROR_CODE", length = 50)
    private String errorCode;

    @Column(name = "ERROR_MSG", length = 1000)
    private String errorMsg;

    /** JSON object of expected response field dot-path -> expected value, asserted best-effort at execution time. */
    @Lob
    @Column(name = "RESPONSE_FIELDS")
    private String responseFields;

    /** Full expected response body JSON (or fragment), asserted best-effort at execution time. */
    @Lob
    @Column(name = "RESPONSE_JSON")
    private String responseJson;
}
