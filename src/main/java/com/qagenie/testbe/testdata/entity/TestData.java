package com.qagenie.testbe.testdata.entity;

import com.qagenie.testbe.common.audit.AuditableEntity;
import com.qagenie.testbe.application.entity.Application;
import com.qagenie.testbe.scenario.entity.TestScenario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

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
}
