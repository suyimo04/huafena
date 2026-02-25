package com.pollen.management.entity;

import com.pollen.management.entity.enums.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ActivityEnhancedEntityTest {

    // --- Activity extended fields ---

    @Test
    void activityBuilderShouldDefaultApprovalModeToAuto() {
        Activity activity = Activity.builder()
                .name("Test")
                .createdBy(1L)
                .build();

        assertEquals(ApprovalMode.AUTO, activity.getApprovalMode());
        assertNull(activity.getCoverImageUrl());
        assertNull(activity.getActivityType());
        assertNull(activity.getCustomFormFields());
    }

    @Test
    void activityBuilderShouldAcceptAllNewFields() {
        Activity activity = Activity.builder()
                .name("Team Building")
                .coverImageUrl("https://example.com/cover.jpg")
                .activityType(ActivityType.TEAM_BUILDING)
                .customFormFields("{\"fields\":[\"tshirt_size\"]}")
                .approvalMode(ApprovalMode.MANUAL)
                .createdBy(1L)
                .build();

        assertEquals("https://example.com/cover.jpg", activity.getCoverImageUrl());
        assertEquals(ActivityType.TEAM_BUILDING, activity.getActivityType());
        assertEquals("{\"fields\":[\"tshirt_size\"]}", activity.getCustomFormFields());
        assertEquals(ApprovalMode.MANUAL, activity.getApprovalMode());
    }

    // --- ActivityRegistration extended fields ---

    @Test
    void registrationBuilderShouldDefaultStatusToApproved() {
        ActivityRegistration reg = ActivityRegistration.builder()
                .activityId(1L)
                .userId(2L)
                .build();

        assertEquals(RegistrationStatus.APPROVED, reg.getStatus());
        assertNull(reg.getExtraFields());
    }

    @Test
    void registrationBuilderShouldAcceptStatusAndExtraFields() {
        ActivityRegistration reg = ActivityRegistration.builder()
                .activityId(1L)
                .userId(2L)
                .status(RegistrationStatus.PENDING)
                .extraFields("{\"dietary\":\"vegetarian\"}")
                .build();

        assertEquals(RegistrationStatus.PENDING, reg.getStatus());
        assertEquals("{\"dietary\":\"vegetarian\"}", reg.getExtraFields());
    }

    // --- ActivityGroup ---

    @Test
    void activityGroupBuilderShouldCreateWithRequiredFields() {
        ActivityGroup group = ActivityGroup.builder()
                .activityId(1L)
                .groupName("Group A")
                .memberIds("[1,2,3]")
                .build();

        assertNull(group.getId());
        assertEquals(1L, group.getActivityId());
        assertEquals("Group A", group.getGroupName());
        assertEquals("[1,2,3]", group.getMemberIds());
    }

    // --- ActivityFeedback ---

    @Test
    void activityFeedbackBuilderShouldCreateWithRequiredFields() {
        ActivityFeedback feedback = ActivityFeedback.builder()
                .activityId(1L)
                .userId(2L)
                .rating(5)
                .comment("Great event!")
                .build();

        assertNull(feedback.getId());
        assertEquals(1L, feedback.getActivityId());
        assertEquals(2L, feedback.getUserId());
        assertEquals(5, feedback.getRating());
        assertEquals("Great event!", feedback.getComment());
    }

    // --- ActivityMaterial ---

    @Test
    void activityMaterialBuilderShouldCreateWithRequiredFields() {
        ActivityMaterial material = ActivityMaterial.builder()
                .activityId(1L)
                .fileName("photo.jpg")
                .fileUrl("https://example.com/photo.jpg")
                .fileType("image/jpeg")
                .uploadedBy(3L)
                .build();

        assertNull(material.getId());
        assertEquals(1L, material.getActivityId());
        assertEquals("photo.jpg", material.getFileName());
        assertEquals("https://example.com/photo.jpg", material.getFileUrl());
        assertEquals("image/jpeg", material.getFileType());
        assertEquals(3L, material.getUploadedBy());
    }

    // --- ActivityStatistics ---

    @Test
    void activityStatisticsBuilderShouldCreateWithDefaults() {
        ActivityStatistics stats = ActivityStatistics.builder()
                .activityId(1L)
                .build();

        assertNull(stats.getId());
        assertEquals(1L, stats.getActivityId());
        assertEquals(0, stats.getTotalRegistered());
        assertEquals(0, stats.getTotalAttended());
        assertEquals(BigDecimal.ZERO, stats.getCheckInRate());
        assertEquals(BigDecimal.ZERO, stats.getAvgFeedbackRating());
    }

    @Test
    void activityStatisticsBuilderShouldAcceptAllFields() {
        ActivityStatistics stats = ActivityStatistics.builder()
                .activityId(1L)
                .totalRegistered(20)
                .totalAttended(15)
                .checkInRate(new BigDecimal("75.00"))
                .avgFeedbackRating(new BigDecimal("4.50"))
                .feedbackSummary("{\"positive\":10,\"negative\":2}")
                .build();

        assertEquals(20, stats.getTotalRegistered());
        assertEquals(15, stats.getTotalAttended());
        assertEquals(new BigDecimal("75.00"), stats.getCheckInRate());
        assertEquals(new BigDecimal("4.50"), stats.getAvgFeedbackRating());
        assertEquals("{\"positive\":10,\"negative\":2}", stats.getFeedbackSummary());
    }

    // --- Enum tests ---

    @Test
    void activityTypeEnumShouldContainAllValues() {
        ActivityType[] values = ActivityType.values();
        assertEquals(5, values.length);
        assertNotNull(ActivityType.valueOf("ONLINE"));
        assertNotNull(ActivityType.valueOf("OFFLINE"));
        assertNotNull(ActivityType.valueOf("TRAINING"));
        assertNotNull(ActivityType.valueOf("TEAM_BUILDING"));
        assertNotNull(ActivityType.valueOf("OTHER"));
    }

    @Test
    void approvalModeEnumShouldContainAllValues() {
        ApprovalMode[] values = ApprovalMode.values();
        assertEquals(2, values.length);
        assertNotNull(ApprovalMode.valueOf("AUTO"));
        assertNotNull(ApprovalMode.valueOf("MANUAL"));
    }

    @Test
    void registrationStatusEnumShouldContainAllValues() {
        RegistrationStatus[] values = RegistrationStatus.values();
        assertEquals(3, values.length);
        assertNotNull(RegistrationStatus.valueOf("PENDING"));
        assertNotNull(RegistrationStatus.valueOf("APPROVED"));
        assertNotNull(RegistrationStatus.valueOf("REJECTED"));
    }
}
