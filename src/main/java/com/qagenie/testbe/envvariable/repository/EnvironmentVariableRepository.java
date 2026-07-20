package com.qagenie.testbe.envvariable.repository;

import com.qagenie.testbe.envvariable.entity.EnvironmentVariable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EnvironmentVariableRepository extends JpaRepository<EnvironmentVariable, Long> {
    List<EnvironmentVariable> findByProjectId(Long projectId);
    boolean existsByProjectIdAndNameIgnoreCase(Long projectId, String name);
}