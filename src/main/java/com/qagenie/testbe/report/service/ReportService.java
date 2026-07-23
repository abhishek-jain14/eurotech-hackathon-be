package com.qagenie.testbe.report.service;

import com.qagenie.testbe.report.dto.ReportDetailDto;
import com.qagenie.testbe.report.dto.ReportSignoffDto;
import com.qagenie.testbe.report.dto.ReportSummaryDto;
import com.qagenie.testbe.report.dto.SignoffRequestDto;

import java.util.List;

public interface ReportService {
    ReportSummaryDto getSummaryForApplication(Long applicationId);

    /** Latest-run-focused view: stats, pass/fail, comparison vs previous run, flakiness, failure detail. */
    ReportDetailDto getDetailForApplication(Long applicationId);

    /** Sign-off state for every application (the release sign-off table). */
    List<ReportSignoffDto> listSignoffs();

    ReportSignoffDto signOff(Long applicationId, SignoffRequestDto request, String signedOffBy);
}
