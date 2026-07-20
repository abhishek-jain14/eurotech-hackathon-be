package com.qagenie.testbe.application.diff;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
