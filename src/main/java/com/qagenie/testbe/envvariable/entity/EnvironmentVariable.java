package com.qagenie.testbe.envvariable.entity;

import com.qagenie.testbe.common.audit.AuditableEntity;
import com.qagenie.testbe.project.entity.Project;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * Simple name/value config pair scoped to a Project - shown behind the
 * "Manage" button on the Project list. Distinct from EnvironmentConfig
 * (which drives spec URL derivation and test execution): this is just
 * free-form key/value data a project owner wants to keep track of.
 */
@Getter
@Setter
@Entity
@Table(name = "PROJECT_ENV_VARIABLE",
        uniqueConstraints = @UniqueConstraint(columnNames = {"PROJECT_ID", "VAR_NAME"}))
public class EnvironmentVariable extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ENV_VARIABLE_ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "PROJECT_ID", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Project project;

    @Column(name = "VAR_NAME", nullable = false, length = 150)
    private String name;

    @Column(name = "VAR_VALUE", length = 2000)
    private String value;
}