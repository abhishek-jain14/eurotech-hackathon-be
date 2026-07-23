package com.qagenie.testbe.report.dto;

public record FailureDetailDto(
        Long resultId, Long scenarioId, String scenarioName, Long testDataId, String testDataRecordName,
        String requestMethod, String requestUrl, Integer responseStatusCode, String errorMessage
) {}
