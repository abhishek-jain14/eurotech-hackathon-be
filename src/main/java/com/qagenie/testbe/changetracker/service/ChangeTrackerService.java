package com.qagenie.testbe.changetracker.service;

import com.qagenie.testbe.application.dto.ApplicationResponseDto;
import com.qagenie.testbe.application.dto.SpecVersionImpactDto;
import com.qagenie.testbe.application.dto.SpecVersionResponseDto;

import java.util.List;

/**
 * Thin wrapper over ApplicationService's real spec-version diffing -
 * "analyze" just triggers a fetch (which is already hash-guarded/versioned
 * by ApplicationService), and "pending changes" are simply that
 * application's PENDING spec versions with their diffs attached.
 */
public interface ChangeTrackerService {
    ApplicationResponseDto analyze(Long applicationId);
    List<SpecVersionResponseDto> listPendingVersions(Long applicationId);
    SpecVersionImpactDto getPendingImpact(Long applicationId, Long specVersionId);
    ApplicationResponseDto heal(Long applicationId, Long specVersionId, String reviewedBy);
}
