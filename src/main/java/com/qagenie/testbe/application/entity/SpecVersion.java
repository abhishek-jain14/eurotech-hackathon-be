package com.qagenie.testbe.application.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

/**
 * One row per DISTINCT spec content ever seen for an application (hash-
 * guarded: a re-fetch/re-upload that returns identical bytes does NOT
 * create a new row, it just bumps lastCheckedAt on the existing one).
 *
 * Exactly one row per application has status=CURRENT at any time - that's
 * what scenario generation and execution read. Every version after the
 * first starts life as PENDING and stays there until a human approves or
 * rejects it (see ApplicationService#approveSpecVersion/rejectSpecVersion) -
 * nothing is promoted automatically, regardless of whether the change came
 * from a manual re-upload/re-fetch or the Change Tracker's background
 * drift detection.
 */
@Getter
@Setter
@Entity
@Table(name = "SPEC_VERSION", uniqueConstraints = @UniqueConstraint(columnNames = {"APPLICATION_ID", "CONTENT_HASH"}))
public class SpecVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SPEC_VERSION_ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "APPLICATION_ID", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Application application;

    @Column(name = "VERSION_NUMBER", nullable = false)
    private Integer versionNumber;

    @Lob
    @Column(name = "CONTENT", nullable = false)
    private String content;

    @Column(name = "CONTENT_HASH", nullable = false, length = 64)
    private String contentHash;

    @Column(name = "FILE_NAME", length = 255)
    private String fileName;

    @Enumerated(EnumType.STRING)
    @Column(name = "SOURCE", nullable = false, length = 20)
    private SpecSource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private SpecVersionStatus status;

    @Column(name = "FETCHED_AT", nullable = false)
    private Instant fetchedAt;

    @Column(name = "LAST_CHECKED_AT")
    private Instant lastCheckedAt;

    @Column(name = "REVIEWED_AT")
    private Instant reviewedAt;

    @Column(name = "REVIEWED_BY", length = 100)
    private String reviewedBy;
}
