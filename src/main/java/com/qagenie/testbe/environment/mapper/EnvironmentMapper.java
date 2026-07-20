package com.qagenie.testbe.environment.mapper;

import com.qagenie.testbe.environment.dto.EnvironmentRequestDto;
import com.qagenie.testbe.environment.dto.EnvironmentResponseDto;
import com.qagenie.testbe.environment.entity.EnvironmentConfig;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface EnvironmentMapper {

    @Mapping(target = "projectId", source = "project.id")
    @Mapping(target = "projectName", source = "project.name")
    EnvironmentResponseDto toResponseDto(EnvironmentConfig entity);

    @Mapping(target = "project", ignore = true)
    EnvironmentConfig toEntity(EnvironmentRequestDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "project", ignore = true)
    void updateEntityFromDto(EnvironmentRequestDto dto, @MappingTarget EnvironmentConfig entity);
}
