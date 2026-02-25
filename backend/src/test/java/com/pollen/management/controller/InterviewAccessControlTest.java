package com.pollen.management.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 面试数据访问控制测试：验证 InterviewController 上的 @PreAuthorize 注解
 * 确保 ADMIN/LEADER 拥有完整读写访问，VICE_LEADER 仅有只读访问。
 * Validates: Requirements 17.6
 */
class InterviewAccessControlTest {

    private static final String ADMIN_LEADER_ONLY = "hasAnyRole('ADMIN', 'LEADER')";
    private static final String READ_ACCESS = "hasAnyRole('ADMIN', 'LEADER', 'VICE_LEADER')";

    // --- Write endpoints: ADMIN/LEADER only ---

    @ParameterizedTest
    @ValueSource(strings = {"startInterview", "processMessage", "endInterview", "manualReview"})
    void writeEndpoints_shouldRequireAdminOrLeaderRole(String methodName) {
        PreAuthorize annotation = findPreAuthorize(methodName);
        assertThat(annotation).as("@PreAuthorize missing on %s", methodName).isNotNull();
        assertThat(annotation.value()).isEqualTo(ADMIN_LEADER_ONLY);
    }

    // --- Read endpoints: ADMIN/LEADER/VICE_LEADER ---

    @ParameterizedTest
    @ValueSource(strings = {"getInterview", "getMessages", "getReport", "getArchive"})
    void readEndpoints_shouldAllowViceLeaderReadAccess(String methodName) {
        PreAuthorize annotation = findPreAuthorize(methodName);
        assertThat(annotation).as("@PreAuthorize missing on %s", methodName).isNotNull();
        assertThat(annotation.value()).isEqualTo(READ_ACCESS);
    }

    // --- All methods should have @PreAuthorize ---

    @Test
    void allPublicMethods_shouldHavePreAuthorizeAnnotation() {
        List<String> endpointMethods = List.of(
                "startInterview", "processMessage", "endInterview",
                "getInterview", "getMessages", "getReport",
                "manualReview", "getArchive"
        );

        for (String methodName : endpointMethods) {
            PreAuthorize annotation = findPreAuthorize(methodName);
            assertThat(annotation)
                    .as("@PreAuthorize annotation missing on method: %s", methodName)
                    .isNotNull();
        }
    }

    // --- VICE_LEADER should NOT have write access ---

    @Test
    void writeEndpoints_shouldNotIncludeViceLeader() {
        List<String> writeMethods = List.of("startInterview", "processMessage", "endInterview", "manualReview");

        for (String methodName : writeMethods) {
            PreAuthorize annotation = findPreAuthorize(methodName);
            assertThat(annotation).isNotNull();
            assertThat(annotation.value())
                    .as("Write method %s should not allow VICE_LEADER", methodName)
                    .doesNotContain("VICE_LEADER");
        }
    }

    // --- Read endpoints should include VICE_LEADER ---

    @Test
    void readEndpoints_shouldIncludeViceLeader() {
        List<String> readMethods = List.of("getInterview", "getMessages", "getReport", "getArchive");

        for (String methodName : readMethods) {
            PreAuthorize annotation = findPreAuthorize(methodName);
            assertThat(annotation).isNotNull();
            assertThat(annotation.value())
                    .as("Read method %s should allow VICE_LEADER", methodName)
                    .contains("VICE_LEADER");
        }
    }

    private PreAuthorize findPreAuthorize(String methodName) {
        Method method = Arrays.stream(InterviewController.class.getDeclaredMethods())
                .filter(m -> m.getName().equals(methodName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Method not found: " + methodName));
        return method.getAnnotation(PreAuthorize.class);
    }
}
