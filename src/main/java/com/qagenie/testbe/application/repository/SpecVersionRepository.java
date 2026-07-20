package com.qagenie.testbe.application.repository;

import com.qagenie.testbe.application.entity.SpecVersion;
import com.qagenie.testbe.application.entity.SpecVersionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpecVersionRepository extends JpaRepository<SpecVersion, Long> {

    Optional<SpecVersion> findByApplicationIdAndStatus(Long applicationId, SpecVersionStatus status);

    Optional<SpecVersion> findByApplicationIdAndContentHash(Long applicationId, String contentHash);

    List<SpecVersion> findByApplicationIdOrderByVersionNumberDesc(Long applicationId);

    List<SpecVersion> findByApplicationIdAndStatusOrderByVersionNumberDesc(Long applicationId, SpecVersionStatus status);

    long countByApplicationId(Long applicationId);
}
