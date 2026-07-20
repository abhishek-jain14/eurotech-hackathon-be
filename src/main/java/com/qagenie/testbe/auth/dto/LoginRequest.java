package com.qagenie.testbe.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Login credentials")
public record LoginRequest(

        @NotBlank
        @Schema(example = "admin", description = "Dummy platform login accepts username 'admin'")
        String username,

        @NotBlank
        @Schema(example = "test", description = "Dummy platform login accepts password 'test'")
        String password
) {}
