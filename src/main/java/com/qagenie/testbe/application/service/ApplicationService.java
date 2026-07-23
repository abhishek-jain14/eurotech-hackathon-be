package com.qagenie.testbe.application.service;

import com.qagenie.testbe.application.dto.*;
import com.qagenie.testbe.scenario.dto.ScenarioGenerationType;
import com.qagenie.testbe.scenario.dto.ScenarioResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ApplicationService {
    ApplicationResponseDto onboard(ApplicationRequestDto request);
    ApplicationResponseDto update(Long id, ApplicationRequestDto request);
    ApplicationResponseDto getById(Long id);
    Page<ApplicationResponseDto> list(Pageable pageable);
    void archive(Long id);
    void delete(Long id);

    /** Manual file upload - versioned (hash-guarded, pending unless it's the first version). */
    ApplicationResponseDto uploadSpec(Long applicationId, MultipartFile file);

    /**
     * Resolves the effective URL (derived or custom), fetches via the parent Project's TLS config, versions the
     * result. If the fetched content hashes the same as the current version, returns changed=false with message
     * "Swagger file is latest" and no diff. Otherwise returns changed=true with a per-endpoint, field-level
     * old/new breakdown of what was added, deleted, renamed, or changed.
     */
    SpecFetchResultDto fetchSpecFromUrl(Long applicationId);

    /** What URL WOULD be used right now, without fetching - for FE preview and for Change Tracker. */
    String resolveEffectiveSpecUrl(Long applicationId);

    List<SpecVersionResponseDto> listSpecVersions(Long applicationId);

    List<SpecDiffEntryDto> diffAgainstCurrent(Long applicationId, Long specVersionId);

    SpecVersionImpactDto getImpact(Long applicationId, Long specVersionId);

    ApplicationResponseDto approveSpecVersion(Long applicationId, Long specVersionId, String reviewedBy);

    SpecVersionResponseDto rejectSpecVersion(Long applicationId, Long specVersionId, String reviewedBy);

    List<ApiEndpoint> getApiEndpoints(Long applicationId);

    /** Synthesizes and persists POSITIVE/NEGATIVE scenarios for a spec version's endpoints, tagged source=AI. */
    List<ScenarioResponseDto> generateScenarios(Long applicationId, Long specVersionId, ScenarioGenerationType type);
}
