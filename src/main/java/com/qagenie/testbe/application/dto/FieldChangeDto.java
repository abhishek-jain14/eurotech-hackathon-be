package com.qagenie.testbe.application.dto;

/**
 * One field-level change within an endpoint's request/response schema.
 * For RENAMED, relatedFieldName carries the field's name on the other side
 * (the new name when this entry sits in the "old" section, the old name
 * when it sits in the "new" section); null for ADDED/DELETED/TYPE_CHANGED.
 */
public record FieldChangeDto(
        String fieldName,
        String fieldPath,
        String changeType, // ADDED | DELETED | RENAMED | TYPE_CHANGED
        String relatedFieldName
) {}
