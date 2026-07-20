package com.qagenie.testbe.environment.controller;

import com.qagenie.testbe.common.response.ApiResponse;
import com.qagenie.testbe.environment.dto.EnvironmentRequestDto;
import com.qagenie.testbe.environment.dto.EnvironmentResponseDto;
import com.qagenie.testbe.environment.service.EnvironmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Environment configuration - can be added MANY times per onboarded project
 * (Dev / Staging / Prod / custom). Separate lifecycle from ProjectController.
 */
@RestController
@RequestMapping("/api/v1/environments")
@RequiredArgsConstructor
@Tag(name = "Environment Onboarding", description = "Add/manage repeatable per-environment execution configs (name, base URL) for a project")
@SecurityRequirement(name = "bearerAuth")
public class EnvironmentController {

    private final EnvironmentService environmentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','TESTER')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a new environment configuration to a project")
    public ApiResponse<EnvironmentResponseDto> create(@Valid @RequestBody EnvironmentRequestDto request) {
        return ApiResponse.ok("Environment added", environmentService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TESTER')")
    @Operation(summary = "Update an environment configuration")
    public ApiResponse<EnvironmentResponseDto> update(@PathVariable Long id, @Valid @RequestBody EnvironmentRequestDto request) {
        return ApiResponse.ok("Environment updated", environmentService.update(id, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an environment configuration by id")
    public ApiResponse<EnvironmentResponseDto> getById(@PathVariable Long id) {
        return ApiResponse.ok(environmentService.getById(id));
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "List all environments configured for a project")
    public ApiResponse<List<EnvironmentResponseDto>> listByProject(@PathVariable Long projectId) {
        return ApiResponse.ok(environmentService.listByProject(projectId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TESTER')")
    @Operation(summary = "Remove an environment configuration")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        environmentService.delete(id);
        return ApiResponse.ok("Environment removed", null);
    }
}
