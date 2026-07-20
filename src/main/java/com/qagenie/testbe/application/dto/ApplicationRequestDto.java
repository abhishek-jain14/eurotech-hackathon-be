package com.qagenie.testbe.application.dto;

import com.qagenie.testbe.application.entity.ApplicationType;
import com.qagenie.testbe.application.entity.SpecFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Payload to onboard a new application. Always belongs to a Project (for its shared " +
        "keystore/truststore) and optionally references one of that Project's Environments to derive the spec URL.")
public record ApplicationRequestDto(

        @NotNull
        @Schema(example = "1", description = "The Project this application belongs to")
        Long projectId,

        @NotBlank
        @Schema(example = "PaymentAPI")
        String name,

        @Schema(example = "Core payments service exposing charge/refund endpoints")
        String description,

        @NotNull
        @Schema(example = "BACKEND")
        ApplicationType applicationType,

        @Schema(example = "3", description = "Required when specSourceMode=DERIVED: which of the Project's " +
                "Environments to derive the base URL from")
        Long referenceEnvironmentId

) {}
