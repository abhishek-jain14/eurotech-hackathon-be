package com.qagenie.testbe.testdata.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qagenie.testbe.common.exception.BusinessException;
import com.qagenie.testbe.common.exception.ResourceNotFoundException;
import com.qagenie.testbe.application.entity.Application;
import com.qagenie.testbe.application.repository.ApplicationRepository;
import com.qagenie.testbe.scenario.entity.TestScenario;
import com.qagenie.testbe.scenario.repository.TestScenarioRepository;
import com.qagenie.testbe.testdata.dto.TestDataRequestDto;
import com.qagenie.testbe.testdata.dto.TestDataResponseDto;
import com.qagenie.testbe.testdata.entity.TestData;
import com.qagenie.testbe.testdata.entity.TestDataMode;
import com.qagenie.testbe.testdata.entity.TestDataStatus;
import com.qagenie.testbe.testdata.mapper.TestDataMapper;
import com.qagenie.testbe.testdata.repository.TestDataRepository;
import com.qagenie.testbe.testdata.service.TestDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class TestDataServiceImpl implements TestDataService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** CSV header name (lowercased) -> canonical TestData field name, for columns that bulk upload
     * should set directly on the entity rather than folding into the generic fieldsJson blob. */
    private static final Map<String, String> KNOWN_COLUMNS = Map.ofEntries(
            Map.entry("servicename", "serviceName"),
            Map.entry("endpoint", "endPoint"),
            Map.entry("environment", "environment"),
            Map.entry("httpstatuscode", "httpStatusCode"),
            Map.entry("errorcode", "errorCode"),
            Map.entry("errormsg", "errorMsg"),
            Map.entry("responsefields", "responseFields"),
            Map.entry("responsejson", "responseJson")
    );

    private final TestDataRepository testDataRepository;
    private final ApplicationRepository applicationRepository;
    private final TestDataMapper testDataMapper;
    private final TestScenarioRepository testScenarioRepository;

    @Override
    public TestDataResponseDto create(TestDataRequestDto request) {
        Application application = findApplication(request.applicationId());
        TestScenario testScenario = findTestScenario(request.scenarioId());
        requireScenarioBelongsToApplication(testScenario, application);

        TestData entity = testDataMapper.toEntity(request);
        entity.setApplication(application);
        entity.setTestScenario(testScenario);
        if (entity.getStatus() == null) {
            entity.setStatus(TestDataStatus.VALID);
        }
        return testDataMapper.toResponseDto(testDataRepository.save(entity));
    }

    @Override
    public List<TestDataResponseDto> bulkUpload(Long applicationId, Long scenarioId, MultipartFile file) {
        Application application = findApplication(applicationId);
        TestScenario testScenario = findTestScenario(scenarioId);
        requireScenarioBelongsToApplication(testScenario, application);
        List<TestData> records = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String header = reader.readLine();
            if (header == null) {
                throw new BusinessException("Uploaded file is empty", "EMPTY_FILE");
            }
            String[] columns = header.split(",");
            String line;
            int rowNum = 1;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] values = line.split(",", -1);

                TestData entity = new TestData();
                entity.setApplication(application);
                entity.setTestScenario(testScenario);
                entity.setRecordName("record_" + String.format("%03d", rowNum));
                entity.setMode(TestDataMode.BULK_UPLOAD);
                entity.setStatus(TestDataStatus.VALID);

                Map<String, String> jsonFields = new LinkedHashMap<>();
                for (int i = 0; i < columns.length && i < values.length; i++) {
                    String columnName = columns[i].trim();
                    String value = values[i].trim();
                    String canonicalField = KNOWN_COLUMNS.get(columnName.toLowerCase());
                    if (canonicalField != null) {
                        applyKnownColumn(entity, canonicalField, value, rowNum);
                    } else {
                        jsonFields.put(columnName, value);
                    }
                }
                entity.setFieldsJson(toFieldsJson(jsonFields));
                records.add(entity);
                rowNum++;
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("Unable to parse bulk upload file: " + e.getMessage(), "BULK_PARSE_ERROR");
        }
        return testDataRepository.saveAll(records).stream().map(testDataMapper::toResponseDto).toList();
    }

    @Override
    public TestDataResponseDto update(Long id, TestDataRequestDto request) {
        TestData entity = findEntity(id);
        testDataMapper.updateEntityFromDto(request, entity);
        return testDataMapper.toResponseDto(testDataRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public TestDataResponseDto getById(Long id) {
        return testDataMapper.toResponseDto(findEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TestDataResponseDto> listByApplication(Long applicationId, Pageable pageable) {
        return testDataRepository.findByApplicationId(applicationId, pageable).map(testDataMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestDataResponseDto> listByScenario(Long scenarioId) {
        return testDataRepository.findByTestScenarioId(scenarioId).stream().map(testDataMapper::toResponseDto).toList();
    }

    @Override
    public void delete(Long id) {
        if (!testDataRepository.existsById(id)) {
            throw ResourceNotFoundException.of("TestData", id);
        }
        testDataRepository.deleteById(id);
    }

    private TestData findEntity(Long id) {
        return testDataRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("TestData", id));
    }

    private Application findApplication(Long id) {
        return applicationRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Application", id));
    }

    private TestScenario findTestScenario(Long id) {
        return testScenarioRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("TestScenario", id));
    }

    private void requireScenarioBelongsToApplication(TestScenario testScenario, Application application) {
        if (!testScenario.getApplication().getId().equals(application.getId())) {
            throw new BusinessException("Scenario does not belong to this application", "SCENARIO_APPLICATION_MISMATCH");
        }
    }

    private void applyKnownColumn(TestData entity, String canonicalField, String rawValue, int rowNum) {
        String value = rawValue.isBlank() ? null : rawValue;
        switch (canonicalField) {
            case "serviceName" -> entity.setServiceName(value);
            case "endPoint" -> entity.setEndPoint(value);
            case "environment" -> entity.setEnvironment(value);
            case "errorCode" -> entity.setErrorCode(value);
            case "errorMsg" -> entity.setErrorMsg(value);
            case "responseFields" -> entity.setResponseFields(validateJson(value, "responseFields", rowNum));
            case "responseJson" -> entity.setResponseJson(validateJson(value, "responseJson", rowNum));
            case "httpStatusCode" -> entity.setHttpStatusCode(parseHttpStatusCode(value, rowNum));
        }
    }

    private String validateJson(String value, String fieldName, int rowNum) {
        if (value == null) return null;
        try {
            MAPPER.readTree(value);
            return value;
        } catch (Exception e) {
            throw new BusinessException("Row " + rowNum + ": '" + fieldName + "' is not valid JSON", "BULK_INVALID_JSON");
        }
    }

    private Integer parseHttpStatusCode(String value, int rowNum) {
        if (value == null) return null;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new BusinessException("Row " + rowNum + ": 'httpStatusCode' must be a valid integer", "BULK_INVALID_HTTP_STATUS_CODE");
        }
    }

    /** Preserves the pre-existing naive (unescaped) CSV-to-JSON string building for the generic field columns. */
    private String toFieldsJson(Map<String, String> jsonFields) {
        StringBuilder json = new StringBuilder("{");
        int i = 0;
        for (Map.Entry<String, String> entry : jsonFields.entrySet()) {
            json.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue()).append("\"");
            if (i < jsonFields.size() - 1) json.append(",");
            i++;
        }
        json.append("}");
        return json.toString();
    }
}
