package com.qagenie.testbe.application.dto;

import java.util.List;

/**
 * Field-level breakdown of a single endpoint's change, segregated into an
 * "old" and "new" section so the UI can render them side by side:
 *  - oldFields: fields deleted (no longer present in the new spec) plus the
 *    old name of any field that looks renamed.
 *  - newFields: fields newly added plus the new name of any field that
 *    looks renamed.
 */
public record EndpointFieldDiffDto(
        String endpoint,
        String changeType, // ADDED | REMOVED | MODIFIED
        List<FieldChangeDto> oldFields,
        List<FieldChangeDto> newFields
) {}
