package com.qagenie.testbe.report.entity;

import com.qagenie.testbe.application.entity.Application;
import com.qagenie.testbe.execution.entity.ExecutionRun;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

/**
 * One row per Application - the current release-readiness sign-off state, re-used
 * (updated in place) across sign-off actions. Tracks which EXECUTION_RUN it was
 * signed off against, so the service can tell the caller the sign-off is "stale"
 * once a newer run has completed since (see ReportServiceImpl).
 */
@Getter
@Setter
@Entity
@Table(name = "REPORT_SIGNOFF", uniqueConstraints = @UniqueConstraint(columnNames = {"APPLICATION_ID"}))
public class ReportSignoff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SIGNOFF_ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "APPLICATION_ID", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Application application;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RUN_ID")
    private ExecutionRun executionRun;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private SignoffStatus status = SignoffStatus.PENDING;

    @Lob
    @Column(name = "SIGNOFF_COMMENT")
    private String comment;

    @Column(name = "SIGNED_OFF_BY", length = 100)
    private String signedOffBy;

    @Column(name = "SIGNED_OFF_AT")
    private Instant signedOffAt;
}
