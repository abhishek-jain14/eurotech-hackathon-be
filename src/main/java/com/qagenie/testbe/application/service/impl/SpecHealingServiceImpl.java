package com.qagenie.testbe.application.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qagenie.testbe.application.dto.EndpointFieldDiffDto;
import com.qagenie.testbe.application.dto.FieldChangeDto;
import com.qagenie.testbe.application.dto.FieldMeta;
import com.qagenie.testbe.application.dto.SpecHealSummaryDto;
import com.qagenie.testbe.application.service.EndpointFieldExtractor;
import com.qagenie.testbe.application.service.SpecHealingService;
import com.qagenie.testbe.scenario.entity.TestScenario;
import com.qagenie.testbe.scenario.repository.TestScenarioRepository;
import com.qagenie.testbe.testdata.entity.TestData;
import com.qagenie.testbe.testdata.entity.TestDataStatus;
import com.qagenie.testbe.testdata.repository.TestDataRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs right before a pending spec version is promoted to CURRENT (see
 * ApplicationServiceImpl#approveSpecVersion), given the field-level diff between the
 * version being superseded and the one being promoted.
 *
 * Only MODIFIED endpoints carry meaningful field-level changes (ADDED/REMOVED endpoints
 * have no prior scenarios to reconcile against). For each MODIFIED endpoint:
 *  - RENAMED: the field's key/fieldName is rewritten in place, value/type untouched.
 *  - DELETED: the field is dropped from every scenario descriptor and test data record.
 *  - ADDED: inserted with a placeholder value (scenario descriptor gets a FieldMeta,
 *    test data gets a type-appropriate empty value) so nothing is silently missing.
 *  - TYPE_CHANGED: can't be safely auto-converted without guessing intent, so any test
 *    data record actually holding that field is flagged INVALID for manual review
 *    instead (never silently downgraded back to VALID by a later heal).
 *
 * "location" (header/path/query/body) picks which TEST_SCENARIO JSON column applies -
 * body fields have no scenario-level descriptor (TEST_SCENARIO doesn't cache request
 * body shape), so only TEST_DATA is touched for those. TEST_DATA's own grouping is
 * coarser (headers/pathParams/requestBody only, per the FE's testDataFields.js) - query
 * and body fields both land in "requestBody" there.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class SpecHealingServiceImpl implements SpecHealingService {

    private static final Logger log = LoggerFactory.getLogger(SpecHealingServiceImpl.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private final TestScenarioRepository testScenarioRepository;
    private final TestDataRepository testDataRepository;
    private final EndpointFieldExtractor fieldExtractor;

    private record FieldOp(String kind, String fieldName, String newFieldName, String fieldType, boolean mandatory) {}

    @Override
    public SpecHealSummaryDto heal(Long applicationId, List<EndpointFieldDiffDto> fieldChanges) {
        int scenariosUpdated = 0, testDataUpdated = 0, fieldsRenamed = 0, fieldsDeleted = 0, fieldsAdded = 0, recordsFlaggedForReview = 0;

        List<TestScenario> allScenarios = testScenarioRepository.findByApplicationId(applicationId);

        for (EndpointFieldDiffDto endpointDiff : fieldChanges) {
            if (!"MODIFIED".equals(endpointDiff.changeType())) {
                continue; // ADDED/REMOVED endpoints have no prior scenarios to reconcile
            }

            Map<String, List<FieldOp>> opsByLocation = buildOps(endpointDiff);
            if (opsByLocation.isEmpty()) {
                continue;
            }
            for (List<FieldOp> ops : opsByLocation.values()) {
                for (FieldOp op : ops) {
                    switch (op.kind()) {
                        case "RENAMED" -> fieldsRenamed++;
                        case "DELETED" -> fieldsDeleted++;
                        case "ADDED" -> fieldsAdded++;
                        default -> { }
                    }
                }
            }

            List<TestScenario> matching = allScenarios.stream()
                    .filter(s -> endpointMatches(endpointDiff.endpoint(), s.getHttpMethod(), s.getEndpoint()))
                    .toList();
            if (matching.isEmpty()) {
                continue;
            }

            List<Long> scenarioIds = new ArrayList<>();
            for (TestScenario scenario : matching) {
                if (healScenarioDescriptors(scenario, opsByLocation)) {
                    testScenarioRepository.save(scenario);
                    scenariosUpdated++;
                }
                scenarioIds.add(scenario.getId());
            }

            List<TestData> records = testDataRepository.findByTestScenario_IdIn(scenarioIds);
            for (TestData record : records) {
                HealResult result = healTestData(record, opsByLocation);
                if (result.changed()) {
                    testDataRepository.save(record);
                    testDataUpdated++;
                    if (result.flaggedForReview()) {
                        recordsFlaggedForReview++;
                    }
                }
            }
        }

        log.info("Spec heal for application id={}: {} scenarios updated, {} test data records updated ({} flagged for review), " +
                        "{} fields renamed, {} deleted, {} added",
                applicationId, scenariosUpdated, testDataUpdated, recordsFlaggedForReview, fieldsRenamed, fieldsDeleted, fieldsAdded);

        return new SpecHealSummaryDto(scenariosUpdated, testDataUpdated, fieldsRenamed, fieldsDeleted, fieldsAdded, recordsFlaggedForReview);
    }

    /**
     * RENAMED/DELETED/TYPE_CHANGED are read from oldFields only, ADDED from newFields only -
     * RENAMED and TYPE_CHANGED both appear on both sides of an EndpointFieldDiffDto for the
     * same field, so reading only one side avoids handling each of those twice.
     */
    private Map<String, List<FieldOp>> buildOps(EndpointFieldDiffDto endpointDiff) {
        Map<String, List<FieldOp>> byLocation = new LinkedHashMap<>();
        for (FieldChangeDto f : endpointDiff.oldFields()) {
            FieldOp op = switch (f.changeType()) {
                case "RENAMED" -> new FieldOp("RENAMED", f.fieldName(), f.relatedFieldName(), f.fieldType(), f.mandatory());
                case "DELETED" -> new FieldOp("DELETED", f.fieldName(), null, f.fieldType(), f.mandatory());
                case "TYPE_CHANGED" -> new FieldOp("TYPE_CHANGED", f.fieldName(), null, f.fieldType(), f.mandatory());
                default -> null;
            };
            if (op != null) {
                byLocation.computeIfAbsent(f.location(), k -> new ArrayList<>()).add(op);
            }
        }
        for (FieldChangeDto f : endpointDiff.newFields()) {
            if ("ADDED".equals(f.changeType())) {
                byLocation.computeIfAbsent(f.location(), k -> new ArrayList<>())
                        .add(new FieldOp("ADDED", f.fieldName(), null, f.fieldType(), f.mandatory()));
            }
        }
        return byLocation;
    }

    private boolean endpointMatches(String diffEndpointKey, String httpMethod, String endpoint) {
        if (httpMethod == null || endpoint == null) return false;
        return diffEndpointKey.equalsIgnoreCase(httpMethod.toUpperCase() + " " + endpoint);
    }

    /** TEST_SCENARIO has no column for "body" fields - only header/path/query descriptors are cached there. */
    private boolean healScenarioDescriptors(TestScenario scenario, Map<String, List<FieldOp>> opsByLocation) {
        boolean changed = false;
        changed |= applyOpsToDescriptor(opsByLocation.get("header"), scenario.getHeaderJson(), scenario::setHeaderJson);
        changed |= applyOpsToDescriptor(opsByLocation.get("path"), scenario.getPathParamJson(), scenario::setPathParamJson);
        changed |= applyOpsToDescriptor(opsByLocation.get("query"), scenario.getRequestParamJson(), scenario::setRequestParamJson);
        return changed;
    }

    private boolean applyOpsToDescriptor(List<FieldOp> ops, String currentJson, java.util.function.Consumer<String> setter) {
        if (ops == null || ops.isEmpty()) {
            return false;
        }
        List<FieldMeta> fields = new ArrayList<>(fieldExtractor.fromJson(currentJson));
        boolean changed = false;
        for (FieldOp op : ops) {
            switch (op.kind()) {
                case "RENAMED" -> {
                    for (int i = 0; i < fields.size(); i++) {
                        if (fields.get(i).fieldName().equals(op.fieldName())) {
                            fields.set(i, new FieldMeta(op.newFieldName(), fields.get(i).type(), fields.get(i).mandatory()));
                            changed = true;
                            break;
                        }
                    }
                }
                case "DELETED" -> changed |= fields.removeIf(f -> f.fieldName().equals(op.fieldName()));
                case "TYPE_CHANGED" -> {
                    if (op.fieldType() != null) {
                        for (int i = 0; i < fields.size(); i++) {
                            if (fields.get(i).fieldName().equals(op.fieldName())) {
                                fields.set(i, new FieldMeta(op.fieldName(), op.fieldType(), fields.get(i).mandatory()));
                                changed = true;
                                break;
                            }
                        }
                    }
                }
                case "ADDED" -> {
                    if (fields.stream().noneMatch(f -> f.fieldName().equals(op.fieldName()))) {
                        fields.add(new FieldMeta(op.fieldName(), op.fieldType(), op.mandatory()));
                        changed = true;
                    }
                }
                default -> { }
            }
        }
        if (changed) {
            setter.accept(fieldExtractor.toJson(fields));
        }
        return changed;
    }

    private record HealResult(boolean changed, boolean flaggedForReview) {}

    private HealResult healTestData(TestData record, Map<String, List<FieldOp>> opsByLocation) {
        Map<String, Object> root = parseFieldsJson(record.getFieldsJson());
        if (root == null) {
            return new HealResult(false, false); // malformed JSON - leave the record untouched rather than risk data loss
        }
        boolean changed = false;
        boolean flaggedForReview = false;

        for (Map.Entry<String, List<FieldOp>> entry : opsByLocation.entrySet()) {
            String group = testDataGroupFor(entry.getKey());
            @SuppressWarnings("unchecked")
            Map<String, Object> groupMap = (Map<String, Object>) root.computeIfAbsent(group, k -> new LinkedHashMap<String, Object>());

            for (FieldOp op : entry.getValue()) {
                switch (op.kind()) {
                    case "RENAMED" -> {
                        if (groupMap.containsKey(op.fieldName())) {
                            Object value = groupMap.remove(op.fieldName());
                            groupMap.put(op.newFieldName(), value);
                            changed = true;
                        }
                    }
                    case "DELETED" -> {
                        if (groupMap.remove(op.fieldName()) != null) {
                            changed = true;
                        }
                    }
                    case "TYPE_CHANGED" -> {
                        if (groupMap.containsKey(op.fieldName())) {
                            flaggedForReview = true;
                        }
                    }
                    case "ADDED" -> {
                        if (!groupMap.containsKey(op.fieldName())) {
                            groupMap.put(op.fieldName(), placeholderFor(op.fieldType()));
                            changed = true;
                        }
                    }
                    default -> { }
                }
            }
        }

        if (changed) {
            record.setFieldsJson(writeFieldsJson(root));
        }
        if (flaggedForReview && record.getStatus() != TestDataStatus.INVALID) {
            record.setStatus(TestDataStatus.INVALID);
            changed = true;
        }
        return new HealResult(changed, flaggedForReview);
    }

    /** TEST_DATA only groups by headers/pathParams/requestBody - query and body fields share "requestBody". */
    private String testDataGroupFor(String location) {
        return switch (location) {
            case "header" -> "headers";
            case "path" -> "pathParams";
            default -> "requestBody";
        };
    }

    private Object placeholderFor(String fieldType) {
        if (fieldType == null) return "";
        return switch (fieldType.toLowerCase()) {
            case "integer", "number" -> 0;
            case "boolean" -> false;
            case "array" -> List.of();
            case "object" -> Map.of();
            default -> "";
        };
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseFieldsJson(String fieldsJson) {
        if (fieldsJson == null || fieldsJson.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, Object> parsed = JSON.readValue(fieldsJson, Map.class);
            return parsed != null ? new LinkedHashMap<>(parsed) : new LinkedHashMap<>();
        } catch (Exception e) {
            log.warn("Unable to parse TEST_DATA.FIELDS_JSON as an object during healing - leaving it untouched: {}", e.getMessage());
            return null;
        }
    }

    private String writeFieldsJson(Map<String, Object> root) {
        try {
            return JSON.writeValueAsString(root);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to serialize healed TEST_DATA.FIELDS_JSON", e);
        }
    }
}