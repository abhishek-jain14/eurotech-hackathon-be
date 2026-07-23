package com.qagenie.testbe.coverage.service.impl;

import com.qagenie.testbe.application.dto.ApiEndpoint;
import com.qagenie.testbe.application.entity.Application;
import com.qagenie.testbe.application.repository.ApplicationRepository;
import com.qagenie.testbe.application.service.ApplicationService;
import com.qagenie.testbe.common.exception.ResourceNotFoundException;
import com.qagenie.testbe.coverage.dto.ApplicationCoverageDto;
import com.qagenie.testbe.coverage.dto.CoverageOverviewDto;
import com.qagenie.testbe.coverage.dto.EndpointCoverageDto;
import com.qagenie.testbe.coverage.service.CoverageService;
import com.qagenie.testbe.execution.entity.ExecutionResult;
import com.qagenie.testbe.execution.entity.ExecutionRun;
import com.qagenie.testbe.execution.entity.ResultStatus;
import com.qagenie.testbe.execution.repository.ExecutionResultRepository;
import com.qagenie.testbe.execution.repository.ExecutionRunRepository;
import com.qagenie.testbe.scenario.entity.ScenarioType;
import com.qagenie.testbe.scenario.entity.TestScenario;
import com.qagenie.testbe.scenario.repository.TestScenarioRepository;
import com.qagenie.testbe.testdata.repository.TestDataRepository;
import com.qagenie.testbe.testflow.repository.TestFlowStepRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Computes test coverage per application by cross-referencing its parsed endpoints
 * (SPEC_ENDPOINT cache, via ApplicationService.getApiEndpoints) against its
 * TEST_SCENARIO rows (matched by httpMethod+path), whether each scenario is used in
 * any TEST_FLOW_STEP, whether it has any linked TEST_DATA, and pass/fail counts from
 * the application's most recent EXECUTION_RUN.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CoverageServiceImpl implements CoverageService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationService applicationService;
    private final TestScenarioRepository testScenarioRepository;
    private final TestFlowStepRepository testFlowStepRepository;
    private final TestDataRepository testDataRepository;
    private final ExecutionRunRepository executionRunRepository;
    private final ExecutionResultRepository executionResultRepository;

    @Override
    public CoverageOverviewDto getOverview() {
        List<ApplicationCoverageDto> appDtos = new ArrayList<>();
        int totalEndpoints = 0, coveredEndpoints = 0, totalScenarios = 0, lastRunPassed = 0, lastRunFailed = 0, gaps = 0;

        for (Application application : applicationRepository.findAll()) {
            List<EndpointCoverageDto> rows = computeEndpointCoverage(application);
            ApplicationCoverageDto dto = summarize(application, rows);
            appDtos.add(dto);
            totalEndpoints += dto.totalEndpoints();
            coveredEndpoints += dto.coveredEndpoints();
            totalScenarios += dto.totalScenarios();
            lastRunPassed += dto.passedCount();
            lastRunFailed += dto.failedCount();
            gaps += dto.gapsCount();
        }

        int pct = totalEndpoints == 0 ? 0 : Math.round(coveredEndpoints * 100f / totalEndpoints);
        return new CoverageOverviewDto(totalEndpoints, coveredEndpoints, pct, totalScenarios, lastRunPassed, lastRunFailed, gaps, appDtos);
    }

    @Override
    public List<EndpointCoverageDto> getApplicationCoverage(Long applicationId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> ResourceNotFoundException.of("Application", applicationId));
        return computeEndpointCoverage(application);
    }

    private List<EndpointCoverageDto> computeEndpointCoverage(Application application) {
        List<ApiEndpoint> endpoints = applicationService.getApiEndpoints(application.getId());
        List<TestScenario> scenarios = testScenarioRepository.findByApplicationId(application.getId());
        List<Long> scenarioIds = scenarios.stream().map(TestScenario::getId).toList();

        Set<Long> flowScenarioIds = scenarioIds.isEmpty() ? Set.of() : testFlowStepRepository.findByScenario_IdIn(scenarioIds).stream()
                .map(step -> step.getScenario().getId())
                .collect(Collectors.toSet());

        Set<Long> testDataScenarioIds = scenarioIds.isEmpty() ? Set.of() : testDataRepository.findByTestScenario_IdIn(scenarioIds).stream()
                .map(td -> td.getTestScenario().getId())
                .collect(Collectors.toSet());

        Map<Long, long[]> passFailByScenario = new HashMap<>(); // scenarioId -> [passed, failed]
        executionRunRepository.findTopByApplicationIdOrderByStartedAtDesc(application.getId()).ifPresent(run -> {
            for (ExecutionResult result : executionResultRepository.findByExecutionRunId(run.getId())) {
                Long scenarioId = result.getScenario().getId();
                long[] counts = passFailByScenario.computeIfAbsent(scenarioId, k -> new long[2]);
                if (result.getResultStatus() == ResultStatus.PASS) counts[0]++;
                else if (result.getResultStatus() == ResultStatus.FAIL) counts[1]++;
            }
        });

        List<EndpointCoverageDto> rows = new ArrayList<>();
        for (ApiEndpoint endpoint : endpoints) {
            List<TestScenario> matching = scenarios.stream()
                    .filter(s -> Objects.equals(s.getHttpMethod(), endpoint.getHttpMethod()) && Objects.equals(s.getEndpoint(), endpoint.getPath()))
                    .toList();

            int positive = (int) matching.stream().filter(s -> s.getScenarioType() == ScenarioType.POSITIVE).count();
            int negative = (int) matching.stream().filter(s -> s.getScenarioType() == ScenarioType.NEGATIVE).count();
            boolean hasFlow = matching.stream().anyMatch(s -> flowScenarioIds.contains(s.getId()));
            boolean hasTestData = matching.stream().anyMatch(s -> testDataScenarioIds.contains(s.getId()));
            long passed = matching.stream().mapToLong(s -> passFailByScenario.getOrDefault(s.getId(), new long[2])[0]).sum();
            long failed = matching.stream().mapToLong(s -> passFailByScenario.getOrDefault(s.getId(), new long[2])[1]).sum();

            String status;
            if (matching.isEmpty()) status = "NO_TESTS";
            else if (failed > 0) status = "FAILURES";
            else if (negative > 0 && hasFlow && hasTestData) status = "FULL";
            else status = "PARTIAL";

            rows.add(new EndpointCoverageDto(endpoint.getHttpMethod(), endpoint.getPath(), endpoint.getSummary(),
                    positive, negative, (int) passed, (int) failed, hasFlow, hasTestData, status));
        }
        return rows;
    }

    private ApplicationCoverageDto summarize(Application application, List<EndpointCoverageDto> rows) {
        int total = rows.size();
        int covered = (int) rows.stream().filter(r -> !"NO_TESTS".equals(r.status())).count();
        int coveragePercent = total == 0 ? 0 : Math.round(covered * 100f / total);
        int totalScenarios = rows.stream().mapToInt(r -> r.positiveCount() + r.negativeCount()).sum();
        int passed = rows.stream().mapToInt(EndpointCoverageDto::passedCount).sum();
        int failed = rows.stream().mapToInt(EndpointCoverageDto::failedCount).sum();
        int gaps = (int) rows.stream().filter(r -> !"FULL".equals(r.status())).count();

        String status;
        if (total > 0 && covered == total && failed == 0) status = "GOOD";
        else if (failed > 0) status = "FAILURES";
        else if (total > 0 && coveragePercent < 50) status = "UNCOVERED";
        else status = "PARTIAL";

        return new ApplicationCoverageDto(
                application.getId(), application.getName(), application.getApplicationType().name(),
                application.getSpecFormat() != null ? application.getSpecFormat().name() : null,
                total, covered, coveragePercent, totalScenarios, passed, failed, gaps, status
        );
    }
}
