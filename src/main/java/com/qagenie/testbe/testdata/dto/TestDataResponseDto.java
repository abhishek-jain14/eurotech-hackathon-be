package com.qagenie.testbe.testdata.dto;

import com.qagenie.testbe.testdata.entity.TestDataMode;
import com.qagenie.testbe.testdata.entity.TestDataStatus;
import java.time.Instant;

public record TestDataResponseDto(
        Long id, Long applicationId, Long scenarioId, String scenarioName, String recordName, TestDataMode mode,
        TestDataStatus status, String fieldsJson, Instant createdAt, Instant updatedAt,
        String serviceName, String endPoint, String environment, Integer httpStatusCode,
        String errorCode, String errorMsg, String responseFields, String responseJson
) {}
