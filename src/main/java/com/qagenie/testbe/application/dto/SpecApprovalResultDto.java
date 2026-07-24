package com.qagenie.testbe.application.dto;

/** Result of approving/healing a pending spec version: the promoted application plus what auto-heal touched. */
public record SpecApprovalResultDto(
        ApplicationResponseDto application,
        SpecHealSummaryDto healSummary
) {}