package com.qagenie.testbe.report.controller;

import com.qagenie.testbe.common.response.ApiResponse;
import com.qagenie.testbe.report.dto.ReportDetailDto;
import com.qagenie.testbe.report.dto.ReportSignoffDto;
import com.qagenie.testbe.report.dto.ReportSummaryDto;
import com.qagenie.testbe.report.dto.SignoffRequestDto;
import com.qagenie.testbe.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Aggregated pass-rate, failure, flakiness and release sign-off insights per application")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/application/{applicationId}/summary")
    @Operation(summary = "All-time aggregated report summary for an application")
    public ApiResponse<ReportSummaryDto> getSummary(@PathVariable Long applicationId) {
        return ApiResponse.ok(reportService.getSummaryForApplication(applicationId));
    }

    @GetMapping("/application/{applicationId}/detail")
    @Operation(summary = "Latest-run-focused report: stats, pass/fail, comparison vs previous run, flakiness, failure detail")
    public ApiResponse<ReportDetailDto> getDetail(@PathVariable Long applicationId) {
        return ApiResponse.ok(reportService.getDetailForApplication(applicationId));
    }

    @GetMapping("/signoff")
    @Operation(summary = "Release sign-off status for every application")
    public ApiResponse<List<ReportSignoffDto>> listSignoffs() {
        return ApiResponse.ok(reportService.listSignoffs());
    }

    @PostMapping("/application/{applicationId}/signoff")
    @PreAuthorize("hasAnyRole('ADMIN','TESTER')")
    @Operation(summary = "Approve or reject an application's latest run for release", description = "Signs off against whatever the latest run is at the time of the call.")
    public ApiResponse<ReportSignoffDto> signOff(@PathVariable Long applicationId, @Valid @RequestBody SignoffRequestDto request, Authentication auth) {
        String signedOffBy = auth != null ? auth.getName() : "SYSTEM";
        return ApiResponse.ok("Sign-off recorded", reportService.signOff(applicationId, request, signedOffBy));
    }
}
