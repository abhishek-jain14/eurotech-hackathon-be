package com.qagenie.testbe.application.entity;

import com.qagenie.testbe.common.audit.AuditableEntity;
import com.qagenie.testbe.environment.entity.EnvironmentConfig;
import com.qagenie.testbe.project.entity.Project;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

/**
 * A single onboarded application (backend API or frontend UI), created
 * ONCE per application. Always belongs to a Project (which supplies the
 * shared keystore/truststore used to fetch this application's spec) and
 * optionally references one of that Project's Environments to derive its
 * spec URL: derivedUrl = environment.baseUrl + "/" + this.name +
 * project.specPathSuffix. The user can instead set specSourceMode=CUSTOM
 * and provide their own specSourceUrl, bypassing derivation entirely.
 */
@Getter
@Setter
@Entity
@Table(name = "APPLICATION", uniqueConstraints = @UniqueConstraint(columnNames = {"PROJECT_ID", "NAME"}))
public class Application extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "APPLICATION_ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "PROJECT_ID", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Project project;

    /**
     * The Environment (belonging to the same Project) used to derive this
     * application's spec URL. Nullable when specSourceMode=CUSTOM, since a
     * fully custom URL doesn't need a reference environment for that
     * purpose (though one may still be picked later purely for execution).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REFERENCE_ENVIRONMENT_ID")
    private EnvironmentConfig referenceEnvironment;

    @Column(name = "NAME", nullable = false, length = 150)
    private String name;

    @Column(name = "DESCRIPTION", length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "APPLICATION_TYPE", nullable = false, length = 20)
    private ApplicationType applicationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "SPEC_FORMAT", length = 20)
    private SpecFormat specFormat;

    @Column(name = "SPEC_FILE_NAME", length = 255)
    private String specFileName;

    @Lob
    @Column(name = "SPEC_CONTENT")
    private String specContent;

    /**
     * DERIVED = compute the URL from referenceEnvironment + name + the
     * parent Project's specPathSuffix at fetch time.
     * CUSTOM  = use specSourceUrl exactly as the user provided it.
     */
    @Column(name = "SPEC_SOURCE_MODE", nullable = false, length = 20)
    private String specSourceMode = "DERIVED";

    /**
     * For CUSTOM mode: the user-supplied URL. For DERIVED mode: populated
     * with the computed value after each successful fetch, for display/audit.
     */
    @Column(name = "SPEC_SOURCE_URL", length = 500)
    private String specSourceUrl;

    @Column(name = "SPEC_LAST_FETCHED_AT")
    private Instant specLastFetchedAt;

    @Column(name = "AUTO_SYNC_ENABLED", nullable = false)
    private boolean autoSyncEnabled = false;

    @Column(name = "AUTO_SYNC_INTERVAL_MIN")
    private Integer autoSyncIntervalMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private ApplicationStatus status = ApplicationStatus.ACTIVE;
}
