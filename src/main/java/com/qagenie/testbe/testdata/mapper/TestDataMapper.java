package com.qagenie.testbe.testdata.mapper;

import com.qagenie.testbe.testdata.dto.TestDataRequestDto;
import com.qagenie.testbe.testdata.dto.TestDataResponseDto;
import com.qagenie.testbe.testdata.entity.TestData;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface TestDataMapper {

    @Mapping(target = "applicationId", source = "application.id")
    @Mapping(target = "scenarioId", source = "testScenario.id")
    @Mapping(target = "scenarioName", source = "testScenario.name")
    TestDataResponseDto toResponseDto(TestData entity);

    @Mapping(target = "application", ignore = true)
    @Mapping(target = "testScenario", ignore = true)
    TestData toEntity(TestDataRequestDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "application", ignore = true)
    @Mapping(target = "testScenario", ignore = true)
    void updateEntityFromDto(TestDataRequestDto dto, @MappingTarget TestData entity);
}
