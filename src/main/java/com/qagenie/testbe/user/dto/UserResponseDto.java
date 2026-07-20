package com.qagenie.testbe.user.dto;

import com.qagenie.testbe.security.Role;
import java.time.Instant;

public record UserResponseDto(
        Long id,
        String username,
        String fullName,
        String email,
        Role role,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {}
