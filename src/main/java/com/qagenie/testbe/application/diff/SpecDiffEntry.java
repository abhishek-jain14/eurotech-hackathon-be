package com.qagenie.testbe.application.diff;

/**
 * One detected difference between two spec versions, at endpoint+method
 * granularity. Mirrors changetracker.entity.ChangeType (ADDED/REMOVED/
 * MODIFIED) but lives here since the diff engine is spec-format-specific
 * and shouldn't depend on the changetracker module.
 */
public record SpecDiffEntry(
        String changeType,   // ADDED | REMOVED | MODIFIED
        String endpoint,     // e.g. "GET /payments/{id}"
        String description
) {}
