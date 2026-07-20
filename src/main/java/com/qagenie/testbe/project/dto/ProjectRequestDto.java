package com.qagenie.testbe.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Payload to create or update a Project - the grouping that owns shared keystore/truststore and Environments")
public record ProjectRequestDto(

        @NotBlank
        @Schema(example = "Payments Platform")
        String name,

        @Schema(example = "All payment-related services sharing the internal gateway cert")
        String description,

        @Schema(example = "https://mycompany.atlassian.net/browse/PROJ")
        String jiraUrl,

        @Schema(example = "/v3/api-docs", description = "Optional fixed suffix appended after the application name when deriving a spec URL")
        String specPathSuffix
) {}
