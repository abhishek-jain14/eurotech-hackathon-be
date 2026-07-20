package com.qagenie.testbe.user.service;

import com.qagenie.testbe.user.dto.UserRequestDto;
import com.qagenie.testbe.user.dto.UserResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
    UserResponseDto create(UserRequestDto request);
    UserResponseDto update(Long id, UserRequestDto request);
    UserResponseDto getById(Long id);
    Page<UserResponseDto> list(Pageable pageable);
    void delete(Long id);
    void deactivate(Long id);
}
