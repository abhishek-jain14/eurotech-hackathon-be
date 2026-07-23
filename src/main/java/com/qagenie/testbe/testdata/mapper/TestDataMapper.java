package com.qagenie.testbe.testdata.mapper;

import com.qagenie.testbe.testdata.dto.TestDataRequestDto;
import com.qagenie.testbe.testdata.dto.TestDataResponseDto;
import com.qagenie.testbe.testdata.entity.TestData;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface TestDataMapper {

    @Mapping(target = "applicationId", source = "application.id")
    TestDataResponseDto toResponseDto(TestData entity);

    @Mapping(target = "application", ignore = true)
    @Mapping(target = "application.id", source = "applicationId")
    @Mapping(target = "testScenario.id", source = "scenarioId")
    TestData toEntity(TestDataRequestDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "application", ignore = true)
    void updateEntityFromDto(TestDataRequestDto dto, @MappingTarget TestData entity);
}
