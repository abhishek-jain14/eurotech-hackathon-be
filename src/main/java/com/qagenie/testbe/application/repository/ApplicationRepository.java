package com.qagenie.testbe.application.repository;

import com.qagenie.testbe.application.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
    boolean existsByProjectIdAndNameIgnoreCase(Long projectId, String name);
    Optional<Application> findByProjectIdAndNameIgnoreCase(Long projectId, String name);
    java.util.List<Application> findByProjectId(Long projectId);
}
