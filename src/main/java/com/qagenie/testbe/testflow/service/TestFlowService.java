package com.qagenie.testbe.testflow.service;

import com.qagenie.testbe.testflow.dto.TestFlowRequestDto;
import com.qagenie.testbe.testflow.dto.TestFlowResponseDto;
import java.util.List;

public interface TestFlowService {
    TestFlowResponseDto create(TestFlowRequestDto request);
    TestFlowResponseDto update(Long id, TestFlowRequestDto request);
    TestFlowResponseDto getById(Long id);
    List<TestFlowResponseDto> listByApplication(Long applicationId);
    void delete(Long id);
}
