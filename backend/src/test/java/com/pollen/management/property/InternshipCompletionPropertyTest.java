package com.pollen.management.property;

import com.pollen.management.entity.InternshipTask;
import com.pollen.management.entity.enums.InternshipStatus;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Property-based tests for internship task completion rate calculation
 * and conversion trigger logic.
 *
 * Property 40: 实习任务完成率计算与转正触发
 * For any internship record, the task completion rate should equal
 * completedTasks / totalTasks; when the internship expires and
 * completion rate >= 80%, conversion should be triggered.
 *
 * **Validates: Requirements 14.5, 14.6**
 */
class InternshipCompletionPropertyTest {

    // ========================================================================
    // Pure calculation helpers (mirrors InternshipServiceImpl logic)
    // ========================================================================

    /**
     * Calculates task completion rate from a list of tasks.
     * This mirrors the logic in InternshipServiceImpl.getProgress().
     */
    static double calculateCompletionRate(List<InternshipTask> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return 0.0;
        }
        long completed = tasks.stream().filter(InternshipTask::getCompleted).count();
        return (double) completed / tasks.size();
    }

    /**
     * Determines the target status when an internship expires.
     * Per design: expired + rate >= 0.8 → PENDING_CONVERSION,
     *             expired + rate < 0.8 → PENDING_EVALUATION.
     */
    static InternshipStatus determineExpiredStatus(double completionRate) {
        return completionRate >= 0.8
                ? InternshipStatus.PENDING_CONVERSION
                : InternshipStatus.PENDING_EVALUATION;
    }

    /**
     * Checks whether an internship has expired (expected end date is today or in the past).
     */
    static boolean isExpired(LocalDate expectedEndDate) {
        return !expectedEndDate.isAfter(LocalDate.now());
    }

    // ========================================================================
    // Property 40.1: Completion rate equals completedCount / totalCount
    // **Validates: Requirements 14.5, 14.6**
    // ========================================================================

    @Property(tries = 500)
    void property40_completionRateEqualsCompletedOverTotal(
            @ForAll @IntRange(min = 1, max = 50) int totalTasks,
            @ForAll @IntRange(min = 0, max = 50) int completedCount) {

        int actualCompleted = Math.min(completedCount, totalTasks);
        List<InternshipTask> tasks = buildTaskList(totalTasks, actualCompleted);

        double rate = calculateCompletionRate(tasks);
        double expected = (double) actualCompleted / totalTasks;

        assertThat(rate).isCloseTo(expected, within(1e-9));
    }

    // ========================================================================
    // Property 40.2: Completion rate is always in [0.0, 1.0]
    // **Validates: Requirements 14.5, 14.6**
    // ========================================================================

    @Property(tries = 500)
    void property40_completionRateAlwaysInUnitInterval(
            @ForAll @IntRange(min = 0, max = 100) int totalTasks,
            @ForAll @IntRange(min = 0, max = 100) int completedCount) {

        int actualCompleted = Math.min(completedCount, totalTasks);
        List<InternshipTask> tasks = buildTaskList(totalTasks, actualCompleted);

        double rate = calculateCompletionRate(tasks);

        assertThat(rate).isBetween(0.0, 1.0);
    }

    @Example
    void property40_emptyTaskListReturnsZero() {
        List<InternshipTask> emptyTasks = new ArrayList<>();
        assertThat(calculateCompletionRate(emptyTasks)).isEqualTo(0.0);
        assertThat(calculateCompletionRate(null)).isEqualTo(0.0);
    }

    // ========================================================================
    // Property 40.3: When rate >= 0.8 and internship expired, trigger conversion
    // **Validates: Requirements 14.5, 14.6**
    // ========================================================================

    @Property(tries = 500)
    void property40_expiredWithHighCompletionTriggersConversion(
            @ForAll @IntRange(min = 5, max = 50) int totalTasks,
            @ForAll @IntRange(min = 0, max = 50) int completedCount) {

        int actualCompleted = Math.min(completedCount, totalTasks);
        List<InternshipTask> tasks = buildTaskList(totalTasks, actualCompleted);
        double rate = calculateCompletionRate(tasks);

        Assume.that(rate >= 0.8);

        InternshipStatus status = determineExpiredStatus(rate);
        assertThat(status).isEqualTo(InternshipStatus.PENDING_CONVERSION);
    }

    // ========================================================================
    // Property 40.4: When rate < 0.8 and internship expired, trigger evaluation
    // **Validates: Requirements 14.5, 14.6**
    // ========================================================================

    @Property(tries = 500)
    void property40_expiredWithLowCompletionTriggersEvaluation(
            @ForAll @IntRange(min = 1, max = 50) int totalTasks,
            @ForAll @IntRange(min = 0, max = 50) int completedCount) {

        int actualCompleted = Math.min(completedCount, totalTasks);
        List<InternshipTask> tasks = buildTaskList(totalTasks, actualCompleted);
        double rate = calculateCompletionRate(tasks);

        Assume.that(rate < 0.8);

        InternshipStatus status = determineExpiredStatus(rate);
        assertThat(status).isEqualTo(InternshipStatus.PENDING_EVALUATION);
    }

    // ========================================================================
    // Property 40.5: Boundary — exactly 80% triggers conversion
    // **Validates: Requirements 14.5, 14.6**
    // ========================================================================

    @Property(tries = 200)
    void property40_exactlyEightyPercentTriggersConversion(
            @ForAll @IntRange(min = 1, max = 20) int multiplier) {

        // Build tasks where exactly 80% are completed (e.g. 4/5, 8/10, 12/15...)
        int totalTasks = 5 * multiplier;
        int completedCount = 4 * multiplier;
        List<InternshipTask> tasks = buildTaskList(totalTasks, completedCount);

        double rate = calculateCompletionRate(tasks);
        assertThat(rate).isCloseTo(0.8, within(1e-9));

        InternshipStatus status = determineExpiredStatus(rate);
        assertThat(status).isEqualTo(InternshipStatus.PENDING_CONVERSION);
    }

    // ========================================================================
    // Property 40.6: Expiry detection correctness
    // **Validates: Requirements 14.5, 14.6**
    // ========================================================================

    @Property(tries = 200)
    void property40_expiryDetection(
            @ForAll @IntRange(min = -30, max = 30) int daysFromNow) {

        LocalDate endDate = LocalDate.now().plusDays(daysFromNow);
        boolean expired = isExpired(endDate);

        if (daysFromNow <= 0) {
            assertThat(expired).isTrue();
        } else {
            assertThat(expired).isFalse();
        }
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    /**
     * Builds a list of InternshipTask with the given total and completed counts.
     * The first {@code completedCount} tasks are marked as completed.
     */
    private static List<InternshipTask> buildTaskList(int total, int completed) {
        List<InternshipTask> tasks = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            InternshipTask task = InternshipTask.builder()
                    .id((long) (i + 1))
                    .internshipId(1L)
                    .taskName("Task " + (i + 1))
                    .deadline(LocalDate.now().plusDays(30))
                    .completed(i < completed)
                    .completedAt(i < completed ? LocalDateTime.now() : null)
                    .build();
            tasks.add(task);
        }
        return tasks;
    }
}
