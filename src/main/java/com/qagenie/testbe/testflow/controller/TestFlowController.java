package com.qagenie.testbe.testflow.controller;

import com.qagenie.testbe.common.response.ApiResponse;
import com.qagenie.testbe.testflow.dto.TestFlowRequestDto;
import com.qagenie.testbe.testflow.dto.TestFlowResponseDto;
import com.qagenie.testbe.testflow.service.TestFlowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/test-flows")
@RequiredArgsConstructor
@Tag(name = "Test Flows", description = "Chained scenarios executed as an ordered flow")
@SecurityRequirement(name = "bearerAuth")
public class TestFlowController {

    private final TestFlowService testFlowService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','TESTER')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a test flow made of ordered scenario steps")
    public ApiResponse<TestFlowResponseDto> create(@Valid @RequestBody TestFlowRequestDto request) {
        return ApiResponse.ok("Test flow created", testFlowService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TESTER')")
    @Operation(summary = "Update a test flow and its steps")
    public ApiResponse<TestFlowResponseDto> update(@PathVariable Long id, @Valid @RequestBody TestFlowRequestDto request) {
        return ApiResponse.ok("Test flow updated", testFlowService.update(id, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a test flow by id")
    public ApiResponse<TestFlowResponseDto> getById(@PathVariable Long id) {
        return ApiResponse.ok(testFlowService.getById(id));
    }

    @GetMapping("/application/{applicationId}")
    @Operation(summary = "List test flows for an application")
    public ApiResponse<List<TestFlowResponseDto>> listByApplication(@PathVariable Long applicationId) {
        return ApiResponse.ok(testFlowService.listByApplication(applicationId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TESTER')")
    @Operation(summary = "Delete a test flow")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        testFlowService.delete(id);
        return ApiResponse.ok("Test flow deleted", null);
    }
}
