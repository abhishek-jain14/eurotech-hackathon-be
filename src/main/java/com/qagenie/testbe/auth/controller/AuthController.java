package com.qagenie.testbe.auth.controller;

import com.qagenie.testbe.auth.dto.LoginRequest;
import com.qagenie.testbe.auth.dto.LoginResponse;
import com.qagenie.testbe.auth.service.AuthService;
import com.qagenie.testbe.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login endpoint. Dummy credentials username=admin, password=test are always accepted as ADMIN.")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Authenticate and receive a JWT", description = "Returns a bearer token plus the resolved role (ADMIN / TESTER / VIEWER).")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok("Login successful", authService.login(request));
    }
}
