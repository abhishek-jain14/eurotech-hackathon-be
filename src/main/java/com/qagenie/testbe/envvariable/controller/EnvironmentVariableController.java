package com.qagenie.testbe.envvariable.controller;

import com.qagenie.testbe.common.response.ApiResponse;
import com.qagenie.testbe.envvariable.dto.EnvironmentVariableRequestDto;
import com.qagenie.testbe.envvariable.dto.EnvironmentVariableResponseDto;
import com.qagenie.testbe.envvariable.service.EnvironmentVariableService;
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
 * Backs the "Manage" button on the Project list - a simple name/value
 * config store per Project, separate from EnvironmentController's
 * execution-oriented EnvironmentConfig.
 */
@RestController
@RequestMapping("/api/v1/project-env-variables")
@RequiredArgsConstructor
@Tag(name = "Project Environment Variables", description = "Manage simple name/value config pairs for a project")
@SecurityRequirement(name = "bearerAuth")
public class EnvironmentVariableController {

    private final EnvironmentVariableService environmentVariableService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','TESTER')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a new environment variable to a project")
    public ApiResponse<EnvironmentVariableResponseDto> create(@Valid @RequestBody EnvironmentVariableRequestDto request) {
        return ApiResponse.ok("Environment variable added", environmentVariableService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TESTER')")
    @Operation(summary = "Update an environment variable")
    public ApiResponse<EnvironmentVariableResponseDto> update(@PathVariable Long id, @Valid @RequestBody EnvironmentVariableRequestDto request) {
        return ApiResponse.ok("Environment variable updated", environmentVariableService.update(id, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an environment variable by id")
    public ApiResponse<EnvironmentVariableResponseDto> getById(@PathVariable Long id) {
        return ApiResponse.ok(environmentVariableService.getById(id));
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "List all environment variables for a project")
    public ApiResponse<List<EnvironmentVariableResponseDto>> listByProject(@PathVariable Long projectId) {
        return ApiResponse.ok(environmentVariableService.listByProject(projectId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TESTER')")
    @Operation(summary = "Remove an environment variable")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        environmentVariableService.delete(id);
        return ApiResponse.ok("Environment variable removed", null);
    }
}