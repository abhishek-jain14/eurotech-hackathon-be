package com.qagenie.testbe.project.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qagenie.testbe.common.exception.BusinessException;
import com.qagenie.testbe.common.exception.ResourceNotFoundException;
import com.qagenie.testbe.project.dto.ProjectRequestDto;
import com.qagenie.testbe.project.dto.ProjectResponseDto;
import com.qagenie.testbe.project.entity.Project;
import com.qagenie.testbe.project.mapper.ProjectMapper;
import com.qagenie.testbe.project.repository.ProjectRepository;
import com.qagenie.testbe.project.service.ProjectService;
import com.qagenie.testbe.project.tls.TlsAuthConfig;
import com.qagenie.testbe.project.tls.TlsMaterialService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectServiceImpl implements ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectServiceImpl.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;
    private final TlsMaterialService tlsMaterialService;

    @Override
    public ProjectResponseDto create(ProjectRequestDto request) {
        if (projectRepository.existsByNameIgnoreCase(request.name())) {
            throw new BusinessException("A project with this name already exists: " + request.name(), "PROJECT_EXISTS");
        }
        Project entity = projectMapper.toEntity(request);
        Project saved = projectRepository.save(entity);
        log.info("Project created: id={}, name={}", saved.getId(), saved.getName());
        return projectMapper.toResponseDto(saved);
    }

    @Override
    public ProjectResponseDto update(Long id, ProjectRequestDto request) {
        Project entity = findEntity(id);
        projectMapper.updateEntityFromDto(request, entity);
        return projectMapper.toResponseDto(projectRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectResponseDto getById(Long id) {
        return projectMapper.toResponseDto(findEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProjectResponseDto> list(Pageable pageable) {
        return projectRepository.findAll(pageable).map(projectMapper::toResponseDto);
    }

    @Override
    public void delete(Long id) {
        if (!projectRepository.existsById(id)) {
            throw ResourceNotFoundException.of("Project", id);
        }
        projectRepository.deleteById(id);
    }

    @Override
    public ProjectResponseDto configureTlsAuth(Long projectId, String authType, MultipartFile keystoreFile, String keystorePassword,
                                                MultipartFile truststoreFile, String truststorePassword,
                                                String username, String password, String bearerToken,
                                                String apiKeyHeaderName, String apiKeyValue) {
        Project entity = findEntity(projectId);

        TlsAuthConfig config = new TlsAuthConfig();
        config.username = username;
        config.password = password;
        config.bearerToken = bearerToken;
        config.apiKeyHeaderName = apiKeyHeaderName;
        config.apiKeyValue = apiKeyValue;

        if (keystoreFile != null && !keystoreFile.isEmpty()) {
            config.keystorePath = tlsMaterialService.storeKeystore(projectId, keystoreFile, "keystore");
            config.keystorePassword = keystorePassword;
        }
        if (truststoreFile != null && !truststoreFile.isEmpty()) {
            config.truststorePath = tlsMaterialService.storeKeystore(projectId, truststoreFile, "truststore");
            config.truststorePassword = truststorePassword;
        }

        if ("MUTUAL_TLS".equals(authType) && config.keystorePath == null) {
            throw new BusinessException("MUTUAL_TLS requires at least a keystore file upload", "TLS_CONFIG_MISSING");
        }

        try {
            entity.setSpecAuthType(authType);
            entity.setSpecAuthConfigJson(MAPPER.writeValueAsString(config));
        } catch (Exception e) {
            throw new BusinessException("Unable to persist TLS/auth config: " + e.getMessage(), "TLS_CONFIG_INVALID");
        }

        log.info("TLS/auth config updated for project id={}, authType={}. Every application under this " +
                "project will use this material for spec fetches.", projectId, authType);
        return projectMapper.toResponseDto(projectRepository.save(entity));
    }

    private Project findEntity(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Project", id));
    }
}
