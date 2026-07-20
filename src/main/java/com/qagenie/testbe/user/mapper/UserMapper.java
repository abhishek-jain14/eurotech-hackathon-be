package com.qagenie.testbe.user.mapper;

import com.qagenie.testbe.user.dto.UserRequestDto;
import com.qagenie.testbe.user.dto.UserResponseDto;
import com.qagenie.testbe.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.BeanMapping;

/**
 * Converts between the controller-facing DTOs and the local JPA entity.
 * Password hashing is handled explicitly in the service layer, never here -
 * passwordHash is intentionally ignored by this mapper.
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponseDto toResponseDto(User entity);

    @Mapping(target = "passwordHash", ignore = true)
    User toEntity(UserRequestDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "passwordHash", ignore = true)
    void updateEntityFromDto(UserRequestDto dto, @MappingTarget User entity);
}
