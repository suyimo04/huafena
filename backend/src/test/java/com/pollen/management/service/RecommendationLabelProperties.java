package com.pollen.management.service;

import net.jqwik.api.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Feature: pollen-group-management, Property 17: 评分推荐标签正确性
 * **Validates: Requirements 5.8, 5.9, 5.10**
 *
 * Property 17: For any AI interview score (0-10), the recommendation label is deterministic
 * and matches the expected mapping:
 * - totalScore >= 8 → "建议通过"
 * - totalScore in [6, 7] → "重点审查对话内容"
 * - totalScore <= 5 → "建议拒绝"
 */
class RecommendationLabelProperties {

    private final InterviewServiceImpl service = new InterviewServiceImpl(null, null, null, null, null, null, null, null);

    /**
     * Property 17a: Scores >= 8 always produce "建议通过".
     */
    @Property(tries = 100)
    void highScoresAlwaysRecommendPass(@ForAll("highScores") int score) {
        String label = service.getRecommendationLabel(score);
        assertThat(label)
                .as("Score %d (>= 8) must produce '建议通过'", score)
                .isEqualTo("建议通过");
    }

    /**
     * Property 17b: Scores in [6, 7] always produce "重点审查对话内容".
     */
    @Property(tries = 100)
    void midScoresAlwaysRecommendReview(@ForAll("midScores") int score) {
        String label = service.getRecommendationLabel(score);
        assertThat(label)
                .as("Score %d (6-7) must produce '重点审查对话内容'", score)
                .isEqualTo("重点审查对话内容");
    }

    /**
     * Property 17c: Scores <= 5 always produce "建议拒绝".
     */
    @Property(tries = 100)
    void lowScoresAlwaysRecommendReject(@ForAll("lowScores") int score) {
        String label = service.getRecommendationLabel(score);
        assertThat(label)
                .as("Score %d (<= 5) must produce '建议拒绝'", score)
                .isEqualTo("建议拒绝");
    }

    /**
     * Property 17d: For any score in [0, 10], the label is deterministic —
     * calling getRecommendationLabel twice with the same score always returns the same result.
     */
    @Property(tries = 100)
    void labelIsDeterministicForAnyValidScore(@ForAll("validScores") int score) {
        String first = service.getRecommendationLabel(score);
        String second = service.getRecommendationLabel(score);
        assertThat(first)
                .as("Label must be deterministic for score %d", score)
                .isEqualTo(second);
    }

    /**
     * Property 17e: For any score in [0, 10], the label is always one of the three valid values.
     */
    @Property(tries = 100)
    void labelIsAlwaysOneOfThreeValidValues(@ForAll("validScores") int score) {
        String label = service.getRecommendationLabel(score);
        assertThat(label)
                .as("Label for score %d must be one of the three valid labels", score)
                .isIn("建议通过", "重点审查对话内容", "建议拒绝");
    }

    // ========== Providers ==========

    @Provide
    Arbitrary<Integer> highScores() {
        return Arbitraries.integers().between(8, 10);
    }

    @Provide
    Arbitrary<Integer> midScores() {
        return Arbitraries.integers().between(6, 7);
    }

    @Provide
    Arbitrary<Integer> lowScores() {
        return Arbitraries.integers().between(0, 5);
    }

    @Provide
    Arbitrary<Integer> validScores() {
        return Arbitraries.integers().between(0, 10);
    }
}
