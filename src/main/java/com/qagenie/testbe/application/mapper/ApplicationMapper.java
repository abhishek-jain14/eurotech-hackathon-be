package com.qagenie.testbe.application.mapper;

import com.qagenie.testbe.application.dto.ApplicationRequestDto;
import com.qagenie.testbe.application.dto.ApplicationResponseDto;
import com.qagenie.testbe.application.entity.Application;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface ApplicationMapper {

    @Mapping(target = "projectId", source = "project.id")
    @Mapping(target = "projectName", source = "project.name")
    @Mapping(target = "referenceEnvironmentId", source = "referenceEnvironment.id")
    @Mapping(target = "referenceEnvironmentName", source = "referenceEnvironment.envName")
    @Mapping(target = "currentSpecVersionNumber", ignore = true)
    @Mapping(target = "hasPendingSpecVersion", ignore = true)
    ApplicationResponseDto toResponseDto(Application entity);

    @Mapping(target = "project", ignore = true)
    @Mapping(target = "referenceEnvironment", ignore = true)
    Application toEntity(ApplicationRequestDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "project", ignore = true)
    @Mapping(target = "referenceEnvironment", ignore = true)
    void updateEntityFromDto(ApplicationRequestDto dto, @MappingTarget Application entity);
}
