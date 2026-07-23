package com.qagenie.testbe.application.diff;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.qagenie.testbe.application.dto.EndpointFieldDiffDto;
import com.qagenie.testbe.application.dto.FieldChangeDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * Best-effort structural diff between two OpenAPI/Swagger documents (JSON
 * or YAML), at "HTTP method + path" granularity. This intentionally does
 * NOT attempt a full semantic OpenAPI diff (parameter-level, schema-level
 * changes) - it flags a path+method as MODIFIED if its subtree changed at
 * all, without describing exactly what inside it changed. That's enough
 * to drive "12 scenarios reference /charge, which changed - review before
 * approving" without pulling in a heavyweight OpenAPI-diff library.
 *
 * Non-OpenAPI content (e.g. a raw DOM snapshot for frontend onboarding)
 * that has no top-level "paths" object falls back to a single whole-
 * document MODIFIED/ADDED/REMOVED entry.
 */
@Service
public class SpecDiffService {

    private static final Logger log = LoggerFactory.getLogger(SpecDiffService.class);
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    public List<SpecDiffEntry> diff(String oldContent, String newContent) {
        JsonNode oldTree = parseLeniently(oldContent);
        JsonNode newTree = parseLeniently(newContent);

        if (oldTree == null || newTree == null || !oldTree.has("paths") || !newTree.has("paths")) {
            return diffAsWholeDocument(oldContent, newContent);
        }
        return diffPaths(oldTree.get("paths"), newTree.get("paths"));
    }

    private List<SpecDiffEntry> diffPaths(JsonNode oldPaths, JsonNode newPaths) {
        List<SpecDiffEntry> entries = new ArrayList<>();

        Map<String, JsonNode> oldFlat = flattenPathMethods(oldPaths);
        Map<String, JsonNode> newFlat = flattenPathMethods(newPaths);

        for (String key : newFlat.keySet()) {
            if (!oldFlat.containsKey(key)) {
                entries.add(new SpecDiffEntry("ADDED", key, "New endpoint added to the specification"));
            } else if (!oldFlat.get(key).equals(newFlat.get(key))) {
                entries.add(new SpecDiffEntry("MODIFIED", key, "Endpoint definition changed (parameters, schema, or response shape)"));
            }
        }
        for (String key : oldFlat.keySet()) {
            if (!newFlat.containsKey(key)) {
                entries.add(new SpecDiffEntry("REMOVED", key, "Endpoint no longer present in the specification"));
            }
        }

        log.info("Spec diff: {} added/removed/modified endpoint entries found", entries.size());
        return entries;
    }

    /** Flattens {"/payments/{id}": {"get": {...}, "post": {...}}} into {"GET /payments/{id}": <node>, ...} */
    private Map<String, JsonNode> flattenPathMethods(JsonNode pathsNode) {
        Map<String, JsonNode> flat = new TreeMap<>();
        Iterator<Map.Entry<String, JsonNode>> pathEntries = pathsNode.fields();
        while (pathEntries.hasNext()) {
            Map.Entry<String, JsonNode> pathEntry = pathEntries.next();
            String path = pathEntry.getKey();
            JsonNode methodsNode = pathEntry.getValue();
            Iterator<Map.Entry<String, JsonNode>> methodEntries = methodsNode.fields();
            while (methodEntries.hasNext()) {
                Map.Entry<String, JsonNode> methodEntry = methodEntries.next();
                String method = methodEntry.getKey().toUpperCase();
                flat.put(method + " " + path, methodEntry.getValue());
            }
        }
        return flat;
    }

    /**
     * Field-level breakdown of every changed endpoint, segregated into old
     * (deleted fields + old names of renamed fields) and new (added fields
     * + new names of renamed fields) sections. Best-effort: it flattens any
     * "properties" object and named "parameters" entries it finds anywhere
     * under an endpoint's subtree into dotted field paths (e.g.
     * "requestBody.customer.email"), so it does not resolve "$ref" schema
     * references. Rename detection is a heuristic - a deleted field and an
     * added field under the same parent path with the same type/format are
     * treated as a rename rather than an unrelated delete+add.
     */
    public List<EndpointFieldDiffDto> diffFields(String oldContent, String newContent) {
        JsonNode oldTree = parseLeniently(oldContent);
        JsonNode newTree = parseLeniently(newContent);
        if (oldTree == null || newTree == null || !oldTree.has("paths") || !newTree.has("paths")) {
            return List.of();
        }

        Map<String, JsonNode> oldFlat = flattenPathMethods(oldTree.get("paths"));
        Map<String, JsonNode> newFlat = flattenPathMethods(newTree.get("paths"));

        List<EndpointFieldDiffDto> result = new ArrayList<>();
        for (String key : newFlat.keySet()) {
            if (!oldFlat.containsKey(key)) {
                result.add(endpointFieldDiff(key, "ADDED", null, newFlat.get(key)));
            } else if (!oldFlat.get(key).equals(newFlat.get(key))) {
                result.add(endpointFieldDiff(key, "MODIFIED", oldFlat.get(key), newFlat.get(key)));
            }
        }
        for (String key : oldFlat.keySet()) {
            if (!newFlat.containsKey(key)) {
                result.add(endpointFieldDiff(key, "REMOVED", oldFlat.get(key), null));
            }
        }
        return result;
    }

    private EndpointFieldDiffDto endpointFieldDiff(String endpoint, String changeType, JsonNode oldNode, JsonNode newNode) {
        Map<String, JsonNode> oldFields = oldNode != null ? extractFields(oldNode) : Map.of();
        Map<String, JsonNode> newFields = newNode != null ? extractFields(newNode) : Map.of();

        Set<String> deleted = new LinkedHashSet<>(oldFields.keySet());
        deleted.removeAll(newFields.keySet());
        Set<String> added = new LinkedHashSet<>(newFields.keySet());
        added.removeAll(oldFields.keySet());

        List<FieldChangeDto> oldSection = new ArrayList<>();
        List<FieldChangeDto> newSection = new ArrayList<>();

        Set<String> matchedDeleted = new LinkedHashSet<>();
        Set<String> matchedAdded = new LinkedHashSet<>();
        for (String deletedPath : deleted) {
            String parent = parentPath(deletedPath);
            for (String addedPath : added) {
                if (matchedAdded.contains(addedPath) || !parent.equals(parentPath(addedPath))) {
                    continue;
                }
                if (schemasEquivalent(oldFields.get(deletedPath), newFields.get(addedPath))) {
                    matchedDeleted.add(deletedPath);
                    matchedAdded.add(addedPath);
                    String oldName = leafName(deletedPath);
                    String newName = leafName(addedPath);
                    oldSection.add(new FieldChangeDto(oldName, deletedPath, "RENAMED", newName));
                    newSection.add(new FieldChangeDto(newName, addedPath, "RENAMED", oldName));
                    break;
                }
            }
        }

        for (String deletedPath : deleted) {
            if (!matchedDeleted.contains(deletedPath)) {
                oldSection.add(new FieldChangeDto(leafName(deletedPath), deletedPath, "DELETED", null));
            }
        }
        for (String addedPath : added) {
            if (!matchedAdded.contains(addedPath)) {
                newSection.add(new FieldChangeDto(leafName(addedPath), addedPath, "ADDED", null));
            }
        }

        Set<String> common = new LinkedHashSet<>(oldFields.keySet());
        common.retainAll(newFields.keySet());
        for (String path : common) {
            if (!schemasEquivalent(oldFields.get(path), newFields.get(path))) {
                oldSection.add(new FieldChangeDto(leafName(path), path, "TYPE_CHANGED", null));
                newSection.add(new FieldChangeDto(leafName(path), path, "TYPE_CHANGED", null));
            }
        }

        return new EndpointFieldDiffDto(endpoint, changeType, oldSection, newSection);
    }

    /** Flattens every "properties" object and named "parameters" entry under an endpoint into dotted field paths. */
    private Map<String, JsonNode> extractFields(JsonNode endpointNode) {
        Map<String, JsonNode> fields = new LinkedHashMap<>();
        collectFields(endpointNode, "", fields);
        return fields;
    }

    private void collectFields(JsonNode node, String path, Map<String, JsonNode> out) {
        if (node == null) {
            return;
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                collectFields(item, path, out);
            }
            return;
        }
        if (!node.isObject()) {
            return;
        }

        // e.g. {"name": "id", "in": "path", "schema": {...}} - a parameter array element
        if (node.has("name") && node.get("name").isTextual() && (node.has("in") || node.has("schema"))) {
            String fieldPath = path.isEmpty() ? node.get("name").asText() : path + "." + node.get("name").asText();
            out.put(fieldPath, node);
        }

        if (node.has("properties") && node.get("properties").isObject()) {
            Iterator<Map.Entry<String, JsonNode>> propEntries = node.get("properties").fields();
            while (propEntries.hasNext()) {
                Map.Entry<String, JsonNode> propEntry = propEntries.next();
                String fieldPath = path.isEmpty() ? propEntry.getKey() : path + "." + propEntry.getKey();
                out.put(fieldPath, propEntry.getValue());
                collectFields(propEntry.getValue(), fieldPath, out);
            }
        }

        Iterator<Map.Entry<String, JsonNode>> entries = node.fields();
        while (entries.hasNext()) {
            Map.Entry<String, JsonNode> entry = entries.next();
            if (entry.getKey().equals("properties")) {
                continue;
            }
            String childPath = entry.getKey().equals("items") ? (path.isEmpty() ? "[]" : path + "[]") : path;
            collectFields(entry.getValue(), childPath, out);
        }
    }

    private String parentPath(String path) {
        int idx = path.lastIndexOf('.');
        return idx < 0 ? "" : path.substring(0, idx);
    }

    private String leafName(String path) {
        int idx = path.lastIndexOf('.');
        return idx < 0 ? path : path.substring(idx + 1);
    }

    private boolean schemasEquivalent(JsonNode a, JsonNode b) {
        if (a == null || b == null) {
            return false;
        }
        String typeA = a.path("type").asText(null);
        String typeB = b.path("type").asText(null);
        String formatA = a.path("format").asText(null);
        String formatB = b.path("format").asText(null);
        return Objects.equals(typeA, typeB) && Objects.equals(formatA, formatB);
    }

    private List<SpecDiffEntry> diffAsWholeDocument(String oldContent, String newContent) {
        List<SpecDiffEntry> entries = new ArrayList<>();
        if (oldContent == null || oldContent.isBlank()) {
            entries.add(new SpecDiffEntry("ADDED", "(whole document)", "Initial content"));
        } else if (!oldContent.equals(newContent)) {
            entries.add(new SpecDiffEntry("MODIFIED", "(whole document)",
                    "Content changed. This document has no recognizable OpenAPI 'paths' section " +
                    "(e.g. a DOM snapshot), so only a whole-document change is reported rather than " +
                    "a per-endpoint breakdown."));
        }
        return entries;
    }

    private JsonNode parseLeniently(String content) {
        if (content == null || content.isBlank()) return null;
        try {
            return JSON_MAPPER.readTree(content);
        } catch (Exception jsonEx) {
            try {
                return YAML_MAPPER.readTree(content);
            } catch (Exception yamlEx) {
                log.warn("Spec content is neither valid JSON nor YAML - falling back to whole-document diff");
                return null;
            }
        }
    }
}
