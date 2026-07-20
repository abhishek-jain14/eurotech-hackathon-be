package com.qagenie.testbe.environment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Payload to add or update an environment configuration under an onboarded project")
public record EnvironmentRequestDto(

        @NotNull
        @Schema(example = "1")
        Long projectId,

        @NotBlank
        @Schema(example = "Staging")
        String envName,

        @NotBlank
        @Schema(example = "SwaggerUrl", description = "SwaggerUrl or Database")
        String configType,

        @NotBlank
        @Schema(example = "https://staging-api.payment.com")
        String baseUrl,

        @Schema(example = "true")
        Boolean active
) {}
