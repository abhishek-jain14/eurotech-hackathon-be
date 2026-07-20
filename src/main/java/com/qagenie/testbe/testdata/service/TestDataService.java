package com.qagenie.testbe.testdata.service;

import com.qagenie.testbe.testdata.dto.TestDataRequestDto;
import com.qagenie.testbe.testdata.dto.TestDataResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface TestDataService {
    TestDataResponseDto create(TestDataRequestDto request);
    List<TestDataResponseDto> bulkUpload(Long applicationId, MultipartFile file);
    TestDataResponseDto update(Long id, TestDataRequestDto request);
    TestDataResponseDto getById(Long id);
    Page<TestDataResponseDto> listByApplication(Long applicationId, Pageable pageable);
    void delete(Long id);
}
