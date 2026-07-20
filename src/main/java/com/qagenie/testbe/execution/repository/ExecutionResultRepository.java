package com.qagenie.testbe.execution.repository;

import com.qagenie.testbe.execution.entity.ExecutionResult;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExecutionResultRepository extends JpaRepository<ExecutionResult, Long> {
    List<ExecutionResult> findByExecutionRunId(Long runId);
}
