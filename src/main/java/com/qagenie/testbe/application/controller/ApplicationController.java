package com.qagenie.testbe.application.controller;

import com.qagenie.testbe.application.dto.*;
import com.qagenie.testbe.application.service.ApplicationService;
import com.qagenie.testbe.common.response.ApiResponse;
import com.qagenie.testbe.common.response.PageResponse;
import com.qagenie.testbe.scenario.dto.GenerateScenariosRequestDto;
import com.qagenie.testbe.scenario.dto.ScenarioResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Application onboarding: pick a Project first (gives you its shared
 * keystore/truststore), then pick one of that Project's Environments to
 * derive the swagger/DOM URL from (baseUrl + applicationName + the
 * project's spec path suffix), or overwrite with a fully custom URL.
 *
 * Every re-upload/re-fetch after the first is hash-guarded and versioned:
 * identical content is a no-op, genuinely new content sits PENDING until
 * approved via the spec-versions endpoints below - nothing goes live
 * automatically.
 *
 * Viewer role: read-only (getById/list/versions/diff/impact).
 * Admin & Tester: full access, including approve/reject.
 */
@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
@Tag(name = "Application Onboarding", description = "Onboard applications under a Project + Environment, with hash-guarded spec versioning and pending-approval drift review")
@SecurityRequirement(name = "bearerAuth")
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','TESTER')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Onboard a new application under a Project",
            description = "specSourceMode=DERIVED requires referenceEnvironmentId (URL = env baseUrl + app name + " +
                    "project's spec path suffix). specSourceMode=CUSTOM requires specSourceUrl directly.")
    public ApiResponse<ApplicationResponseDto> onboard(@Valid @RequestBody ApplicationRequestDto request) {
        return ApiResponse.ok("Application onboarded", applicationService.onboard(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TESTER')")
    @Operation(summary = "Update application metadata (name, description, source mode, reference environment)")
    public ApiResponse<ApplicationResponseDto> update(@PathVariable Long id, @Valid @RequestBody ApplicationRequestDto request) {
        return ApiResponse.ok("Application updated", applicationService.update(id, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an onboarded application by id")
    public ApiResponse<ApplicationResponseDto> getById(@PathVariable Long id) {
        return ApiResponse.ok(applicationService.getById(id));
    }

    @GetMapping
    @Operation(summary = "List onboarded applications")
    public ApiResponse<PageResponse<ApplicationResponseDto>> list(Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(applicationService.list(pageable)));
    }

    @GetMapping("/{id}/resolve-spec-url")
    @Operation(summary = "Preview the URL that would be used right now (derived or custom) without fetching anything")
    public ApiResponse<String> resolveSpecUrl(@PathVariable Long id) {
        return ApiResponse.ok(applicationService.resolveEffectiveSpecUrl(id));
    }

    @PatchMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('ADMIN','TESTER')")
    @Operation(summary = "Archive an application without deleting its history")
    public ApiResponse<Void> archive(@PathVariable Long id) {
        applicationService.archive(id);
        return ApiResponse.ok("Application archived", null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Permanently delete an application and its onboarding record")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        applicationService.delete(id);
        return ApiResponse.ok("Application deleted", null);
    }

    // ---------------------------------------------------------- Spec ingestion

    @PostMapping(value = "/{id}/spec", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('ADMIN','TESTER')")
    @Operation(summary = "Manually upload/replace the spec file",
            description = "Hash-guarded: identical content is a no-op. New content becomes CURRENT immediately " +
                    "if this is the application's first version, otherwise it's held PENDING for review. " +
                    "useAiAgent=true additionally sends the file to the external AI agent to generate test scenarios.")
    public ApiResponse<ApplicationResponseDto> uploadSpec(@PathVariable Long id, @RequestParam("file") MultipartFile file,
                                                           @RequestParam(defaultValue = "false") boolean useAiAgent) {
        return ApiResponse.ok("Specification uploaded", applicationService.uploadSpec(id, file, useAiAgent));
    }

    @PostMapping("/{id}/fetch-spec")
    @PreAuthorize("hasAnyRole('ADMIN','TESTER')")
    @Operation(summary = "Fetch/refresh the spec from the resolved URL (derived or custom)",
            description = "Uses the parent Project's keystore/truststore (or basic/bearer/api-key credential) to " +
                    "reach the endpoint. Fails with a clear TLS_REQUIRED error if the endpoint needs mutual TLS " +
                    "or a custom trust store the Project hasn't been configured with yet. Same hash-guard/pending " +
                    "rules as manual upload apply. useAiAgent=true additionally sends the fetched spec to the " +
                    "external AI agent to generate test scenarios.")
    public ApiResponse<SpecFetchResultDto> fetchSpec(@PathVariable Long id,
                                                      @RequestParam(defaultValue = "false") boolean useAiAgent) {
        SpecFetchResultDto result = applicationService.fetchSpecFromUrl(id, useAiAgent);
        return ApiResponse.ok(result.message(), result);
    }

    // ---------------------------------------------------------- Version history / approval

    @GetMapping("/{id}/spec-versions")
    @Operation(summary = "List spec version history (CURRENT/PENDING/SUPERSEDED/REJECTED) for an application")
    public ApiResponse<List<SpecVersionResponseDto>> listVersions(@PathVariable Long id) {
        return ApiResponse.ok(applicationService.listSpecVersions(id));
    }

    @GetMapping("/{id}/spec-versions/{versionId}/diff")
    @Operation(summary = "Diff a spec version against the current live version")
    public ApiResponse<List<SpecDiffEntryDto>> diff(@PathVariable Long id, @PathVariable Long versionId) {
        return ApiResponse.ok(applicationService.diffAgainstCurrent(id, versionId));
    }

    @GetMapping("/{id}/spec-versions/{versionId}/impact")
    @Operation(summary = "Diff plus which existing scenarios reference the changed endpoints",
            description = "Use this before approving a pending version to see what it would affect.")
    public ApiResponse<SpecVersionImpactDto> impact(@PathVariable Long id, @PathVariable Long versionId) {
        return ApiResponse.ok(applicationService.getImpact(id, versionId));
    }

    @PostMapping("/{id}/spec-versions/{versionId}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','TESTER')")
    @Operation(summary = "Approve a pending spec version, promoting it to CURRENT",
            description = "The previous CURRENT version is demoted to SUPERSEDED, never deleted.")
    public ApiResponse<SpecApprovalResultDto> approve(@PathVariable Long id, @PathVariable Long versionId, Authentication auth) {
        String reviewer = auth != null ? auth.getName() : "SYSTEM";
        return ApiResponse.ok("Spec version approved", applicationService.approveSpecVersion(id, versionId, reviewer));
    }

    @PostMapping("/{id}/spec-versions/{versionId}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','TESTER')")
    @Operation(summary = "Reject a pending spec version",
            description = "Terminal - this exact content will not be re-flagged again on future fetches.")
    public ApiResponse<SpecVersionResponseDto> reject(@PathVariable Long id, @PathVariable Long versionId, Authentication auth) {
        String reviewer = auth != null ? auth.getName() : "SYSTEM";
        return ApiResponse.ok("Spec version rejected", applicationService.rejectSpecVersion(id, versionId, reviewer));
    }

    @GetMapping("/{id}/fetch-endpoints")
    @PreAuthorize("hasAnyRole('ADMIN','TESTER')")
    @Operation(summary = "Approve a pending spec version, promoting it to CURRENT",
            description = "The previous CURRENT version is demoted to SUPERSEDED, never deleted.")
    public ApiResponse<List<ApiEndpoint>> fetchEndpoints(@PathVariable Long id) {

        return ApiResponse.ok("Apis Found", applicationService.getApiEndpoints(id));
    }

    @PostMapping("/{id}/spec-versions/{versionId}/generate-scenarios")
    @PreAuthorize("hasAnyRole('ADMIN','TESTER')")
    @Operation(summary = "Generate test scenarios for a spec version",
            description = "Synthesizes POSITIVE/NEGATIVE scenarios for every endpoint in the given spec version, " +
                    "tagged source=AI. Uses a real LLM call when qagenie.scenario-generation.use-ai=true, otherwise " +
                    "a deterministic in-code generator (no external call, no API key needed).")
    public ApiResponse<List<ScenarioResponseDto>> generateScenarios(@PathVariable Long id, @PathVariable Long versionId,
                                                                     @Valid @RequestBody GenerateScenariosRequestDto request) {
        return ApiResponse.ok("Scenarios generated", applicationService.generateScenarios(id, versionId, request.scenarioType(), request.prompt()));
    }
}
