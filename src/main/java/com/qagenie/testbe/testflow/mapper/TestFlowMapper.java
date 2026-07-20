package com.qagenie.testbe.testflow.mapper;

import com.qagenie.testbe.testflow.dto.TestFlowResponseDto;
import com.qagenie.testbe.testflow.dto.TestFlowStepDto;
import com.qagenie.testbe.testflow.entity.TestFlow;
import com.qagenie.testbe.testflow.entity.TestFlowStep;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TestFlowMapper {

    @Mapping(target = "applicationId", source = "application.id")
    @Mapping(target = "steps", source = "steps")
    TestFlowResponseDto toResponseDto(TestFlow entity);

    @Mapping(target = "scenarioId", source = "scenario.id")
    @Mapping(target = "scenarioName", source = "scenario.name")
    TestFlowStepDto toStepDto(TestFlowStep step);

    List<TestFlowStepDto> toStepDtoList(List<TestFlowStep> steps);
}
