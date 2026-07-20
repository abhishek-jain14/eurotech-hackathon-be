package com.qagenie.testbe.testflow.repository;

import com.qagenie.testbe.testflow.entity.TestFlowStep;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestFlowStepRepository extends JpaRepository<TestFlowStep, Long> {
}
