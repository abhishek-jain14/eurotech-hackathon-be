package com.qagenie.testbe.application.dto;

import java.util.List;

public record SpecVersionImpactDto(
        Long specVersionId,
        List<SpecDiffEntryDto> changes,
        List<EndpointFieldDiffDto> fieldChanges,
        int affectedScenarioCount,
        List<AffectedScenarioDto> affectedScenarios
) {
    public record AffectedScenarioDto(Long scenarioId, String scenarioName, String endpoint) {}
}
