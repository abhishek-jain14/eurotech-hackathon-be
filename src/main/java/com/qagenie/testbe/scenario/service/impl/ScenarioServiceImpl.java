package com.qagenie.testbe.scenario.service.impl;

import com.qagenie.testbe.application.entity.SpecVersionStatus;
import com.qagenie.testbe.application.repository.SpecVersionRepository;
import com.qagenie.testbe.common.exception.ResourceNotFoundException;
import com.qagenie.testbe.application.entity.Application;
import com.qagenie.testbe.application.repository.ApplicationRepository;
import com.qagenie.testbe.scenario.dto.ScenarioRequestDto;
import com.qagenie.testbe.scenario.dto.ScenarioResponseDto;
import com.qagenie.testbe.scenario.entity.TestScenario;
import com.qagenie.testbe.scenario.mapper.ScenarioMapper;
import com.qagenie.testbe.scenario.repository.TestScenarioRepository;
import com.qagenie.testbe.scenario.service.GherkinGenerator;
import com.qagenie.testbe.scenario.service.ScenarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ScenarioServiceImpl implements ScenarioService {

    private final TestScenarioRepository scenarioRepository;
    private final ApplicationRepository applicationRepository;
    private final SpecVersionRepository specVersionRepository;
    private final ScenarioMapper scenarioMapper;

    @Autowired
    private GherkinGenerator gherkinGenerator;

    @Override
    public ScenarioResponseDto create(ScenarioRequestDto request) {
        Application application = applicationRepository.findById(request.applicationId())
                .orElseThrow(() -> ResourceNotFoundException.of("Application", request.applicationId()));
        TestScenario entity = scenarioMapper.toEntity(request);
        entity.setApplication(application);
        entity.setDescription(gherkinGenerator.generateGherkin(request.apiTestData()));
        // Stamp whichever spec version is CURRENT right now, so later drift
        // detection can tell precisely which scenarios a given change affects.
        // Left null if the application has no spec ingested yet (manual
        // scenario authored ahead of onboarding a spec) - not an error case.
        specVersionRepository.findByApplicationIdAndStatus(application.getId(), SpecVersionStatus.CURRENT)
                .ifPresent(entity::setSpecVersion);

        return scenarioMapper.toResponseDto(scenarioRepository.save(entity));
    }

    @Override
    public ScenarioResponseDto update(Long id, ScenarioRequestDto request) {
        TestScenario entity = findEntity(id);
        scenarioMapper.updateEntityFromDto(request, entity);
        return scenarioMapper.toResponseDto(scenarioRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public ScenarioResponseDto getById(Long id) {
        return scenarioMapper.toResponseDto(findEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ScenarioResponseDto> listByApplication(Long applicationId, Pageable pageable) {
        return scenarioRepository.findByApplicationId(applicationId, pageable).map(scenarioMapper::toResponseDto);
    }

    @Override
    public void delete(Long id) {
        if (!scenarioRepository.existsById(id)) {
            throw ResourceNotFoundException.of("TestScenario", id);
        }
        scenarioRepository.deleteById(id);
    }

    private TestScenario findEntity(Long id) {
        return scenarioRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("TestScenario", id));
    }
}
