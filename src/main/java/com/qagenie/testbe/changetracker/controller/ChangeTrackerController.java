package com.qagenie.testbe.changetracker.controller;

import com.qagenie.testbe.application.dto.SpecApprovalResultDto;
import com.qagenie.testbe.application.dto.SpecFetchResultDto;
import com.qagenie.testbe.application.dto.SpecVersionImpactDto;
import com.qagenie.testbe.application.dto.SpecVersionResponseDto;
import com.qagenie.testbe.changetracker.service.ChangeTrackerService;
import com.qagenie.testbe.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Change Tracker screen, now backed entirely by real spec-version diffing
 * (see application.service.ApplicationService) instead of placeholder
 * records: "analyze" triggers a hash-guarded re-fetch, "pending" lists
 * whatever landed in PENDING status, and "heal" approves one.
 */
@RestController
@RequestMapping("/api/v1/change-tracker")
@RequiredArgsConstructor
@Tag(name = "Change Tracker", description = "Specification drift detection backed by real spec-version diffing, with pending-approval healing")
@SecurityRequirement(name = "bearerAuth")
public class ChangeTrackerController {

    private final ChangeTrackerService changeTrackerService;

    @PostMapping("/application/{applicationId}/analyze")
    @PreAuthorize("hasAnyRole('ADMIN','TESTER')")
    @Operation(summary = "Re-fetch the live spec and compare against the current version",
            description = "Identical content is a no-op. Genuinely different content lands as a new PENDING " +
                    "spec version - see /api/v1/applications/{id}/spec-versions for the result.")
    public ApiResponse<SpecFetchResultDto> analyze(@PathVariable Long applicationId) {
        SpecFetchResultDto result = changeTrackerService.analyze(applicationId);
        return ApiResponse.ok(result.message(), result);
    }

    @GetMapping("/application/{applicationId}")
    @Operation(summary = "List pending (unreviewed) spec versions for an application")
    public ApiResponse<List<SpecVersionResponseDto>> listPending(@PathVariable Long applicationId) {
        return ApiResponse.ok(changeTrackerService.listPendingVersions(applicationId));
    }

    @GetMapping("/application/{applicationId}/spec-versions/{versionId}/impact")
    @Operation(summary = "Diff plus affected scenarios for a pending version")
    public ApiResponse<SpecVersionImpactDto> impact(@PathVariable Long applicationId, @PathVariable Long versionId) {
        return ApiResponse.ok(changeTrackerService.getPendingImpact(applicationId, versionId));
    }

    @PatchMapping("/application/{applicationId}/spec-versions/{versionId}/heal")
    @PreAuthorize("hasAnyRole('ADMIN','TESTER')")
    @Operation(summary = "Approve (heal) a single pending spec version")
    public ApiResponse<SpecApprovalResultDto> heal(@PathVariable Long applicationId, @PathVariable Long versionId, Authentication auth) {
        String reviewer = auth != null ? auth.getName() : "SYSTEM";
        return ApiResponse.ok("Spec version healed", changeTrackerService.heal(applicationId, versionId, reviewer));
    }
}
