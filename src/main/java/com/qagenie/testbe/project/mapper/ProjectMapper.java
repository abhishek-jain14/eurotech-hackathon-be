package com.qagenie.testbe.project.mapper;

import com.qagenie.testbe.project.dto.ProjectRequestDto;
import com.qagenie.testbe.project.dto.ProjectResponseDto;
import com.qagenie.testbe.project.entity.Project;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface ProjectMapper {

    @Mapping(target = "tlsConfigured", expression = "java(entity.getSpecAuthConfigJson() != null)")
    ProjectResponseDto toResponseDto(Project entity);

    @Mapping(target = "specAuthType", ignore = true)
    @Mapping(target = "specAuthConfigJson", ignore = true)
    Project toEntity(ProjectRequestDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "specAuthType", ignore = true)
    @Mapping(target = "specAuthConfigJson", ignore = true)
    void updateEntityFromDto(ProjectRequestDto dto, @MappingTarget Project entity);
}
