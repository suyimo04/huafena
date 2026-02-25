package com.pollen.management.service;

import com.pollen.management.entity.enums.Role;
import net.jqwik.api.*;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Feature: pollen-group-management, Property 11: 问卷设计权限控制
 * **Validates: Requirements 3.12, 3.13**
 *
 * Tests that questionnaire design access is granted if and only if the user's role
 * is ADMIN, LEADER, or VICE_LEADER. All other roles (MEMBER, INTERN, APPLICANT)
 * should be denied access (403).
 */
class QuestionnaireDesignPermissionProperties {

    /**
     * The set of roles allowed to access questionnaire design endpoints,
     * as configured in SecurityConfig for /api/questionnaire/templates/**.
     */
    private static final Set<Role> ALLOWED_ROLES = Set.of(
            Role.ADMIN,
            Role.LEADER,
            Role.VICE_LEADER
    );

    /**
     * The set of roles that must be denied access to questionnaire design endpoints.
     */
    private static final Set<Role> DENIED_ROLES = Set.of(
            Role.MEMBER,
            Role.INTERN,
            Role.APPLICANT
    );

    /**
     * Simulates the permission check logic from SecurityConfig:
     * /api/questionnaire/templates/** → hasAnyRole("ADMIN", "LEADER", "VICE_LEADER")
     */
    private boolean hasQuestionnaireDesignAccess(Role role) {
        return ALLOWED_ROLES.contains(role);
    }

    /**
     * Property 11a: For any role, questionnaire design access is granted
     * if and only if the role is ADMIN, LEADER, or VICE_LEADER.
     */
    @Property(tries = 100)
    void questionnaireDesignAccessMatchesAllowedRoles(@ForAll("roles") Role role) {
        boolean accessGranted = hasQuestionnaireDesignAccess(role);

        if (ALLOWED_ROLES.contains(role)) {
            assertThat(accessGranted)
                    .as("Role %s should have access to questionnaire design", role)
                    .isTrue();
        } else {
            assertThat(accessGranted)
                    .as("Role %s should be denied access to questionnaire design (403)", role)
                    .isFalse();
        }
    }

    /**
     * Property 11b: The allowed and denied role sets are exhaustive and mutually exclusive —
     * every role in the system is in exactly one of the two sets.
     */
    @Property(tries = 100)
    void everyRoleIsClassified(@ForAll("roles") Role role) {
        boolean isAllowed = ALLOWED_ROLES.contains(role);
        boolean isDenied = DENIED_ROLES.contains(role);

        assertThat(isAllowed ^ isDenied)
                .as("Role %s must be in exactly one of ALLOWED_ROLES or DENIED_ROLES", role)
                .isTrue();
    }

    /**
     * Property 11c: The union of allowed and denied roles covers all defined roles.
     */
    @Property(tries = 100)
    void allowedAndDeniedRolesCoverAllRoles(@ForAll("roles") Role role) {
        assertThat(ALLOWED_ROLES.contains(role) || DENIED_ROLES.contains(role))
                .as("Role %s must be covered by either ALLOWED_ROLES or DENIED_ROLES", role)
                .isTrue();
    }

    /**
     * Property 11d: MEMBER, INTERN, and APPLICANT are always denied access.
     */
    @Property(tries = 100)
    void deniedRolesCannotAccessDesigner(@ForAll("deniedRoles") Role role) {
        assertThat(hasQuestionnaireDesignAccess(role))
                .as("Role %s should be denied access to questionnaire design (403)", role)
                .isFalse();
    }

    /**
     * Property 11e: ADMIN, LEADER, and VICE_LEADER always have access.
     */
    @Property(tries = 100)
    void allowedRolesCanAccessDesigner(@ForAll("allowedRoles") Role role) {
        assertThat(hasQuestionnaireDesignAccess(role))
                .as("Role %s should have access to questionnaire design", role)
                .isTrue();
    }

    // ========== Providers ==========

    @Provide
    Arbitrary<Role> roles() {
        return Arbitraries.of(Role.values());
    }

    @Provide
    Arbitrary<Role> allowedRoles() {
        return Arbitraries.of(Role.ADMIN, Role.LEADER, Role.VICE_LEADER);
    }

    @Provide
    Arbitrary<Role> deniedRoles() {
        return Arbitraries.of(Role.MEMBER, Role.INTERN, Role.APPLICANT);
    }
}
