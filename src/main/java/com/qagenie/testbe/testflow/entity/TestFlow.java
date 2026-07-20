package com.qagenie.testbe.testflow.entity;

import com.qagenie.testbe.common.audit.AuditableEntity;
import com.qagenie.testbe.application.entity.Application;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "TEST_FLOW")
public class TestFlow extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FLOW_ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "APPLICATION_ID", nullable = false)
    private Application application;

    @Column(name = "NAME", nullable = false, length = 200)
    private String name;

    @Column(name = "DESCRIPTION", length = 1000)
    private String description;

    @Column(name = "IS_ACTIVE", nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "testFlow", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequenceOrder ASC")
    private List<TestFlowStep> steps = new ArrayList<>();
}
