package com.qagenie.testbe.execution.repository;

import com.qagenie.testbe.execution.entity.ExecutionScenarioResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExecutionScenarioResultRepository extends JpaRepository<ExecutionScenarioResult, Long> {
    List<ExecutionScenarioResult> findByExecutionRunId(Long runId);
    Optional<ExecutionScenarioResult> findByExecutionRunIdAndScenarioId(Long runId, Long scenarioId);
}
