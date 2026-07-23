package com.qagenie.testbe.execution.entity;

import com.qagenie.testbe.scenario.entity.TestScenario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * Scenario-level summary for one EXECUTION_RUN - exactly one row per
 * (run, scenario) pair, aggregated from that scenario's per-Test-Data-row
 * EXECUTION_RESULT rows. Lets a caller answer "did scenario X pass overall in
 * run Y" with a single lookup instead of grouping/aggregating EXECUTION_RESULT
 * every time; EXECUTION_RESULT remains the detailed, per-Test-Data-row record
 * (full request/response) that this summarizes.
 */
@Getter
@Setter
@Entity
@Table(name = "EXECUTION_SCENARIO_RESULT", uniqueConstraints = @UniqueConstraint(columnNames = {"RUN_ID", "SCENARIO_ID"}))
public class ExecutionScenarioResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SCENARIO_RESULT_ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "RUN_ID", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ExecutionRun executionRun;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "SCENARIO_ID", nullable = false)
    private TestScenario scenario;

    /** PASS only if every linked Test Data row passed; FAIL if any failed; SKIPPED if none ran. */
    @Enumerated(EnumType.STRING)
    @Column(name = "OVERALL_STATUS", nullable = false, length = 20)
    private ResultStatus overallStatus;

    @Column(name = "TOTAL_TEST_DATA", nullable = false)
    private Integer totalTestData = 0;

    @Column(name = "PASSED_COUNT", nullable = false)
    private Integer passedCount = 0;

    @Column(name = "FAILED_COUNT", nullable = false)
    private Integer failedCount = 0;

    @Column(name = "SKIPPED_COUNT", nullable = false)
    private Integer skippedCount = 0;

    /** Sum of responseTimeMs across every linked EXECUTION_RESULT row. */
    @Column(name = "TOTAL_RESPONSE_TIME_MS")
    private Long totalResponseTimeMs = 0L;
}
