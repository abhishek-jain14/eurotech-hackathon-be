package com.qagenie.testbe.project.service;

import com.qagenie.testbe.project.dto.ProjectRequestDto;
import com.qagenie.testbe.project.dto.ProjectResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface ProjectService {
    ProjectResponseDto create(ProjectRequestDto request);
    ProjectResponseDto update(Long id, ProjectRequestDto request);
    ProjectResponseDto getById(Long id);
    Page<ProjectResponseDto> list(Pageable pageable);
    void delete(Long id);

    ProjectResponseDto configureTlsAuth(Long projectId, String authType, MultipartFile keystoreFile, String keystorePassword,
                                         MultipartFile truststoreFile, String truststorePassword,
                                         String username, String password, String bearerToken,
                                         String apiKeyHeaderName, String apiKeyValue);
}
