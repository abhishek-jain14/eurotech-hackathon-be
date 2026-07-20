package com.qagenie.testbe.testflow.entity;

import com.qagenie.testbe.scenario.entity.TestScenario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "TEST_FLOW_STEP")
public class TestFlowStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FLOW_STEP_ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "FLOW_ID", nullable = false)
    private TestFlow testFlow;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "SCENARIO_ID", nullable = false)
    private TestScenario scenario;

    @Column(name = "SEQUENCE_ORDER", nullable = false)
    private Integer sequenceOrder;
}
