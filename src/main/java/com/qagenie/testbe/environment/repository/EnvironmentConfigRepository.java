package com.qagenie.testbe.environment.repository;

import com.qagenie.testbe.environment.entity.EnvironmentConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnvironmentConfigRepository extends JpaRepository<EnvironmentConfig, Long> {
    List<EnvironmentConfig> findByProjectId(Long projectId);
    Optional<EnvironmentConfig> findByProjectIdAndEnvNameIgnoreCase(Long projectId, String envName);
    boolean existsByProjectIdAndEnvNameIgnoreCase(Long projectId, String envName);
}
