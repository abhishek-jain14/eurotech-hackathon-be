package com.qagenie.testbe.report.controller;

import com.qagenie.testbe.common.response.ApiResponse;
import com.qagenie.testbe.report.dto.ReportSummaryDto;
import com.qagenie.testbe.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Aggregated pass-rate, failure and duration insights per application")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/application/{applicationId}/summary")
    @Operation(summary = "Get aggregated report summary for an application")
    public ApiResponse<ReportSummaryDto> getSummary(@PathVariable Long applicationId) {
        return ApiResponse.ok(reportService.getSummaryForApplication(applicationId));
    }
}
