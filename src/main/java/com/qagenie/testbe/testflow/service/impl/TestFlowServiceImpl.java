package com.qagenie.testbe.testflow.service.impl;

import com.qagenie.testbe.common.exception.ResourceNotFoundException;
import com.qagenie.testbe.application.entity.Application;
import com.qagenie.testbe.application.repository.ApplicationRepository;
import com.qagenie.testbe.scenario.entity.TestScenario;
import com.qagenie.testbe.scenario.repository.TestScenarioRepository;
import com.qagenie.testbe.testflow.dto.TestFlowRequestDto;
import com.qagenie.testbe.testflow.dto.TestFlowResponseDto;
import com.qagenie.testbe.testflow.dto.TestFlowStepDto;
import com.qagenie.testbe.testflow.entity.TestFlow;
import com.qagenie.testbe.testflow.entity.TestFlowStep;
import com.qagenie.testbe.testflow.mapper.TestFlowMapper;
import com.qagenie.testbe.testflow.repository.TestFlowRepository;
import com.qagenie.testbe.testflow.service.TestFlowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TestFlowServiceImpl implements TestFlowService {

    private final TestFlowRepository testFlowRepository;
    private final ApplicationRepository applicationRepository;
    private final TestScenarioRepository scenarioRepository;
    private final TestFlowMapper testFlowMapper;

    @Override
    public TestFlowResponseDto create(TestFlowRequestDto request) {
        Application application = applicationRepository.findById(request.applicationId())
                .orElseThrow(() -> ResourceNotFoundException.of("Application", request.applicationId()));

        TestFlow flow = new TestFlow();
        flow.setApplication(application);
        flow.setName(request.name());
        flow.setDescription(request.description());
        flow.setActive(request.active() == null || request.active());
        applySteps(flow, request.steps());

        return testFlowMapper.toResponseDto(testFlowRepository.save(flow));
    }

    @Override
    public TestFlowResponseDto update(Long id, TestFlowRequestDto request) {
        TestFlow flow = findEntity(id);
        flow.setName(request.name());
        flow.setDescription(request.description());
        if (request.active() != null) {
            flow.setActive(request.active());
        }
        flow.getSteps().clear();
        applySteps(flow, request.steps());
        return testFlowMapper.toResponseDto(testFlowRepository.save(flow));
    }

    @Override
    @Transactional(readOnly = true)
    public TestFlowResponseDto getById(Long id) {
        return testFlowMapper.toResponseDto(findEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestFlowResponseDto> listByApplication(Long applicationId) {
        return testFlowRepository.findByApplicationId(applicationId).stream().map(testFlowMapper::toResponseDto).toList();
    }

    @Override
    public void delete(Long id) {
        if (!testFlowRepository.existsById(id)) {
            throw ResourceNotFoundException.of("TestFlow", id);
        }
        testFlowRepository.deleteById(id);
    }

    private void applySteps(TestFlow flow, List<TestFlowStepDto> stepDtos) {
        for (TestFlowStepDto stepDto : stepDtos) {
            TestScenario scenario = scenarioRepository.findById(stepDto.scenarioId())
                    .orElseThrow(() -> ResourceNotFoundException.of("TestScenario", stepDto.scenarioId()));
            TestFlowStep step = new TestFlowStep();
            step.setTestFlow(flow);
            step.setScenario(scenario);
            step.setSequenceOrder(stepDto.sequenceOrder());
            flow.getSteps().add(step);
        }
    }

    private TestFlow findEntity(Long id) {
        return testFlowRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("TestFlow", id));
    }
}
