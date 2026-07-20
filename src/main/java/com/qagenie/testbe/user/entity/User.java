package com.qagenie.testbe.user.entity;

import com.qagenie.testbe.common.audit.AuditableEntity;
import com.qagenie.testbe.security.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "APP_USER")
public class User extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "USER_ID")
    private Long id;

    @Column(name = "USERNAME", nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "PASSWORD_HASH", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "FULL_NAME", length = 150)
    private String fullName;

    @Column(name = "EMAIL", length = 150)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "ROLE", nullable = false, length = 20)
    private Role role;

    @Column(name = "IS_ACTIVE", nullable = false)
    private boolean active = true;
}
