package com.qagenie.testbe.user.controller;

import com.qagenie.testbe.common.response.ApiResponse;
import com.qagenie.testbe.common.response.PageResponse;
import com.qagenie.testbe.user.dto.UserRequestDto;
import com.qagenie.testbe.user.dto.UserResponseDto;
import com.qagenie.testbe.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * User management - ADMIN only. Tester and Viewer roles never see this module.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Admin-only CRUD for platform users and role assignment")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    @PostMapping
    @Operation(summary = "Create a new platform user with an assigned role")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponseDto> create(@Valid @RequestBody UserRequestDto request) {
        return ApiResponse.ok("User created", userService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing user's profile or role")
    public ApiResponse<UserResponseDto> update(@PathVariable Long id, @Valid @RequestBody UserRequestDto request) {
        return ApiResponse.ok("User updated", userService.update(id, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a user by id")
    public ApiResponse<UserResponseDto> getById(@PathVariable Long id) {
        return ApiResponse.ok(userService.getById(id));
    }

    @GetMapping
    @Operation(summary = "List users with pagination")
    public ApiResponse<PageResponse<UserResponseDto>> list(Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(userService.list(pageable)));
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate a user without deleting their history")
    public ApiResponse<Void> deactivate(@PathVariable Long id) {
        userService.deactivate(id);
        return ApiResponse.ok("User deactivated", null);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Permanently delete a user")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ApiResponse.ok("User deleted", null);
    }
}
