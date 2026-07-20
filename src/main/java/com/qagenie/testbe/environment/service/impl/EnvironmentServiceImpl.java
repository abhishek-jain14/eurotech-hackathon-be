package com.qagenie.testbe.environment.service.impl;

import com.qagenie.testbe.common.exception.BusinessException;
import com.qagenie.testbe.common.exception.ResourceNotFoundException;
import com.qagenie.testbe.environment.dto.EnvironmentRequestDto;
import com.qagenie.testbe.environment.dto.EnvironmentResponseDto;
import com.qagenie.testbe.environment.entity.EnvironmentConfig;
import com.qagenie.testbe.environment.mapper.EnvironmentMapper;
import com.qagenie.testbe.environment.repository.EnvironmentConfigRepository;
import com.qagenie.testbe.environment.service.EnvironmentService;
import com.qagenie.testbe.project.entity.Project;
import com.qagenie.testbe.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class EnvironmentServiceImpl implements EnvironmentService {

    private static final Logger log = LoggerFactory.getLogger(EnvironmentServiceImpl.class);

    private final EnvironmentConfigRepository environmentRepository;
    private final ProjectRepository projectRepository;
    private final EnvironmentMapper environmentMapper;

    @Override
    public EnvironmentResponseDto create(EnvironmentRequestDto request) {
        Project project = projectRepository.findById(request.projectId())
                .orElseThrow(() -> ResourceNotFoundException.of("Project", request.projectId()));

        if (environmentRepository.existsByProjectIdAndEnvNameIgnoreCase(request.projectId(), request.envName())) {
            throw new BusinessException("Environment '" + request.envName() + "' already exists for this project", "ENV_EXISTS");
        }

        EnvironmentConfig entity = environmentMapper.toEntity(request);
        entity.setProject(project);
        EnvironmentConfig saved = environmentRepository.save(entity);
        log.info("Environment created: id={}, project={}, env={}, baseUrl={}",
                saved.getId(), project.getName(), saved.getEnvName(), saved.getBaseUrl());
        return environmentMapper.toResponseDto(saved);
    }

    @Override
    public EnvironmentResponseDto update(Long id, EnvironmentRequestDto request) {
        EnvironmentConfig entity = findEntity(id);
        environmentMapper.updateEntityFromDto(request, entity);
        return environmentMapper.toResponseDto(environmentRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public EnvironmentResponseDto getById(Long id) {
        return environmentMapper.toResponseDto(findEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnvironmentResponseDto> listByProject(Long projectId) {
        return environmentRepository.findByProjectId(projectId).stream()
                .map(environmentMapper::toResponseDto)
                .toList();
    }

    @Override
    public void delete(Long id) {
        if (!environmentRepository.existsById(id)) {
            throw ResourceNotFoundException.of("EnvironmentConfig", id);
        }
        environmentRepository.deleteById(id);
    }

    private EnvironmentConfig findEntity(Long id) {
        return environmentRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("EnvironmentConfig", id));
    }
}
