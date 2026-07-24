package com.qagenie.testbe.testdata.dto;

import com.qagenie.testbe.testdata.entity.TestDataMode;
import com.qagenie.testbe.testdata.entity.TestDataStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record TestDataRequestDto(
        @NotNull Long applicationId,
        @NotNull Long scenarioId,
        @NotBlank String recordName,
        @NotNull TestDataMode mode,
        TestDataStatus status,
        String fieldsJson,
        Map<String,Object> placeHolders,
        String serviceName,
        String endPoint,
        String environment,
        @Min(100) @Max(599) Integer httpStatusCode,
        String errorCode,
        String errorMsg,
        String responseFields,
        String responseJson
) {}
