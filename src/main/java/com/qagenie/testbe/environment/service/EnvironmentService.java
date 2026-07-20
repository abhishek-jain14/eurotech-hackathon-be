package com.qagenie.testbe.environment.service;

import com.qagenie.testbe.environment.dto.EnvironmentRequestDto;
import com.qagenie.testbe.environment.dto.EnvironmentResponseDto;

import java.util.List;

public interface EnvironmentService {
    EnvironmentResponseDto create(EnvironmentRequestDto request);
    EnvironmentResponseDto update(Long id, EnvironmentRequestDto request);
    EnvironmentResponseDto getById(Long id);
    List<EnvironmentResponseDto> listByProject(Long projectId);
    void delete(Long id);
}
