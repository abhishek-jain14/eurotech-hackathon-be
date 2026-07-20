package com.qagenie.testbe.user.service.impl;

import com.qagenie.testbe.common.exception.BusinessException;
import com.qagenie.testbe.common.exception.ResourceNotFoundException;
import com.qagenie.testbe.user.dto.UserRequestDto;
import com.qagenie.testbe.user.dto.UserResponseDto;
import com.qagenie.testbe.user.entity.User;
import com.qagenie.testbe.user.mapper.UserMapper;
import com.qagenie.testbe.user.repository.UserRepository;
import com.qagenie.testbe.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponseDto create(UserRequestDto request) {
        if (userRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new BusinessException("Username already exists: " + request.username(), "USER_EXISTS");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new BusinessException("Password is required when creating a user", "PASSWORD_REQUIRED");
        }
        User entity = userMapper.toEntity(request);
        entity.setPasswordHash(passwordEncoder.encode(request.password()));
        entity.setActive(request.active() == null || request.active());
        User saved = userRepository.save(entity);
        return userMapper.toResponseDto(saved);
    }

    @Override
    public UserResponseDto update(Long id, UserRequestDto request) {
        User entity = findEntity(id);
        userMapper.updateEntityFromDto(request, entity);
        if (request.password() != null && !request.password().isBlank()) {
            entity.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        if (request.active() != null) {
            entity.setActive(request.active());
        }
        return userMapper.toResponseDto(userRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto getById(Long id) {
        return userMapper.toResponseDto(findEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponseDto> list(Pageable pageable) {
        return userRepository.findAll(pageable).map(userMapper::toResponseDto);
    }

    @Override
    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw ResourceNotFoundException.of("User", id);
        }
        userRepository.deleteById(id);
    }

    @Override
    public void deactivate(Long id) {
        User entity = findEntity(id);
        entity.setActive(false);
        userRepository.save(entity);
    }

    private User findEntity(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("User", id));
    }
}
