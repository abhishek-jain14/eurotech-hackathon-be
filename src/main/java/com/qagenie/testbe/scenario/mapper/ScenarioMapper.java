package com.qagenie.testbe.scenario.mapper;

import com.qagenie.testbe.scenario.dto.ScenarioRequestDto;
import com.qagenie.testbe.scenario.dto.ScenarioResponseDto;
import com.qagenie.testbe.scenario.entity.TestScenario;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ScenarioMapper {

    @Mapping(target = "applicationId", source = "application.id")
    @Mapping(target = "applicationName", source = "application.name")
    @Mapping(target = "specVersionNumber", source = "specVersion.versionNumber")
    ScenarioResponseDto toResponseDto(TestScenario entity);

    @Mapping(target = "application", ignore = true)
    @Mapping(target = "specVersion", ignore = true)
    TestScenario toEntity(ScenarioRequestDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "application", ignore = true)
    @Mapping(target = "specVersion", ignore = true)
    void updateEntityFromDto(ScenarioRequestDto dto, @MappingTarget TestScenario entity);
}
