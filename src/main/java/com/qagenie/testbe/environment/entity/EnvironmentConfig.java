package com.qagenie.testbe.environment.entity;

import com.qagenie.testbe.common.audit.AuditableEntity;
import com.qagenie.testbe.project.entity.Project;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Repeatable environment configuration for a Project (Dev/Staging/Prod, or
 * further custom named environments). A Project can have any number of
 * these; Applications onboarded under the same Project pick one to derive
 * their spec URL and to execute tests against.
 */
@Getter
@Setter
@Entity
@Table(name = "ENVIRONMENT_CONFIG",
        uniqueConstraints = @UniqueConstraint(columnNames = {"PROJECT_ID", "ENV_NAME"}))
public class EnvironmentConfig extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ENVIRONMENT_ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "PROJECT_ID", nullable = false)
    private Project project;

    @Column(name = "ENV_NAME", nullable = false, length = 50)
    private String envName;

    /**
     * What this environment config entry is for: SwaggerUrl | Database.
     */
    @Column(name = "CONFIG_TYPE", nullable = false, length = 30)
    private String configType;

    @Column(name = "BASE_URL", nullable = false, length = 500)
    private String baseUrl;

    @Column(name = "IS_ACTIVE", nullable = false)
    private boolean active = true;
}
