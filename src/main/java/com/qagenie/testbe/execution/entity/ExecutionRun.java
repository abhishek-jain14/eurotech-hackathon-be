package com.qagenie.testbe.execution.entity;

import com.qagenie.testbe.common.audit.AuditableEntity;
import com.qagenie.testbe.environment.entity.EnvironmentConfig;
import com.qagenie.testbe.application.entity.Application;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "EXECUTION_RUN")
public class ExecutionRun extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RUN_ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "APPLICATION_ID", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Application application;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ENVIRONMENT_ID", nullable = false)
    private EnvironmentConfig environment;

    @Column(name = "SUITE_NAME", length = 150)
    private String suiteName;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private ExecutionStatus status = ExecutionStatus.QUEUED;

    @Column(name = "STARTED_AT")
    private Instant startedAt;

    @Column(name = "COMPLETED_AT")
    private Instant completedAt;

    @Column(name = "TOTAL_SCENARIOS")
    private Integer totalScenarios = 0;

    @Column(name = "PASSED_COUNT")
    private Integer passedCount = 0;

    @Column(name = "FAILED_COUNT")
    private Integer failedCount = 0;
}
