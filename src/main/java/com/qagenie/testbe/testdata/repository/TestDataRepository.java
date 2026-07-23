package com.qagenie.testbe.testdata.repository;

import com.qagenie.testbe.testdata.entity.TestData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TestDataRepository extends JpaRepository<TestData, Long> {
    Page<TestData> findByApplicationId(Long applicationId, Pageable pageable);
    List<TestData> findByTestScenarioId(Long scenarioId);
}
