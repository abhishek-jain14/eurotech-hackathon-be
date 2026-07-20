package com.qagenie.testbe.testdata.controller;

import com.qagenie.testbe.common.response.ApiResponse;
import com.qagenie.testbe.common.response.PageResponse;
import com.qagenie.testbe.testdata.dto.TestDataRequestDto;
import com.qagenie.testbe.testdata.dto.TestDataResponseDto;
import com.qagenie.testbe.testdata.service.TestDataService;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/test-data")
@RequiredArgsConstructor
@Tag(name = "Test Data", description = "AI prompt, single-entry and bulk-upload test data records")
@SecurityRequirement(name = "bearerAuth")
public class TestDataController {

    private final TestDataService testDataService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','TESTER')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a single test data record")
    public ApiResponse<TestDataResponseDto> create(@Valid @RequestBody TestDataRequestDto request) {
        return ApiResponse.ok("Test data created", testDataService.create(request));
    }

    @PostMapping(value = "/application/{applicationId}/bulk-upload", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('ADMIN','TESTER')")
    @Operation(summary = "Bulk upload test data via CSV")
    public ApiResponse<List<TestDataResponseDto>> bulkUpload(@PathVariable Long applicationId, @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok("Bulk upload complete", testDataService.bulkUpload(applicationId, file));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TESTER')")
    @Operation(summary = "Update a test data record")
    public ApiResponse<TestDataResponseDto> update(@PathVariable Long id, @Valid @RequestBody TestDataRequestDto request) {
        return ApiResponse.ok("Test data updated", testDataService.update(id, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a test data record by id")
    public ApiResponse<TestDataResponseDto> getById(@PathVariable Long id) {
        return ApiResponse.ok(testDataService.getById(id));
    }

    @GetMapping("/application/{applicationId}")
    @Operation(summary = "List test data records for an application")
    public ApiResponse<PageResponse<TestDataResponseDto>> listByApplication(@PathVariable Long applicationId, Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(testDataService.listByApplication(applicationId, pageable)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TESTER')")
    @Operation(summary = "Delete a test data record")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        testDataService.delete(id);
        return ApiResponse.ok("Test data deleted", null);
    }
}
