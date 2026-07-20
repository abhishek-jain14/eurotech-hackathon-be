package com.qagenie.testbe.execution.service;

import com.qagenie.testbe.execution.dto.ExecutionRequestDto;
import com.qagenie.testbe.execution.dto.ExecutionRunResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ExecutionService {
    ExecutionRunResponseDto trigger(ExecutionRequestDto request);
    ExecutionRunResponseDto getRunById(Long runId);
    Page<ExecutionRunResponseDto> listByApplication(Long applicationId, Pageable pageable);
}
