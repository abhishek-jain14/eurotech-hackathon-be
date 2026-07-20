package com.qagenie.testbe.project.controller;

import com.qagenie.testbe.common.response.ApiResponse;
import com.qagenie.testbe.common.response.PageResponse;
import com.qagenie.testbe.project.dto.ProjectRequestDto;
import com.qagenie.testbe.project.dto.ProjectResponseDto;
import com.qagenie.testbe.project.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Project = the top-level grouping that owns shared keystore/truststore
 * material and a set of Environments. Applications are onboarded UNDER a
 * Project (see ApplicationController) and inherit its TLS config when
 * fetching their spec.
 *
 * Creating/editing a Project (and especially its TLS config) is ADMIN-only
 * since it governs credentials shared by everything under it; Tester can
 * still create Applications/Environments within an existing Project.
 */
@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
@Tag(name = "Projects", description = "Top-level grouping owning shared keystore/truststore + Environments, under which Applications are onboarded")
@SecurityRequirement(name = "bearerAuth")
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new project")
    public ApiResponse<ProjectResponseDto> create(@Valid @RequestBody ProjectRequestDto request) {
        return ApiResponse.ok("Project created", projectService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update project metadata (name, description, spec path suffix)")
    public ApiResponse<ProjectResponseDto> update(@PathVariable Long id, @Valid @RequestBody ProjectRequestDto request) {
        return ApiResponse.ok("Project updated", projectService.update(id, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a project by id")
    public ApiResponse<ProjectResponseDto> getById(@PathVariable Long id) {
        return ApiResponse.ok(projectService.getById(id));
    }

    @GetMapping
    @Operation(summary = "List projects")
    public ApiResponse<PageResponse<ProjectResponseDto>> list(Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(projectService.list(pageable)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a project (only if no applications/environments remain under it)")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        projectService.delete(id);
        return ApiResponse.ok("Project deleted", null);
    }

    @PostMapping(value = "/{id}/tls-config", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Configure the shared keystore/truststore (or basic/bearer/api-key credential) used to fetch specs for every application under this project",
            description = "authType = NONE | BASIC | BEARER | API_KEY | MUTUAL_TLS. For MUTUAL_TLS, upload a " +
                    "keystoreFile (client cert) and optionally a truststoreFile (custom CA) with their " +
                    "passwords; files are stored outside the database and referenced by path only.")
    public ApiResponse<ProjectResponseDto> configureTlsAuth(
            @PathVariable Long id,
            @RequestParam String authType,
            @RequestParam(required = false) MultipartFile keystoreFile,
            @RequestParam(required = false) String keystorePassword,
            @RequestParam(required = false) MultipartFile truststoreFile,
            @RequestParam(required = false) String truststorePassword,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String password,
            @RequestParam(required = false) String bearerToken,
            @RequestParam(required = false) String apiKeyHeaderName,
            @RequestParam(required = false) String apiKeyValue) {
        return ApiResponse.ok("TLS/auth config saved", projectService.configureTlsAuth(
                id, authType, keystoreFile, keystorePassword, truststoreFile, truststorePassword,
                username, password, bearerToken, apiKeyHeaderName, apiKeyValue));
    }
}
