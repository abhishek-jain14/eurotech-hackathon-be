package com.qagenie.testbe.user.dto;

import com.qagenie.testbe.security.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Payload for creating or updating a platform user")
public record UserRequestDto(

        @NotBlank @Size(min = 3, max = 100)
        @Schema(example = "jane.doe")
        String username,

        @Size(min = 6, max = 100)
        @Schema(example = "StrongPassword123", description = "Required on create, optional on update")
        String password,

        @NotBlank @Size(max = 150)
        @Schema(example = "Jane Doe")
        String fullName,

        @Email @Size(max = 150)
        @Schema(example = "jane.doe@example.com")
        String email,

        @NotNull
        @Schema(example = "TESTER")
        Role role,

        @Schema(example = "true")
        Boolean active
) {}
