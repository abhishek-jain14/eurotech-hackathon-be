package com.qagenie.testbe.coverage.controller;

import com.qagenie.testbe.common.response.ApiResponse;
import com.qagenie.testbe.coverage.dto.CoverageOverviewDto;
import com.qagenie.testbe.coverage.dto.EndpointCoverageDto;
import com.qagenie.testbe.coverage.service.CoverageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/coverage")
@RequiredArgsConstructor
@Tag(name = "Coverage", description = "Endpoint-level test coverage - which endpoints have scenarios, negative tests, a flow, test data, and how they last ran")
@SecurityRequirement(name = "bearerAuth")
public class CoverageController {

    private final CoverageService coverageService;

    @GetMapping("/overview")
    @Operation(summary = "Platform-wide coverage stat strip plus one summary row per application")
    public ApiResponse<CoverageOverviewDto> getOverview() {
        return ApiResponse.ok(coverageService.getOverview());
    }

    @GetMapping("/applications/{applicationId}")
    @Operation(summary = "Per-endpoint coverage breakdown for one application")
    public ApiResponse<List<EndpointCoverageDto>> getApplicationCoverage(@PathVariable Long applicationId) {
        return ApiResponse.ok(coverageService.getApplicationCoverage(applicationId));
    }
}
