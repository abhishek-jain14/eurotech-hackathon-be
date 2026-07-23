package com.qagenie.testbe.report.repository;

import com.qagenie.testbe.report.entity.ReportSignoff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReportSignoffRepository extends JpaRepository<ReportSignoff, Long> {
    Optional<ReportSignoff> findByApplicationId(Long applicationId);
}
