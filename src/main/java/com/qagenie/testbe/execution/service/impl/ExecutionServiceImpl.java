package com.qagenie.testbe.execution.service.impl;

import com.qagenie.testbe.application.entity.Application;
import com.qagenie.testbe.application.repository.ApplicationRepository;
import com.qagenie.testbe.common.exception.BusinessException;
import com.qagenie.testbe.common.exception.ResourceNotFoundException;
import com.qagenie.testbe.environment.entity.EnvironmentConfig;
import com.qagenie.testbe.environment.repository.EnvironmentConfigRepository;
import com.qagenie.testbe.execution.cucumber.CucumberFeatureRunner;
import com.qagenie.testbe.execution.cucumber.CucumberFeatureRunner.StepResult;
import com.qagenie.testbe.execution.cucumber.ExecutionContextHolder.CallLog;
import com.qagenie.testbe.execution.cucumber.GherkinFeatureBuilder;
import com.qagenie.testbe.execution.dto.ExecutionRequestDto;
import com.qagenie.testbe.execution.dto.ExecutionRunResponseDto;
import com.qagenie.testbe.execution.entity.ExecutionResult;
import com.qagenie.testbe.execution.entity.ExecutionRun;
import com.qagenie.testbe.execution.entity.ExecutionScenarioResult;
import com.qagenie.testbe.execution.entity.ExecutionStatus;
import com.qagenie.testbe.execution.entity.ResultStatus;
import com.qagenie.testbe.execution.mapper.ExecutionMapper;
import com.qagenie.testbe.execution.repository.ExecutionResultRepository;
import com.qagenie.testbe.execution.repository.ExecutionRunRepository;
import com.qagenie.testbe.execution.repository.ExecutionScenarioResultRepository;
import com.qagenie.testbe.execution.service.ExecutionService;
import com.qagenie.testbe.scenario.entity.TestScenario;
import com.qagenie.testbe.scenario.repository.TestScenarioRepository;
import com.qagenie.testbe.testdata.entity.TestData;
import com.qagenie.testbe.testdata.repository.TestDataRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Orchestrates a suite run: for each requested scenario, reads its Gherkin
 * (a Scenario Outline with &lt;fieldName&gt; placeholders), finds the TEST_DATA
 * rows linked to it, substitutes them into an Examples: table, writes the
 * resulting .feature file and runs it via Cucumber (see the {@code execution.cucumber}
 * package) - real HTTP calls against the chosen environment, real pass/fail from
 * real assertions, replacing the earlier random-simulation executor.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ExecutionServiceImpl implements ExecutionService {

    private static final Logger log = LoggerFactory.getLogger(ExecutionServiceImpl.class);

    private final ExecutionRunRepository executionRunRepository;
    private final ExecutionResultRepository executionResultRepository;
    private final ExecutionScenarioResultRepository executionScenarioResultRepository;
    private final ApplicationRepository applicationRepository;
    private final EnvironmentConfigRepository environmentConfigRepository;
    private final TestScenarioRepository scenarioRepository;
    private final TestDataRepository testDataRepository;
    private final GherkinFeatureBuilder gherkinFeatureBuilder;
    private final CucumberFeatureRunner cucumberFeatureRunner;
    private final ExecutionMapper executionMapper;

    @Override
    public ExecutionRunResponseDto trigger(ExecutionRequestDto request) {
        Application application = applicationRepository.findById(request.applicationId())
                .orElseThrow(() -> ResourceNotFoundException.of("Application", request.applicationId()));
        EnvironmentConfig environment = environmentConfigRepository.findById(request.environmentId())
                .orElseThrow(() -> ResourceNotFoundException.of("EnvironmentConfig", request.environmentId()));

        if (!environment.getProject().getId().equals(application.getProject().getId())) {
            throw new BusinessException("Selected environment does not belong to this application's project", "ENV_PROJECT_MISMATCH");
        }

        ExecutionRun run = new ExecutionRun();
        run.setApplication(application);
        run.setEnvironment(environment);
        run.setSuiteName(request.suiteName() != null ? request.suiteName() : "Ad-hoc Suite");
        run.setStatus(ExecutionStatus.RUNNING);
        run.setStartedAt(Instant.now());
        run.setTotalScenarios(request.scenarioIds().size());
        run = executionRunRepository.save(run);

        log.info("Execution run started: id={}, application={}, env={}, scenarios={}",
                run.getId(), application.getName(), environment.getEnvName(), request.scenarioIds().size());

        int passed = 0, failed = 0;
        for (Long scenarioId : request.scenarioIds()) {
            TestScenario scenario = scenarioRepository.findById(scenarioId)
                    .orElseThrow(() -> ResourceNotFoundException.of("TestScenario", scenarioId));

            List<ExecutionResult> results = executeScenario(run, scenario, environment);
            executionResultRepository.saveAll(results);
            executionScenarioResultRepository.save(buildScenarioSummary(run, scenario, results));
            for (ExecutionResult result : results) {
                if (result.getResultStatus() == ResultStatus.PASS) passed++;
                else if (result.getResultStatus() == ResultStatus.FAIL) failed++;
            }
        }

        run.setPassedCount(passed);
        run.setFailedCount(failed);
        run.setStatus(failed == 0 ? ExecutionStatus.COMPLETED : ExecutionStatus.FAILED);
        run.setCompletedAt(Instant.now());
        run = executionRunRepository.save(run);

        log.info("Execution run finished: id={}, status={}, passed={}, failed={}",
                run.getId(), run.getStatus(), passed, failed);

        return buildFullResponse(run);
    }

    /**
     * One scenario can yield multiple results - one per TEST_DATA row linked to it
     * (each becomes an Examples row). A scenario with no linked test data can't be
     * meaningfully run (nothing to substitute into its placeholders), so it's
     * reported as a single SKIPPED result instead of silently doing nothing.
     */
    private List<ExecutionResult> executeScenario(ExecutionRun run, TestScenario scenario, EnvironmentConfig environment) {
        List<TestData> testDataRows = testDataRepository.findByTestScenarioId(scenario.getId());

        if (testDataRows.isEmpty()) {
            ExecutionResult skipped = new ExecutionResult();
            skipped.setExecutionRun(run);
            skipped.setScenario(scenario);
            skipped.setResultStatus(ResultStatus.SKIPPED);
            skipped.setErrorMessage("No test data linked to this scenario - nothing to substitute into its Gherkin placeholders");
            return List.of(skipped);
        }

        String featureText;
        List<StepResult> stepResults;
        try {
            featureText = gherkinFeatureBuilder.build(scenario, testDataRows);
            stepResults = cucumberFeatureRunner.run(featureText, resolveExecutionBaseUrl(scenario.getApplication(), environment));
        } catch (Exception e) {
            log.error("Execution failed for scenario id={}", scenario.getId(), e);
            ExecutionResult failure = new ExecutionResult();
            failure.setExecutionRun(run);
            failure.setScenario(scenario);
            failure.setResultStatus(ResultStatus.FAIL);
            failure.setErrorMessage("Unable to run scenario: " + e.getMessage());
            return List.of(failure);
        }

        if (stepResults.size() != testDataRows.size()) {
            log.warn("Cucumber returned {} results for {} test data rows on scenario id={} - zipping by index up to the shorter length",
                    stepResults.size(), testDataRows.size(), scenario.getId());
        }

        List<ExecutionResult> results = new java.util.ArrayList<>();
        int count = Math.min(stepResults.size(), testDataRows.size());
        for (int i = 0; i < count; i++) {
            StepResult stepResult = stepResults.get(i);
            ExecutionResult result = new ExecutionResult();
            result.setExecutionRun(run);
            result.setScenario(scenario);
            result.setTestData(testDataRows.get(i));
            applyExpectedValues(result, testDataRows.get(i));
            result.setResultStatus(stepResult.passed() ? ResultStatus.PASS : ResultStatus.FAIL);
            result.setResponseTimeMs(stepResult.durationMs());
            result.setErrorMessage(stepResult.errorMessage());
            applyCallLog(result, stepResult.callLog());
            results.add(result);
        }
        // Any test data rows Cucumber didn't produce a matching result for (report/parse mismatch).
        for (int i = count; i < testDataRows.size(); i++) {
            ExecutionResult result = new ExecutionResult();
            result.setExecutionRun(run);
            result.setScenario(scenario);
            result.setTestData(testDataRows.get(i));
            result.setResultStatus(ResultStatus.SKIPPED);
            result.setErrorMessage("No matching result parsed from the Cucumber report for this test data row");
            results.add(result);
        }
        return results;
    }

    /**
     * Aggregates a scenario's per-Test-Data-row EXECUTION_RESULT rows (already built by
     * executeScenario) into the single EXECUTION_SCENARIO_RESULT summary row for this run -
     * PASS only if every row passed, FAIL if any row failed, SKIPPED only if none ran.
     */
    private ExecutionScenarioResult buildScenarioSummary(ExecutionRun run, TestScenario scenario, List<ExecutionResult> results) {
        int passedCount = 0, failedCount = 0, skippedCount = 0;
        long totalResponseTimeMs = 0;
        for (ExecutionResult result : results) {
            switch (result.getResultStatus()) {
                case PASS -> passedCount++;
                case FAIL -> failedCount++;
                case SKIPPED -> skippedCount++;
            }
            if (result.getResponseTimeMs() != null) {
                totalResponseTimeMs += result.getResponseTimeMs();
            }
        }
        ResultStatus overallStatus = failedCount > 0 ? ResultStatus.FAIL
                : (passedCount > 0 ? ResultStatus.PASS : ResultStatus.SKIPPED);

        ExecutionScenarioResult summary = new ExecutionScenarioResult();
        summary.setExecutionRun(run);
        summary.setScenario(scenario);
        summary.setOverallStatus(overallStatus);
        summary.setTotalTestData(results.size());
        summary.setPassedCount(passedCount);
        summary.setFailedCount(failedCount);
        summary.setSkippedCount(skippedCount);
        summary.setTotalResponseTimeMs(totalResponseTimeMs);
        return summary;
    }

    /** Copies the expected outcome captured on this TestData row, for expected-vs-actual reporting. */
    private void applyExpectedValues(ExecutionResult result, TestData testData) {
        result.setExpectedHttpStatusCode(testData.getHttpStatusCode());
        result.setExpectedErrorCode(testData.getErrorCode());
        result.setExpectedErrorMsg(testData.getErrorMsg());
        result.setExpectedResponseJson(testData.getResponseJson());
    }

    /** Persists the full (untruncated) request/response ApiSteps captured for this row. */
    private void applyCallLog(ExecutionResult result, CallLog callLog) {
        if (callLog == null) return;
        result.setRequestMethod(callLog.requestMethod());
        result.setRequestUrl(callLog.requestUrl());
        result.setRequestHeaders(callLog.requestHeadersJson());
        result.setRequestBody(callLog.requestBody());
        result.setResponseStatusCode(callLog.responseStatus());
        result.setResponseHeaders(callLog.responseHeadersJson());
        result.setResponseBody(callLog.responseBody());
    }

    /**
     * The environment's raw baseUrl is just the host (e.g. http://localhost:8081) -
     * same convention as ApplicationServiceImpl#resolveEffectiveSpecUrl, the
     * application's own name is the context path segment under it (e.g.
     * http://localhost:8081/product-app) that the scenario's resource path is
     * relative to. Without this, "resource : /api/products/{id}" resolves against
     * the bare host and 404s before it ever reaches the app.
     */
    private String resolveExecutionBaseUrl(Application application, EnvironmentConfig environment) {
        String base = environment.getBaseUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/" + application.getName().toLowerCase();
    }

    @Override
    @Transactional(readOnly = true)
    public ExecutionRunResponseDto getRunById(Long runId) {
        ExecutionRun run = executionRunRepository.findById(runId)
                .orElseThrow(() -> ResourceNotFoundException.of("ExecutionRun", runId));
        return buildFullResponse(run);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExecutionRunResponseDto> listByApplication(Long applicationId, Pageable pageable) {
        return executionRunRepository.findByApplicationId(applicationId, pageable)
                .map(executionMapper::toResponseDto);
    }

    private ExecutionRunResponseDto buildFullResponse(ExecutionRun run) {
        List<ExecutionResult> results = executionResultRepository.findByExecutionRunId(run.getId());
        List<ExecutionScenarioResult> scenarioResults = executionScenarioResultRepository.findByExecutionRunId(run.getId());
        ExecutionRunResponseDto base = executionMapper.toResponseDto(run);
        return new ExecutionRunResponseDto(
                base.id(), base.applicationId(), base.environmentId(), base.environmentName(), base.suiteName(),
                base.status(), base.startedAt(), base.completedAt(), base.totalScenarios(),
                base.passedCount(), base.failedCount(),
                executionMapper.toScenarioResultDtoList(scenarioResults),
                executionMapper.toResultDtoList(results)
        );
    }
}
