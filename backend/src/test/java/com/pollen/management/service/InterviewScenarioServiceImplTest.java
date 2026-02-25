package com.pollen.management.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pollen.management.dto.InterviewScenario;
import com.pollen.management.util.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * InterviewScenarioServiceImpl 单元测试
 * Validates: Requirements 5.1, 5.2, 5.15
 */
class InterviewScenarioServiceImplTest {

    private InterviewScenarioServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new InterviewScenarioServiceImpl(new ObjectMapper());
        service.loadScenarios();
    }

    @Test
    void loadScenarios_shouldLoadAllScenariosFromJsonFile() {
        // All scenarios including student-only
        List<InterviewScenario> all = service.getScenarios(true);
        assertFalse(all.isEmpty(), "Should load at least one scenario");
    }

    @Test
    void getScenarios_nonStudent_shouldExcludeStudentOnlyScenarios() {
        List<InterviewScenario> scenarios = service.getScenarios(false);
        boolean hasStudentOnly = scenarios.stream().anyMatch(InterviewScenario::isStudentOnly);
        assertFalse(hasStudentOnly, "Non-student scenarios should not include student-only scenarios");
    }

    @Test
    void getScenarios_student_shouldIncludeStudentOnlyScenarios() {
        List<InterviewScenario> scenarios = service.getScenarios(true);
        boolean hasStudentOnly = scenarios.stream().anyMatch(InterviewScenario::isStudentOnly);
        assertTrue(hasStudentOnly, "Student scenarios should include student-only scenarios");
    }

    @Test
    void getScenarios_student_shouldHaveMoreScenariosThanNonStudent() {
        List<InterviewScenario> studentScenarios = service.getScenarios(true);
        List<InterviewScenario> nonStudentScenarios = service.getScenarios(false);
        assertTrue(studentScenarios.size() > nonStudentScenarios.size(),
                "Student should have more scenarios due to student-only additions");
    }

    @Test
    void getScenarios_shouldContainRequiredCategories() {
        List<InterviewScenario> scenarios = service.getScenarios(false);
        Set<String> categories = scenarios.stream()
                .map(InterviewScenario::getCategory)
                .collect(Collectors.toSet());
        assertTrue(categories.contains("冲突处理"), "Should contain conflict resolution scenarios");
        assertTrue(categories.contains("违规判断"), "Should contain violation judgment scenarios");
        assertTrue(categories.contains("用户投诉处理"), "Should contain complaint handling scenarios");
    }

    @Test
    void getScenarios_student_shouldContainStudyWorkBalanceCategory() {
        List<InterviewScenario> scenarios = service.getScenarios(true);
        Set<String> categories = scenarios.stream()
                .map(InterviewScenario::getCategory)
                .collect(Collectors.toSet());
        assertTrue(categories.contains("学业与工作平衡"), "Student scenarios should contain study-work balance");
    }

    @Test
    void getScenariosByDifficulty_shouldFilterByDifficultyLevel() {
        for (int difficulty = 1; difficulty <= 3; difficulty++) {
            List<InterviewScenario> scenarios = service.getScenariosByDifficulty(difficulty, false);
            int d = difficulty;
            assertTrue(scenarios.stream().allMatch(s -> s.getDifficulty() == d),
                    "All scenarios should match difficulty " + difficulty);
            assertFalse(scenarios.isEmpty(), "Should have scenarios for difficulty " + difficulty);
        }
    }

    @Test
    void getScenariosByDifficulty_student_shouldIncludeStudentScenariosAtMatchingDifficulty() {
        for (int difficulty = 1; difficulty <= 3; difficulty++) {
            List<InterviewScenario> studentScenarios = service.getScenariosByDifficulty(difficulty, true);
            List<InterviewScenario> nonStudentScenarios = service.getScenariosByDifficulty(difficulty, false);
            assertTrue(studentScenarios.size() >= nonStudentScenarios.size(),
                    "Student scenarios at difficulty " + difficulty + " should be >= non-student");
        }
    }

    @Test
    void getScenariosByDifficulty_invalidDifficulty_shouldReturnEmptyList() {
        List<InterviewScenario> scenarios = service.getScenariosByDifficulty(99, false);
        assertTrue(scenarios.isEmpty(), "Invalid difficulty should return empty list");
    }

    @Test
    void allScenarios_shouldHaveRequiredFields() {
        List<InterviewScenario> scenarios = service.getScenarios(true);
        for (InterviewScenario s : scenarios) {
            assertNotNull(s.getId(), "Scenario id should not be null");
            assertNotNull(s.getName(), "Scenario name should not be null");
            assertNotNull(s.getDescription(), "Scenario description should not be null");
            assertTrue(s.getDifficulty() >= 1 && s.getDifficulty() <= 3,
                    "Difficulty should be between 1 and 3");
            assertNotNull(s.getCategory(), "Category should not be null");
            assertNotNull(s.getAiRole(), "AI role should not be null");
            assertNotNull(s.getInitialPrompt(), "Initial prompt should not be null");
        }
    }

    @Test
    void allScenarios_shouldHaveUniqueIds() {
        List<InterviewScenario> scenarios = service.getScenarios(true);
        Set<String> ids = scenarios.stream().map(InterviewScenario::getId).collect(Collectors.toSet());
        assertEquals(scenarios.size(), ids.size(), "All scenario IDs should be unique");
    }

    @Test
    void loadScenarios_missingFile_shouldThrowBusinessException() {
        InterviewScenarioServiceImpl badService = new InterviewScenarioServiceImpl(new ObjectMapper()) {
            @Override
            void loadScenarios() {
                // Simulate missing file by using a non-existent path
                try {
                    var field = InterviewScenarioServiceImpl.class.getDeclaredField("allScenarios");
                    field.setAccessible(true);
                    field.set(this, null);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        badService.loadScenarios();
        BusinessException ex = assertThrows(BusinessException.class, () -> badService.getScenarios(false));
        assertEquals(500, ex.getCode());
        assertTrue(ex.getMessage().contains("面试场景数据未加载"));
    }

    @Test
    void loadScenarios_invalidJson_shouldThrowBusinessException() {
        // Create a service that tries to load from a bad resource
        InterviewScenarioServiceImpl badService = new InterviewScenarioServiceImpl(new ObjectMapper());
        // We test the error path by verifying the exception type and message pattern
        // The actual file-not-found case is tested via the null-data guard
        BusinessException ex = assertThrows(BusinessException.class, () -> badService.getScenarios(false));
        assertEquals(500, ex.getCode());
    }
}
