package com.qagenie.testbe.testdata.service.impl;

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
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class TestDataServiceImpl implements TestDataService {

    private final TestDataRepository testDataRepository;
    private final ApplicationRepository applicationRepository;
    private final TestDataMapper testDataMapper;
    private final TestScenarioRepository testScenarioRepository;

    @Override
    public TestDataResponseDto create(TestDataRequestDto request) {
        Application application = findApplication(request.applicationId());
        TestScenario testScenario= findTestScenario(request.scenarioId());
        if (Objects.isNull(application) || Objects.isNull(testScenario)) {
            throw new RuntimeException("applicationId or scenarioId is not valid");
        }
        TestData entity = testDataMapper.toEntity(request);
        entity.setApplication(application);
        if (entity.getStatus() == null) {
            entity.setStatus(TestDataStatus.VALID);
        }
        return testDataMapper.toResponseDto(testDataRepository.save(entity));
    }

    @Override
    public List<TestDataResponseDto> bulkUpload(Long applicationId, MultipartFile file) {
        Application application = findApplication(applicationId);
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
                StringBuilder json = new StringBuilder("{");
                for (int i = 0; i < columns.length && i < values.length; i++) {
                    json.append("\"").append(columns[i].trim()).append("\":\"").append(values[i].trim()).append("\"");
                    if (i < columns.length - 1) json.append(",");
                }
                json.append("}");

                TestData entity = new TestData();
                entity.setApplication(application);
                entity.setRecordName("record_" + String.format("%03d", rowNum++));
                entity.setMode(TestDataMode.BULK_UPLOAD);
                entity.setStatus(TestDataStatus.VALID);
                entity.setFieldsJson(json.toString());
                records.add(entity);
            }
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

    private TestScenario findTestScenario(Long id){
        return testScenarioRepository.getReferenceById(id);
    }
}
