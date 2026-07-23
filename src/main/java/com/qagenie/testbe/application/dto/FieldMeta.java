package com.qagenie.testbe.application.dto;

/** One parameter/field's metadata, cached as JSON on SPEC_ENDPOINT and TEST_SCENARIO. */
public record FieldMeta(String fieldName, String type, boolean mandatory) {}
