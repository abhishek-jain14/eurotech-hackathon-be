package com.qagenie.testbe.report.dto;

import java.time.Instant;

public record ReportSignoffDto(
        Long applicationId, String applicationName, String status, String comment,
        String signedOffBy, Instant signedOffAt, Long signedOffRunId, Long latestRunId, boolean stale
) {}
