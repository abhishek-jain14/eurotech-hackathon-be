package com.qagenie.testbe.application.service;

import com.qagenie.testbe.application.dto.EndpointFieldDiffDto;
import com.qagenie.testbe.application.dto.SpecHealSummaryDto;

import java.util.List;

/**
 * Rewrites existing TEST_SCENARIO parameter descriptors and TEST_DATA values so they stay
 * consistent with a spec version that's about to be promoted to CURRENT - see
 * SpecHealingServiceImpl for exactly what each field-level change type does.
 */
public interface SpecHealingService {
    SpecHealSummaryDto heal(Long applicationId, List<EndpointFieldDiffDto> fieldChanges);
}
