package com.qagenie.testbe.testdata.dto;

import com.qagenie.testbe.testdata.entity.TestDataMode;
import com.qagenie.testbe.testdata.entity.TestDataStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TestDataRequestDto(
        @NotNull Long applicationId,
        @NotBlank String recordName,
        @NotNull TestDataMode mode,
        TestDataStatus status,
        @NotBlank String fieldsJson
) {}
