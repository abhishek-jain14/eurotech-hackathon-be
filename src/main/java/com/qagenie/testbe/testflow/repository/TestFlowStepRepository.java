package com.qagenie.testbe.testflow.repository;

import com.qagenie.testbe.testflow.entity.TestFlowStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TestFlowStepRepository extends JpaRepository<TestFlowStep, Long> {
    List<TestFlowStep> findByScenario_IdIn(List<Long> scenarioIds);
}
