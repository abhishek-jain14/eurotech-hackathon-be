package com.qagenie.testbe.execution.controller;

import com.qagenie.testbe.common.response.ApiResponse;
import com.qagenie.testbe.common.response.PageResponse;
import com.qagenie.testbe.execution.dto.ExecutionRequestDto;
import com.qagenie.testbe.execution.dto.ExecutionRunResponseDto;
import com.qagenie.testbe.execution.service.ExecutionService;
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
@RequestMapping("/api/v1/executions")
@RequiredArgsConstructor
@Tag(name = "Execution", description = "Trigger and track suite executions against a chosen environment")
@SecurityRequirement(name = "bearerAuth")
public class ExecutionController {

    private final ExecutionService executionService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','TESTER')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Trigger a suite execution", description = "Runs the given scenario ids against the selected environment and returns the completed run with results.")
    public ApiResponse<ExecutionRunResponseDto> trigger(@Valid @RequestBody ExecutionRequestDto request) {
        return ApiResponse.ok("Execution completed", executionService.trigger(request));
    }

    @GetMapping("/{runId}")
    @Operation(summary = "Get a run and its scenario-level results")
    public ApiResponse<ExecutionRunResponseDto> getRun(@PathVariable Long runId) {
        return ApiResponse.ok(executionService.getRunById(runId));
    }

    @GetMapping("/application/{applicationId}")
    @Operation(summary = "List execution runs for an application")
    public ApiResponse<PageResponse<ExecutionRunResponseDto>> listByApplication(@PathVariable Long applicationId, Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(executionService.listByApplication(applicationId, pageable)));
    }
}
