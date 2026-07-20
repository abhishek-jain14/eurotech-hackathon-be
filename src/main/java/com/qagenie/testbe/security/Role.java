package com.qagenie.testbe.security;

/**
 * The 3 platform roles.
 * ADMIN   - full access: onboarding, environment config, user management, everything.
 * TESTER  - full access to test authoring/execution, no user management.
 * VIEWER  - read-only access across all screens, no create/edit/delete.
 */
public enum Role {
    ADMIN,
    TESTER,
    VIEWER
}
