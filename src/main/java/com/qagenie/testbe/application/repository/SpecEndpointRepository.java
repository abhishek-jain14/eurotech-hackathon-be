package com.qagenie.testbe.application.repository;

import com.qagenie.testbe.application.entity.SpecEndpoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpecEndpointRepository extends JpaRepository<SpecEndpoint, Long> {
    List<SpecEndpoint> findBySpecVersionId(Long specVersionId);
}
