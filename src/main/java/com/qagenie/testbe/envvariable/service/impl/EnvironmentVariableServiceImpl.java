package com.qagenie.testbe.envvariable.service.impl;

import com.qagenie.testbe.common.exception.BusinessException;
import com.qagenie.testbe.common.exception.ResourceNotFoundException;
import com.qagenie.testbe.envvariable.dto.EnvironmentVariableRequestDto;
import com.qagenie.testbe.envvariable.dto.EnvironmentVariableResponseDto;
import com.qagenie.testbe.envvariable.entity.EnvironmentVariable;
import com.qagenie.testbe.envvariable.mapper.EnvironmentVariableMapper;
import com.qagenie.testbe.envvariable.repository.EnvironmentVariableRepository;
import com.qagenie.testbe.envvariable.service.EnvironmentVariableService;
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
public class EnvironmentVariableServiceImpl implements EnvironmentVariableService {

    private static final Logger log = LoggerFactory.getLogger(EnvironmentVariableServiceImpl.class);

    private final EnvironmentVariableRepository environmentVariableRepository;
    private final ProjectRepository projectRepository;
    private final EnvironmentVariableMapper environmentVariableMapper;

    @Override
    public EnvironmentVariableResponseDto create(EnvironmentVariableRequestDto request) {
        Project project = projectRepository.findById(request.projectId())
                .orElseThrow(() -> ResourceNotFoundException.of("Project", request.projectId()));

        if (environmentVariableRepository.existsByProjectIdAndNameIgnoreCase(request.projectId(), request.name())) {
            throw new BusinessException("Environment variable '" + request.name() + "' already exists for this project", "ENV_VARIABLE_EXISTS");
        }

        EnvironmentVariable entity = environmentVariableMapper.toEntity(request);
        entity.setProject(project);
        EnvironmentVariable saved = environmentVariableRepository.save(entity);
        log.info("Environment variable created: id={}, project={}, name={}", saved.getId(), project.getName(), saved.getName());
        return environmentVariableMapper.toResponseDto(saved);
    }

    @Override
    public EnvironmentVariableResponseDto update(Long id, EnvironmentVariableRequestDto request) {
        EnvironmentVariable entity = findEntity(id);

        if (!entity.getName().equalsIgnoreCase(request.name())
                && environmentVariableRepository.existsByProjectIdAndNameIgnoreCase(entity.getProject().getId(), request.name())) {
            throw new BusinessException("Environment variable '" + request.name() + "' already exists for this project", "ENV_VARIABLE_EXISTS");
        }

        environmentVariableMapper.updateEntityFromDto(request, entity);
        return environmentVariableMapper.toResponseDto(environmentVariableRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public EnvironmentVariableResponseDto getById(Long id) {
        return environmentVariableMapper.toResponseDto(findEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnvironmentVariableResponseDto> listByProject(Long projectId) {
        return environmentVariableRepository.findByProjectId(projectId).stream()
                .map(environmentVariableMapper::toResponseDto)
                .toList();
    }

    @Override
    public void delete(Long id) {
        if (!environmentVariableRepository.existsById(id)) {
            throw ResourceNotFoundException.of("EnvironmentVariable", id);
        }
        environmentVariableRepository.deleteById(id);
    }

    private EnvironmentVariable findEntity(Long id) {
        return environmentVariableRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("EnvironmentVariable", id));
    }
}