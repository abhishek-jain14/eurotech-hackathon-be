package com.qagenie.testbe.envvariable.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Payload to create or update a Project environment variable (Manage popup on the Project list)")
public record EnvironmentVariableRequestDto(

        @NotNull
        @Schema(example = "1")
        Long projectId,

        @NotBlank
        @Schema(example = "API_BASE_URL")
        String name,

        @Schema(example = "https://api.example.com")
        String value
) {}