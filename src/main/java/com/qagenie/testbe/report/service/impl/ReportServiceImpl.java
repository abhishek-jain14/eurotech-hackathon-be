package com.qagenie.testbe.report.service.impl;

import com.qagenie.testbe.execution.entity.ExecutionRun;
import com.qagenie.testbe.execution.repository.ExecutionRunRepository;
import com.qagenie.testbe.report.dto.ReportSummaryDto;
import com.qagenie.testbe.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private final ExecutionRunRepository executionRunRepository;

    @Override
    public ReportSummaryDto getSummaryForApplication(Long applicationId) {
        List<ExecutionRun> runs = executionRunRepository.findByApplicationId(applicationId, Pageable.unpaged()).getContent();

        long totalRuns = runs.size();
        long totalScenarios = runs.stream().mapToLong(r -> r.getTotalScenarios() == null ? 0 : r.getTotalScenarios()).sum();
        long totalPassed = runs.stream().mapToLong(r -> r.getPassedCount() == null ? 0 : r.getPassedCount()).sum();
        long totalFailed = runs.stream().mapToLong(r -> r.getFailedCount() == null ? 0 : r.getFailedCount()).sum();
        double passRate = totalScenarios == 0 ? 0.0 : (totalPassed * 100.0) / totalScenarios;

        double avgDurationSeconds = runs.stream()
                .filter(r -> r.getStartedAt() != null && r.getCompletedAt() != null)
                .mapToLong(r -> Duration.between(r.getStartedAt(), r.getCompletedAt()).toSeconds())
                .average()
                .orElse(0.0);

        return new ReportSummaryDto(applicationId, totalRuns, totalScenarios, totalPassed, totalFailed, passRate, avgDurationSeconds);
    }
}
