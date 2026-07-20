package com.qagenie.testbe.execution.entity;

import com.qagenie.testbe.scenario.entity.TestScenario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "EXECUTION_RESULT")
public class ExecutionResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RESULT_ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "RUN_ID", nullable = false)
    private ExecutionRun executionRun;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "SCENARIO_ID", nullable = false)
    private TestScenario scenario;

    @Enumerated(EnumType.STRING)
    @Column(name = "RESULT_STATUS", nullable = false, length = 20)
    private ResultStatus resultStatus;

    @Column(name = "RESPONSE_TIME_MS")
    private Long responseTimeMs;

    @Lob
    @Column(name = "ERROR_MESSAGE")
    private String errorMessage;
}
