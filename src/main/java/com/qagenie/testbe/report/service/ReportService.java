package com.qagenie.testbe.report.service;

import com.qagenie.testbe.report.dto.ReportSummaryDto;

public interface ReportService {
    ReportSummaryDto getSummaryForApplication(Long applicationId);
}
