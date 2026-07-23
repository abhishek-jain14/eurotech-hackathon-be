package com.qagenie.testbe.application.service.impl;

import com.qagenie.testbe.application.diff.SpecDiffEntry;
import com.qagenie.testbe.application.diff.SpecDiffService;
import com.qagenie.testbe.application.dto.*;
import com.qagenie.testbe.application.entity.*;
import com.qagenie.testbe.application.mapper.ApplicationMapper;
import com.qagenie.testbe.application.repository.ApplicationRepository;
import com.qagenie.testbe.application.repository.SpecVersionRepository;
import com.qagenie.testbe.application.service.ApplicationService;
import com.qagenie.testbe.application.service.SpecFetchService;
import com.qagenie.testbe.common.exception.BusinessException;
import com.qagenie.testbe.common.exception.ResourceNotFoundException;
import com.qagenie.testbe.environment.entity.EnvironmentConfig;
import com.qagenie.testbe.environment.repository.EnvironmentConfigRepository;
import com.qagenie.testbe.project.entity.Project;
import com.qagenie.testbe.project.repository.ProjectRepository;
import com.qagenie.testbe.scenario.entity.TestScenario;
import com.qagenie.testbe.scenario.repository.TestScenarioRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ApplicationServiceImpl implements ApplicationService {

    private static final Logger log = LoggerFactory.getLogger(ApplicationServiceImpl.class);

    private final ApplicationRepository applicationRepository;
    private final ApplicationMapper applicationMapper;
    private final ProjectRepository projectRepository;
    private final EnvironmentConfigRepository environmentConfigRepository;
    private final SpecVersionRepository specVersionRepository;
    private final TestScenarioRepository scenarioRepository;
    private final SpecFetchService specFetchService;
    private final SpecDiffService specDiffService;

    @Value("${qagenie.spec.swagger-suffix}")
    private String defaultSwaggerSuffix;

    // ---------------------------------------------------------------- CRUD

    @Override
    public ApplicationResponseDto onboard(ApplicationRequestDto request) {
        Project project = projectRepository.findById(request.projectId())
                .orElseThrow(() -> ResourceNotFoundException.of("Project", request.projectId()));

        if (applicationRepository.existsByProjectIdAndNameIgnoreCase(project.getId(), request.name())) {
            throw new BusinessException(
                    "An application named '" + request.name() + "' already exists under project '" + project.getName() + "'",
                    "APPLICATION_EXISTS");
        }

        Application entity = applicationMapper.toEntity(request);
        entity.setProject(project);
        entity.setStatus(ApplicationStatus.ACTIVE);

        /*if ("DERIVED".equals(mode)) {
            if (request.referenceEnvironmentId() == null) {
                throw new BusinessException("specSourceMode=DERIVED requires a referenceEnvironmentId", "REFERENCE_ENV_REQUIRED");
            }
            EnvironmentConfig env = resolveEnvironmentForProject(request.referenceEnvironmentId(), project.getId());
            entity.setReferenceEnvironment(env);
        } else if ("CUSTOM".equals(mode)) {
            if (request.specSourceUrl() == null || request.specSourceUrl().isBlank()) {
                throw new BusinessException("specSourceMode=CUSTOM requires specSourceUrl", "CUSTOM_URL_REQUIRED");
            }
            if (request.referenceEnvironmentId() != null) {
                entity.setReferenceEnvironment(resolveEnvironmentForProject(request.referenceEnvironmentId(), project.getId()));
            }
        } else {
            throw new BusinessException("specSourceMode must be DERIVED or CUSTOM", "INVALID_SPEC_SOURCE_MODE");
        }*/

        Application saved = applicationRepository.save(entity);
        log.info("Application onboarded: id={}, name={}, project={}",
                saved.getId(), saved.getName(), project.getName());
        return toResponseDtoWithVersionInfo(saved);
    }

    private EnvironmentConfig resolveEnvironmentForProject(Long environmentId, Long projectId) {
        EnvironmentConfig env = environmentConfigRepository.findById(environmentId)
                .orElseThrow(() -> ResourceNotFoundException.of("EnvironmentConfig", environmentId));
        if (!env.getProject().getId().equals(projectId)) {
            throw new BusinessException("Selected environment does not belong to the selected project", "ENV_PROJECT_MISMATCH");
        }
        return env;
    }

    @Override
    public ApplicationResponseDto update(Long id, ApplicationRequestDto request) {
        Application entity = findEntity(id);
        applicationMapper.updateEntityFromDto(request, entity);
        if (request.referenceEnvironmentId() != null) {
            entity.setReferenceEnvironment(resolveEnvironmentForProject(request.referenceEnvironmentId(), entity.getProject().getId()));
        }
        return toResponseDtoWithVersionInfo(applicationRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationResponseDto getById(Long id) {
        return toResponseDtoWithVersionInfo(findEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ApplicationResponseDto> list(Pageable pageable) {
        return applicationRepository.findAll(pageable).map(this::toResponseDtoWithVersionInfo);
    }

    @Override
    public void archive(Long id) {
        Application entity = findEntity(id);
        entity.setStatus(ApplicationStatus.ARCHIVED);
        applicationRepository.save(entity);
    }

    @Override
    public void delete(Long id) {
        if (!applicationRepository.existsById(id)) {
            throw ResourceNotFoundException.of("Application", id);
        }
        applicationRepository.deleteById(id);
    }

    // ------------------------------------------------------------- URL derivation

    @Override
    @Transactional(readOnly = true)
    public String resolveEffectiveSpecUrl(Long applicationId) {
        Application app = findEntity(applicationId);
        return resolveEffectiveSpecUrl(app);
    }

    private static final String CONFIG_TYPE_SWAGGER_URL = "SwaggerUrl";
    private static final String ENV_NAME_UAT = "UAT";
    private static final String ENV_NAME_DEV = "DEV";

    private String resolveEffectiveSpecUrl(Application app) {
        Project project = app.getProject();
        EnvironmentConfig swaggerEnv = resolveSwaggerEnvironment(project.getId());

        String base = swaggerEnv.getBaseUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }

        String suffix = project.getSpecPathSuffix() == null || project.getSpecPathSuffix().isBlank()
                ? defaultSwaggerSuffix
                : project.getSpecPathSuffix();
        if (!suffix.startsWith("/")) {
            suffix = "/" + suffix;
        }

        return base + "/" + app.getName().toLowerCase() + suffix;
    }

    /**
     * UAT takes priority over DEV when both have a SwaggerUrl config entry
     * for the project; falls back to DEV if UAT isn't configured.
     */
    private EnvironmentConfig resolveSwaggerEnvironment(Long projectId) {
        List<EnvironmentConfig> swaggerConfigs = environmentConfigRepository.findByProjectId(projectId).stream()
                .filter(EnvironmentConfig::isActive)
                .filter(e -> CONFIG_TYPE_SWAGGER_URL.equalsIgnoreCase(e.getConfigType()))
                .toList();

        return swaggerConfigs.stream()
                .filter(e -> ENV_NAME_UAT.equalsIgnoreCase(e.getEnvName()))
                .findFirst()
                .or(() -> swaggerConfigs.stream()
                        .filter(e -> ENV_NAME_DEV.equalsIgnoreCase(e.getEnvName()))
                        .findFirst())
                .orElseThrow(() -> new BusinessException(
                        "No active SwaggerUrl environment config found for project id=" + projectId + " (checked UAT, then DEV)",
                        "SWAGGER_ENV_NOT_FOUND"));
    }

    // ------------------------------------------------------------- Spec ingestion (versioned)

    @Override
    public ApplicationResponseDto uploadSpec(Long applicationId, MultipartFile file) {
        Application app = findEntity(applicationId);
        String content;
        try {
            content = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new BusinessException("Unable to read uploaded specification file", "SPEC_READ_ERROR");
        }
        ingestContent(app, content, file.getOriginalFilename(), SpecSource.UPLOAD);
        return toResponseDtoWithVersionInfo(app);
    }

    @Override
    public SpecFetchResultDto fetchSpecFromUrl(Long applicationId) {
        Application app = findEntity(applicationId);
        String url = resolveEffectiveSpecUrl(app);
        String content = specFetchService.fetchSpecContent(url, app.getProject());

        app.setSpecSourceUrl(url); // record what was actually used, useful for DERIVED mode audit too
        SpecIngestResult ingest = ingestContent(app, content, deriveFileName(url), SpecSource.FETCH_URL);

        if (ingest.contentUnchanged()) {
            throw new BusinessException("No change in specification file.", "SPEC_UNCHANGED");
        }

        if (ingest.previousCurrentContent() == null) {
            // First version ever for this application - nothing to diff against.
            return new SpecFetchResultDto(true, "Initial swagger specification captured and set as current",
                    toResponseDtoWithVersionInfo(app), toVersionDto(ingest.resultVersion()), List.of());
        }

        List<EndpointFieldDiffDto> endpointDiffs = specDiffService.diffFields(ingest.previousCurrentContent(), content);
        return new SpecFetchResultDto(true, "Changes detected in the swagger specification - review the endpoint differences",
                toResponseDtoWithVersionInfo(app), toVersionDto(ingest.resultVersion()), endpointDiffs);
    }

    /** Outcome of {@link #ingestContent}: whether content changed vs. CURRENT, the relevant version row, and the previous CURRENT content (for diffing), or null if there was none yet. */
    private record SpecIngestResult(boolean contentUnchanged, SpecVersion resultVersion, String previousCurrentContent) {}

    /**
     * Core hash-guarded versioning rule:
     *  - identical hash to CURRENT -> no new row, just bump lastCheckedAt
     *  - identical hash to an existing REJECTED version -> don't re-flag, bump lastCheckedAt on it
     *  - identical hash to an existing PENDING version -> don't duplicate, bump lastCheckedAt
     *  - genuinely new content, no CURRENT exists yet (first ever) -> becomes CURRENT immediately
     *  - genuinely new content, a CURRENT already exists -> becomes PENDING, awaits manual approve/reject
     */
    private SpecIngestResult ingestContent(Application app, String content, String fileName, SpecSource source) {
        String hash = sha256(content);
        Instant now = Instant.now();

        Optional<SpecVersion> current = specVersionRepository.findByApplicationIdAndStatus(app.getId(), SpecVersionStatus.CURRENT);
        String previousCurrentContent = current.map(SpecVersion::getContent).orElse(null);

        if (current.isPresent() && current.get().getContentHash().equals(hash)) {
            current.get().setLastCheckedAt(now);
            specVersionRepository.save(current.get());
            log.info("Spec content for application id={} unchanged (hash match) - no new version created", app.getId());
            return new SpecIngestResult(true, current.get(), previousCurrentContent);
        }

        Optional<SpecVersion> existingSameHash = specVersionRepository.findByApplicationIdAndContentHash(app.getId(), hash);
        if (existingSameHash.isPresent()) {
            SpecVersion existing = existingSameHash.get();
            existing.setLastCheckedAt(now);
            specVersionRepository.save(existing);
            log.info("Spec content for application id={} matches an existing {} version (id={}) - not re-flagging",
                    app.getId(), existing.getStatus(), existing.getId());
            return new SpecIngestResult(false, existing, previousCurrentContent);
        }

        int nextVersionNumber = (int) specVersionRepository.countByApplicationId(app.getId()) + 1;

        SpecVersion version = new SpecVersion();
        version.setApplication(app);
        version.setVersionNumber(nextVersionNumber);
        version.setContent(content);
        version.setContentHash(hash);
        version.setFileName(fileName);
        version.setSource(source);
        version.setFetchedAt(now);
        version.setLastCheckedAt(now);

        if (current.isEmpty()) {
            // First version ever for this application - nothing depends on it yet, goes live immediately.
            version.setStatus(SpecVersionStatus.CURRENT);
            applySpecVersionToApplication(app, version);
            log.info("First spec version stored for application id={} - promoted to CURRENT immediately", app.getId());
        } else {
            version.setStatus(SpecVersionStatus.PENDING);
            log.info("New spec version (v{}) detected for application id={} via {} - held PENDING for review",
                    nextVersionNumber, app.getId(), source);
        }
        specVersionRepository.save(version);
        return new SpecIngestResult(false, version, previousCurrentContent);
    }

    private void applySpecVersionToApplication(Application app, SpecVersion version) {
        app.setSpecContent(version.getContent());
        app.setSpecFileName(version.getFileName());
        app.setSpecLastFetchedAt(version.getFetchedAt());
        applicationRepository.save(app);
    }

    private String deriveFileName(String url) {
        if (url == null) return null;
        int idx = url.lastIndexOf('/');
        return idx >= 0 && idx < url.length() - 1 ? url.substring(idx + 1) : url;
    }

    private String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new BusinessException("Unable to hash spec content", "HASH_ERROR");
        }
    }

    // ------------------------------------------------------------- Versions / diff / approval

    @Override
    @Transactional(readOnly = true)
    public List<SpecVersionResponseDto> listSpecVersions(Long applicationId) {
        return specVersionRepository.findByApplicationIdOrderByVersionNumberDesc(applicationId).stream()
                .map(this::toVersionDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SpecDiffEntryDto> diffAgainstCurrent(Long applicationId, Long specVersionId) {
        SpecVersion target = findVersionEntity(applicationId, specVersionId);
        SpecVersion current = specVersionRepository.findByApplicationIdAndStatus(applicationId, SpecVersionStatus.CURRENT)
                .orElse(null);
        String oldContent = current != null ? current.getContent() : "";
        List<SpecDiffEntry> diff = specDiffService.diff(oldContent, target.getContent());
        return diff.stream().map(e -> new SpecDiffEntryDto(e.changeType(), e.endpoint(), e.description())).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SpecVersionImpactDto getImpact(Long applicationId, Long specVersionId) {
        List<SpecDiffEntryDto> changes = diffAgainstCurrent(applicationId, specVersionId);

        List<TestScenario> scenarios = scenarioRepository.findByApplicationId(applicationId);
        List<SpecVersionImpactDto.AffectedScenarioDto> affected = scenarios.stream()
                .filter(s -> changes.stream().anyMatch(c -> endpointMatches(c.endpoint(), s.getHttpMethod(), s.getEndpoint())))
                .map(s -> new SpecVersionImpactDto.AffectedScenarioDto(s.getId(), s.getName(), s.getHttpMethod() + " " + s.getEndpoint()))
                .toList();

        return new SpecVersionImpactDto(specVersionId, changes, affected.size(), affected);
    }

    private boolean endpointMatches(String diffEndpointKey, String httpMethod, String endpoint) {
        if (httpMethod == null || endpoint == null) return false;
        String scenarioKey = httpMethod.toUpperCase() + " " + endpoint;
        return diffEndpointKey.equalsIgnoreCase(scenarioKey);
    }

    @Override
    public ApplicationResponseDto approveSpecVersion(Long applicationId, Long specVersionId, String reviewedBy) {
        Application app = findEntity(applicationId);
        SpecVersion target = findVersionEntity(applicationId, specVersionId);

        if (target.getStatus() != SpecVersionStatus.PENDING) {
            throw new BusinessException("Only a PENDING version can be approved (current status: " + target.getStatus() + ")", "INVALID_VERSION_STATUS");
        }

        specVersionRepository.findByApplicationIdAndStatus(applicationId, SpecVersionStatus.CURRENT).ifPresent(oldCurrent -> {
            oldCurrent.setStatus(SpecVersionStatus.SUPERSEDED);
            specVersionRepository.save(oldCurrent);
        });

        target.setStatus(SpecVersionStatus.CURRENT);
        target.setReviewedAt(Instant.now());
        target.setReviewedBy(reviewedBy);
        specVersionRepository.save(target);

        applySpecVersionToApplication(app, target);
        log.info("Spec version v{} approved and promoted to CURRENT for application id={} by {}",
                target.getVersionNumber(), applicationId, reviewedBy);

        return toResponseDtoWithVersionInfo(app);
    }

    @Override
    public SpecVersionResponseDto rejectSpecVersion(Long applicationId, Long specVersionId, String reviewedBy) {
        SpecVersion target = findVersionEntity(applicationId, specVersionId);
        if (target.getStatus() != SpecVersionStatus.PENDING) {
            throw new BusinessException("Only a PENDING version can be rejected (current status: " + target.getStatus() + ")", "INVALID_VERSION_STATUS");
        }
        target.setStatus(SpecVersionStatus.REJECTED);
        target.setReviewedAt(Instant.now());
        target.setReviewedBy(reviewedBy);
        SpecVersion saved = specVersionRepository.save(target);
        log.info("Spec version v{} rejected for application id={} by {} - this exact content will not be re-flagged",
                target.getVersionNumber(), applicationId, reviewedBy);
        return toVersionDto(saved);
    }

    // ------------------------------------------------------------- helpers

    private ApplicationResponseDto toResponseDtoWithVersionInfo(Application entity) {
        ApplicationResponseDto base = applicationMapper.toResponseDto(entity);
        Integer currentVersionNumber = specVersionRepository.findByApplicationIdAndStatus(entity.getId(), SpecVersionStatus.CURRENT)
                .map(SpecVersion::getVersionNumber).orElse(null);
        boolean hasPending = !specVersionRepository.findByApplicationIdAndStatusOrderByVersionNumberDesc(entity.getId(), SpecVersionStatus.PENDING).isEmpty();

        return new ApplicationResponseDto(
                base.id(), base.projectId(), base.projectName(), base.referenceEnvironmentId(), base.referenceEnvironmentName(),
                base.name(), base.description(), base.applicationType(), base.specFormat(), base.specFileName(),
                base.specSourceMode(), base.specSourceUrl(), currentVersionNumber, hasPending, base.specLastFetchedAt(),
                base.autoSyncEnabled(), base.autoSyncIntervalMinutes(), base.status(), base.createdAt(), base.updatedAt()
        );
    }

    private SpecVersionResponseDto toVersionDto(SpecVersion v) {
        return new SpecVersionResponseDto(
                v.getId(), v.getApplication().getId(), v.getVersionNumber(), v.getFileName(), v.getContentHash(),
                v.getSource().name(), v.getStatus().name(), v.getFetchedAt(), v.getLastCheckedAt(),
                v.getReviewedAt(), v.getReviewedBy()
        );
    }

    private Application findEntity(Long id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Application", id));
    }

    private SpecVersion findVersionEntity(Long applicationId, Long specVersionId) {
        SpecVersion version = specVersionRepository.findById(specVersionId)
                .orElseThrow(() -> ResourceNotFoundException.of("SpecVersion", specVersionId));
        if (!version.getApplication().getId().equals(applicationId)) {
            throw new BusinessException("Spec version does not belong to this application", "VERSION_APPLICATION_MISMATCH");
        }
        return version;
    }
}
