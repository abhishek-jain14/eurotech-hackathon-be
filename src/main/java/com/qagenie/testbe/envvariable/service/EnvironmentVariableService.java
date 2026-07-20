package com.qagenie.testbe.envvariable.service;

import com.qagenie.testbe.envvariable.dto.EnvironmentVariableRequestDto;
import com.qagenie.testbe.envvariable.dto.EnvironmentVariableResponseDto;

import java.util.List;

public interface EnvironmentVariableService {
    EnvironmentVariableResponseDto create(EnvironmentVariableRequestDto request);
    EnvironmentVariableResponseDto update(Long id, EnvironmentVariableRequestDto request);
    EnvironmentVariableResponseDto getById(Long id);
    List<EnvironmentVariableResponseDto> listByProject(Long projectId);
    void delete(Long id);
}