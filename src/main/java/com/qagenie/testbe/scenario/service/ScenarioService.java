package com.qagenie.testbe.scenario.service;

import com.qagenie.testbe.scenario.dto.ScenarioRequestDto;
import com.qagenie.testbe.scenario.dto.ScenarioResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ScenarioService {
    ScenarioResponseDto create(ScenarioRequestDto request);
    ScenarioResponseDto update(Long id, ScenarioRequestDto request);
    ScenarioResponseDto getById(Long id);
    Page<ScenarioResponseDto> listByApplication(Long applicationId, Pageable pageable);
    void delete(Long id);
}
