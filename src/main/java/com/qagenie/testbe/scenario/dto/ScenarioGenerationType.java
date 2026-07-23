package com.qagenie.testbe.scenario.dto;

/**
 * Which scenario type(s) to synthesize for a "Generate Scenarios" request.
 * Broader than the persisted {@link com.qagenie.testbe.scenario.entity.ScenarioType}
 * (POSITIVE/NEGATIVE only) since a single request can ask for both.
 */
public enum ScenarioGenerationType { POSITIVE, NEGATIVE, POSITIVE_NEGATIVE }
