package com.qagenie.testbe.scenario.repository;

import com.qagenie.testbe.scenario.entity.TestScenario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestScenarioRepository extends JpaRepository<TestScenario, Long> {
    Page<TestScenario> findByApplicationId(Long applicationId, Pageable pageable);
    java.util.List<TestScenario> findByApplicationId(Long applicationId);
}
