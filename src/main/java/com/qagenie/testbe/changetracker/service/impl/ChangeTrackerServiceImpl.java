package com.qagenie.testbe.changetracker.service.impl;

import com.qagenie.testbe.application.dto.SpecApprovalResultDto;
import com.qagenie.testbe.application.dto.SpecFetchResultDto;
import com.qagenie.testbe.application.dto.SpecVersionImpactDto;
import com.qagenie.testbe.application.dto.SpecVersionResponseDto;
import com.qagenie.testbe.application.service.ApplicationService;
import com.qagenie.testbe.changetracker.service.ChangeTrackerService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ChangeTrackerServiceImpl implements ChangeTrackerService {

    private static final Logger log = LoggerFactory.getLogger(ChangeTrackerServiceImpl.class);

    private final ApplicationService applicationService;

    @Override
    public SpecFetchResultDto analyze(Long applicationId) {
        log.info("Change analysis triggered for application id={}", applicationId);
        // fetchSpecFromUrl already does hash-guarded versioning: identical
        // content is a no-op, new content lands PENDING automatically. The AI agent
        // toggle only applies to the Onboarding spec screen's fetch/upload, not here.
        return applicationService.fetchSpecFromUrl(applicationId, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SpecVersionResponseDto> listPendingVersions(Long applicationId) {
        return applicationService.listSpecVersions(applicationId).stream()
                .filter(v -> "PENDING".equals(v.status()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SpecVersionImpactDto getPendingImpact(Long applicationId, Long specVersionId) {
        return applicationService.getImpact(applicationId, specVersionId);
    }

    @Override
    public SpecApprovalResultDto heal(Long applicationId, Long specVersionId, String reviewedBy) {
        return applicationService.approveSpecVersion(applicationId, specVersionId, reviewedBy);
    }
}
