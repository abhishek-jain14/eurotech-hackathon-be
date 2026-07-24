package com.qagenie.testbe.application.dto;

/**
 * One field-level change within an endpoint's request/response schema.
 * For RENAMED, relatedFieldName carries the field's name on the other side
 * (the new name when this entry sits in the "old" section, the old name
 * when it sits in the "new" section); null for ADDED/DELETED/TYPE_CHANGED.
 *
 * location/fieldType/mandatory are best-effort, sourced directly from the
 * OpenAPI parameter/schema node this field was found on - location is
 * "header" | "path" | "query" | "body" (parameters use their declared
 * "in", schema properties default to "body"), matching the same location
 * vocabulary EndpointFieldExtractor already uses for TEST_SCENARIO's
 * header/path/query JSON columns. fieldType/mandatory drive spec-healing
 * (renamed/added fields get the right type; TYPE_CHANGED can show the
 * actual old-type -> new-type transition instead of just the field name).
 */
public record FieldChangeDto(
        String fieldName,
        String fieldPath,
        String changeType, // ADDED | DELETED | RENAMED | TYPE_CHANGED
        String relatedFieldName,
        String location, // header | path | query | body
        String fieldType,
        boolean mandatory
) {}