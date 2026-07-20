package com.qagenie.testbe.dashboard.service.impl;

import com.qagenie.testbe.application.repository.ApplicationRepository;
import com.qagenie.testbe.dashboard.dto.DashboardStatsDto;
import com.qagenie.testbe.dashboard.service.DashboardService;
import com.qagenie.testbe.execution.entity.ExecutionRun;
import com.qagenie.testbe.execution.repository.ExecutionRunRepository;
import com.qagenie.testbe.project.repository.ProjectRepository;
import com.qagenie.testbe.scenario.repository.TestScenarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final ExecutionRunRepository executionRunRepository;
    private final ProjectRepository projectRepository;
    private final ApplicationRepository applicationRepository;
    private final TestScenarioRepository scenarioRepository;

    @Override
    public DashboardStatsDto getOverallStats() {
        List<ExecutionRun> runs = executionRunRepository.findAll();

        long totalScenarios = runs.stream().mapToLong(r -> r.getTotalScenarios() == null ? 0 : r.getTotalScenarios()).sum();
        long totalPassed = runs.stream().mapToLong(r -> r.getPassedCount() == null ? 0 : r.getPassedCount()).sum();
        long totalFailed = runs.stream().mapToLong(r -> r.getFailedCount() == null ? 0 : r.getFailedCount()).sum();
        double passRate = totalScenarios == 0 ? 0.0 : (totalPassed * 100.0) / totalScenarios;

        double avgDurationSeconds = runs.stream()
                .filter(r -> r.getStartedAt() != null && r.getCompletedAt() != null)
                .mapToLong(r -> Duration.between(r.getStartedAt(), r.getCompletedAt()).toSeconds())
                .average()
                .orElse(0.0);

        return new DashboardStatsDto(passRate, totalFailed, scenarioRepository.count(),
                applicationRepository.count(), projectRepository.count(), avgDurationSeconds);
    }
}
