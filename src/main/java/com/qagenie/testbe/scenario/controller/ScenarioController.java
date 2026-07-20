package com.qagenie.testbe.scenario.controller;

import com.qagenie.testbe.common.response.ApiResponse;
import com.qagenie.testbe.common.response.PageResponse;
import com.qagenie.testbe.scenario.dto.ScenarioRequestDto;
import com.qagenie.testbe.scenario.dto.ScenarioResponseDto;
import com.qagenie.testbe.scenario.service.ScenarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/scenarios")
@RequiredArgsConstructor
@Tag(name = "Test Scenarios", description = "AI-generated, manual and Jira-sourced test scenarios per application")
@SecurityRequirement(name = "bearerAuth")
public class ScenarioController {

    private final ScenarioService scenarioService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','TESTER')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a test scenario")
    public ApiResponse<ScenarioResponseDto> create(@Valid @RequestBody ScenarioRequestDto request) {
        return ApiResponse.ok("Scenario created", scenarioService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TESTER')")
    @Operation(summary = "Update a test scenario")
    public ApiResponse<ScenarioResponseDto> update(@PathVariable Long id, @Valid @RequestBody ScenarioRequestDto request) {
        return ApiResponse.ok("Scenario updated", scenarioService.update(id, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a test scenario by id")
    public ApiResponse<ScenarioResponseDto> getById(@PathVariable Long id) {
        return ApiResponse.ok(scenarioService.getById(id));
    }

    @GetMapping("/application/{applicationId}")
    @Operation(summary = "List scenarios for an application")
    public ApiResponse<PageResponse<ScenarioResponseDto>> listByApplication(@PathVariable Long applicationId, Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(scenarioService.listByApplication(applicationId, pageable)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TESTER')")
    @Operation(summary = "Delete a test scenario")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        scenarioService.delete(id);
        return ApiResponse.ok("Scenario deleted", null);
    }
}
