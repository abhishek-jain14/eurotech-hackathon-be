package com.qagenie.testbe.testflow.repository;

import com.qagenie.testbe.testflow.entity.TestFlow;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TestFlowRepository extends JpaRepository<TestFlow, Long> {
    List<TestFlow> findByApplicationId(Long applicationId);
}
