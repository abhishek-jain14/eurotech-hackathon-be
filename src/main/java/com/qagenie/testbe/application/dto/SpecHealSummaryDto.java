package com.qagenie.testbe.application.dto;

/**
 * What SpecHealingService actually did while promoting a spec version - RENAMED and
 * DELETED fields are rewritten in place (safe, mechanical), ADDED fields are inserted
 * with a placeholder value, and TYPE_CHANGED fields can't be safely auto-converted so
 * the TestData record carrying them is flagged INVALID for manual review instead.
 */
public record SpecHealSummaryDto(
        int scenariosUpdated,
        int testDataUpdated,
        int fieldsRenamed,
        int fieldsDeleted,
        int fieldsAdded,
        int recordsFlaggedForReview
) {
    public static SpecHealSummaryDto empty() {
        return new SpecHealSummaryDto(0, 0, 0, 0, 0, 0);
    }
}