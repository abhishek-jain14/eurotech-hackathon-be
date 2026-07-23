package com.qagenie.testbe.report.service.impl;

import com.qagenie.testbe.application.entity.Application;
import com.qagenie.testbe.application.repository.ApplicationRepository;
import com.qagenie.testbe.common.exception.BusinessException;
import com.qagenie.testbe.common.exception.ResourceNotFoundException;
import com.qagenie.testbe.coverage.dto.EndpointCoverageDto;
import com.qagenie.testbe.coverage.service.CoverageService;
import com.qagenie.testbe.execution.entity.ExecutionResult;
import com.qagenie.testbe.execution.entity.ExecutionRun;
import com.qagenie.testbe.execution.entity.ResultStatus;
import com.qagenie.testbe.execution.repository.ExecutionResultRepository;
import com.qagenie.testbe.execution.repository.ExecutionRunRepository;
import com.qagenie.testbe.report.dto.*;
import com.qagenie.testbe.report.entity.ReportSignoff;
import com.qagenie.testbe.report.entity.SignoffStatus;
import com.qagenie.testbe.report.repository.ReportSignoffRepository;
import com.qagenie.testbe.report.service.ReportService;
import com.qagenie.testbe.scenario.entity.TestScenario;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private static final int FLAKINESS_WINDOW = 10; // last N runs considered for flakiness

    private final ExecutionRunRepository executionRunRepository;
    private final ExecutionResultRepository executionResultRepository;
    private final ApplicationRepository applicationRepository;
    private final CoverageService coverageService;
    private final ReportSignoffRepository reportSignoffRepository;

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

    @Override
    public ReportDetailDto getDetailForApplication(Long applicationId) {
        if (!applicationRepository.existsById(applicationId)) {
            throw ResourceNotFoundException.of("Application", applicationId);
        }

        List<ExecutionRun> lastTwo = executionRunRepository
                .findByApplicationId(applicationId, PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "startedAt")))
                .getContent();

        ExecutionRun latest = lastTwo.isEmpty() ? null : lastTwo.get(0);
        ExecutionRun previous = lastTwo.size() < 2 ? null : lastTwo.get(1);

        int skippedCount = 0;
        List<FailureDetailDto> failures = List.of();
        if (latest != null) {
            List<ExecutionResult> latestResults = executionResultRepository.findByExecutionRunId(latest.getId());
            skippedCount = (int) latestResults.stream().filter(r -> r.getResultStatus() == ResultStatus.SKIPPED).count();
            failures = latestResults.stream()
                    .filter(r -> r.getResultStatus() == ResultStatus.FAIL)
                    .map(this::toFailureDetail)
                    .toList();
        }

        List<EndpointCoverageDto> coverageRows = coverageService.getApplicationCoverage(applicationId);
        int uncovered = (int) coverageRows.stream().filter(r -> "NO_TESTS".equals(r.status())).count();
        int partial = (int) coverageRows.stream().filter(r -> "PARTIAL".equals(r.status())).count();

        return new ReportDetailDto(
                toSnapshot(latest), toSnapshot(previous),
                skippedCount, uncovered, partial,
                computeFlakiness(applicationId), failures
        );
    }

    private RunSnapshotDto toSnapshot(ExecutionRun run) {
        if (run == null) return null;
        int total = run.getTotalScenarios() == null ? 0 : run.getTotalScenarios();
        int passed = run.getPassedCount() == null ? 0 : run.getPassedCount();
        int failed = run.getFailedCount() == null ? 0 : run.getFailedCount();
        long durationSeconds = (run.getStartedAt() != null && run.getCompletedAt() != null)
                ? Duration.between(run.getStartedAt(), run.getCompletedAt()).toSeconds() : 0;
        int attempts = passed + failed;
        double passRate = attempts == 0 ? 0.0 : (passed * 100.0) / attempts;
        return new RunSnapshotDto(run.getId(), run.getSuiteName(), run.getEnvironment().getEnvName(),
                run.getStartedAt(), run.getCompletedAt(), durationSeconds, total, passed, failed, passRate);
    }

    private FailureDetailDto toFailureDetail(ExecutionResult result) {
        return new FailureDetailDto(
                result.getId(), result.getScenario().getId(), result.getScenario().getName(),
                result.getTestData() != null ? result.getTestData().getId() : null,
                result.getTestData() != null ? result.getTestData().getRecordName() : null,
                result.getRequestMethod(), result.getRequestUrl(), result.getResponseStatusCode(), result.getErrorMessage()
        );
    }

    /** A scenario is "flaky" (not just failing) when it has BOTH passes and fails across its last runs. */
    private List<FlakyScenarioDto> computeFlakiness(Long applicationId) {
        List<ExecutionRun> recentRuns = executionRunRepository
                .findByApplicationId(applicationId, PageRequest.of(0, FLAKINESS_WINDOW, Sort.by(Sort.Direction.DESC, "startedAt")))
                .getContent();
        if (recentRuns.isEmpty()) return List.of();

        List<Long> runIds = recentRuns.stream().map(ExecutionRun::getId).toList();
        Map<Long, List<ExecutionResult>> byScenario = executionResultRepository.findByExecutionRun_IdIn(runIds).stream()
                .filter(r -> r.getResultStatus() != ResultStatus.SKIPPED)
                .collect(Collectors.groupingBy(r -> r.getScenario().getId()));

        List<FlakyScenarioDto> flaky = new ArrayList<>();
        for (List<ExecutionResult> results : byScenario.values()) {
            int total = results.size();
            int passed = (int) results.stream().filter(r -> r.getResultStatus() == ResultStatus.PASS).count();
            int failed = total - passed;
            if (passed == 0 || failed == 0) continue; // consistent pass or consistent fail isn't "flaky"
            TestScenario scenario = results.get(0).getScenario();
            double stability = passed * 100.0 / total;
            flaky.add(new FlakyScenarioDto(scenario.getId(), scenario.getName(), total, passed, failed, stability));
        }
        flaky.sort((a, b) -> Double.compare(a.stabilityPercent(), b.stabilityPercent()));
        return flaky;
    }

    @Override
    public List<ReportSignoffDto> listSignoffs() {
        List<Application> applications = applicationRepository.findAll();
        List<ReportSignoffDto> dtos = new ArrayList<>();
        for (Application application : applications) {
            Long latestRunId = executionRunRepository
                    .findTopByApplicationIdOrderByStartedAtDesc(application.getId())
                    .map(ExecutionRun::getId).orElse(null);
            ReportSignoff signoff = reportSignoffRepository.findByApplicationId(application.getId()).orElse(null);
            dtos.add(toSignoffDto(application, signoff, latestRunId));
        }
        return dtos;
    }

    @Override
    public ReportSignoffDto signOff(Long applicationId, SignoffRequestDto request, String signedOffBy) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> ResourceNotFoundException.of("Application", applicationId));
        ExecutionRun latestRun = executionRunRepository.findTopByApplicationIdOrderByStartedAtDesc(applicationId)
                .orElseThrow(() -> new BusinessException("No execution run yet for this application - nothing to sign off", "NO_RUN_TO_SIGNOFF"));

        ReportSignoff signoff = reportSignoffRepository.findByApplicationId(applicationId).orElseGet(() -> {
            ReportSignoff s = new ReportSignoff();
            s.setApplication(application);
            return s;
        });
        signoff.setExecutionRun(latestRun);
        signoff.setStatus("APPROVE".equals(request.action()) ? SignoffStatus.APPROVED : SignoffStatus.REJECTED);
        signoff.setComment(request.comment());
        signoff.setSignedOffBy(signedOffBy);
        signoff.setSignedOffAt(Instant.now());
        ReportSignoff saved = reportSignoffRepository.save(signoff);

        return toSignoffDto(application, saved, latestRun.getId());
    }

    private ReportSignoffDto toSignoffDto(Application application, ReportSignoff signoff, Long latestRunId) {
        if (signoff == null) {
            return new ReportSignoffDto(application.getId(), application.getName(), SignoffStatus.PENDING.name(),
                    null, null, null, null, latestRunId, false);
        }
        Long signedOffRunId = signoff.getExecutionRun() != null ? signoff.getExecutionRun().getId() : null;
        boolean stale = signedOffRunId != null && latestRunId != null && !signedOffRunId.equals(latestRunId);
        return new ReportSignoffDto(application.getId(), application.getName(), signoff.getStatus().name(),
                signoff.getComment(), signoff.getSignedOffBy(), signoff.getSignedOffAt(), signedOffRunId, latestRunId, stale);
    }
}
