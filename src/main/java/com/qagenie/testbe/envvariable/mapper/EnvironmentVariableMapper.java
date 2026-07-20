package com.qagenie.testbe.envvariable.mapper;

import com.qagenie.testbe.envvariable.dto.EnvironmentVariableRequestDto;
import com.qagenie.testbe.envvariable.dto.EnvironmentVariableResponseDto;
import com.qagenie.testbe.envvariable.entity.EnvironmentVariable;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface EnvironmentVariableMapper {

    @Mapping(target = "projectId", source = "project.id")
    EnvironmentVariableResponseDto toResponseDto(EnvironmentVariable entity);

    @Mapping(target = "project", ignore = true)
    EnvironmentVariable toEntity(EnvironmentVariableRequestDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "project", ignore = true)
    void updateEntityFromDto(EnvironmentVariableRequestDto dto, @MappingTarget EnvironmentVariable entity);
}