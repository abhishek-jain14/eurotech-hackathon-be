package com.qagenie.testbe.report.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record SignoffRequestDto(
        @NotNull @Pattern(regexp = "APPROVE|REJECT") String action,
        String comment
) {}
