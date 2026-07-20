package com.qagenie.testbe.dashboard.controller;

import com.qagenie.testbe.common.response.ApiResponse;
import com.qagenie.testbe.dashboard.dto.DashboardStatsDto;
import com.qagenie.testbe.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Overall platform statistics across all onboarded applications")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    @Operation(summary = "Get overall dashboard statistics")
    public ApiResponse<DashboardStatsDto> getStats() {
        return ApiResponse.ok(dashboardService.getOverallStats());
    }
}
