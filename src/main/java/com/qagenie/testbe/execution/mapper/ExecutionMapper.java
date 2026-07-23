package com.qagenie.testbe.execution.mapper;

import com.qagenie.testbe.execution.dto.ExecutionResultDto;
import com.qagenie.testbe.execution.dto.ExecutionRunResponseDto;
import com.qagenie.testbe.execution.entity.ExecutionResult;
import com.qagenie.testbe.execution.entity.ExecutionRun;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ExecutionMapper {

    @Mapping(target = "applicationId", source = "application.id")
    @Mapping(target = "environmentId", source = "environment.id")
    @Mapping(target = "environmentName", source = "environment.envName")
    @Mapping(target = "results", ignore = true)
    ExecutionRunResponseDto toResponseDto(ExecutionRun entity);

    @Mapping(target = "scenarioId", source = "scenario.id")
    @Mapping(target = "scenarioName", source = "scenario.name")
    @Mapping(target = "testDataId", source = "testData.id")
    @Mapping(target = "testDataRecordName", source = "testData.recordName")
    ExecutionResultDto toResultDto(ExecutionResult entity);

    List<ExecutionResultDto> toResultDtoList(List<ExecutionResult> entities);
}
