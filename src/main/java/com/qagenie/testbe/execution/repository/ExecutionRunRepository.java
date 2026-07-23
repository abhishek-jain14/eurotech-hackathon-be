package com.qagenie.testbe.execution.repository;

import com.qagenie.testbe.execution.entity.ExecutionRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExecutionRunRepository extends JpaRepository<ExecutionRun, Long> {
    Page<ExecutionRun> findByApplicationId(Long applicationId, Pageable pageable);
    Optional<ExecutionRun> findTopByApplicationIdOrderByStartedAtDesc(Long applicationId);
}
