package com.qagenie.testbe.coverage.service;

import com.qagenie.testbe.coverage.dto.CoverageOverviewDto;
import com.qagenie.testbe.coverage.dto.EndpointCoverageDto;

import java.util.List;

public interface CoverageService {
    /** Platform-wide stat strip + one summary row per application. */
    CoverageOverviewDto getOverview();

    /** Per-endpoint breakdown for one application (the expandable detail row). */
    List<EndpointCoverageDto> getApplicationCoverage(Long applicationId);
}
