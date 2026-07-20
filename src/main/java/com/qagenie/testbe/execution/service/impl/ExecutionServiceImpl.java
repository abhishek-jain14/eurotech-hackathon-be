package com.qagenie.testbe.execution.service.impl;

import com.qagenie.testbe.common.exception.ResourceNotFoundException;
import com.qagenie.testbe.environment.entity.EnvironmentConfig;
import com.qagenie.testbe.environment.repository.EnvironmentConfigRepository;
import com.qagenie.testbe.execution.dto.ExecutionRequestDto;
import com.qagenie.testbe.execution.dto.ExecutionRunResponseDto;
import com.qagenie.testbe.execution.entity.ExecutionResult;
import com.qagenie.testbe.execution.entity.ExecutionRun;
import com.qagenie.testbe.execution.entity.ExecutionStatus;
import com.qagenie.testbe.execution.entity.ResultStatus;
import com.qagenie.testbe.execution.mapper.ExecutionMapper;
import com.qagenie.testbe.execution.repository.ExecutionResultRepository;
import com.qagenie.testbe.execution.repository.ExecutionRunRepository;
import com.qagenie.testbe.execution.service.ExecutionService;
import com.qagenie.testbe.application.entity.Application;
import com.qagenie.testbe.application.repository.ApplicationRepository;
import com.qagenie.testbe.scenario.entity.TestScenario;
import com.qagenie.testbe.scenario.repository.TestScenarioRepository;
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
 * Orchestrates a suite run. The actual HTTP/UI execution engine is a
 * pluggable integration point (executeScenario) - wire your real test
 * runner (REST-assured, Playwright, etc.) there. For now it produces a
 * deterministic simulated result so the platform is runnable end-to-end
 * out of the box.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ExecutionServiceImpl implements ExecutionService {

    private static final Logger log = LoggerFactory.getLogger(ExecutionServiceImpl.class);

    private final ExecutionRunRepository executionRunRepository;
    private final ExecutionResultRepository executionResultRepository;
    private final ApplicationRepository applicationRepository;
    private final EnvironmentConfigRepository environmentConfigRepository;
    private final TestScenarioRepository scenarioRepository;
    private final ExecutionMapper executionMapper;

    @Override
    public ExecutionRunResponseDto trigger(ExecutionRequestDto request) {
        Application application = applicationRepository.findById(request.applicationId())
                .orElseThrow(() -> ResourceNotFoundException.of("Application", request.applicationId()));
        EnvironmentConfig environment = environmentConfigRepository.findById(request.environmentId())
                .orElseThrow(() -> ResourceNotFoundException.of("EnvironmentConfig", request.environmentId()));

        if (!environment.getProject().getId().equals(application.getProject().getId())) {
            throw new com.qagenie.testbe.common.exception.BusinessException(
                    "Selected environment does not belong to this application's project", "ENV_PROJECT_MISMATCH");
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

            ExecutionResult result = executeScenario(run, scenario, environment);
            executionResultRepository.save(result);
            if (result.getResultStatus() == ResultStatus.PASS) passed++;
            else if (result.getResultStatus() == ResultStatus.FAIL) failed++;
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
     * Integration point: replace with a call into the real execution engine
     * (HTTP client hitting environment.getBaseUrl() + scenario.getEndpoint(),
     * or a UI runner for frontend projects).
     */
    private ExecutionResult executeScenario(ExecutionRun run, TestScenario scenario, EnvironmentConfig environment) {
        ExecutionResult result = new ExecutionResult();
        result.setExecutionRun(run);
        result.setScenario(scenario);
        result.setResponseTimeMs((long) (100 + Math.random() * 400));
        boolean simulatedPass = Math.random() > 0.15;
        result.setResultStatus(simulatedPass ? ResultStatus.PASS : ResultStatus.FAIL);
        if (!simulatedPass) {
            result.setErrorMessage("Simulated failure calling " + environment.getBaseUrl() + scenario.getEndpoint());
        }
        return result;
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
        ExecutionRunResponseDto base = executionMapper.toResponseDto(run);
        return new com.qagenie.testbe.execution.dto.ExecutionRunResponseDto(
                base.id(), base.applicationId(), base.environmentId(), base.environmentName(), base.suiteName(),
                base.status(), base.startedAt(), base.completedAt(), base.totalScenarios(),
                base.passedCount(), base.failedCount(), executionMapper.toResultDtoList(results)
        );
    }
}
