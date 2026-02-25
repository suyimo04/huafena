package com.pollen.management.property;

import com.pollen.management.entity.Activity;
import com.pollen.management.entity.ActivityRegistration;
import com.pollen.management.entity.enums.ActivityStatus;
import com.pollen.management.entity.enums.ApprovalMode;
import com.pollen.management.entity.enums.RegistrationStatus;
import com.pollen.management.repository.ActivityFeedbackRepository;
import com.pollen.management.repository.ActivityGroupRepository;
import com.pollen.management.repository.ActivityMaterialRepository;
import com.pollen.management.repository.ActivityRegistrationRepository;
import com.pollen.management.repository.ActivityRepository;
import com.pollen.management.repository.ActivityStatisticsRepository;
import com.pollen.management.service.ActivityServiceImpl;
import com.pollen.management.service.PointsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.jqwik.api.*;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for activity approval mode registration status.
 *
 * Property 34: 活动审核模式报名状态
 * For any activity configured as MANUAL approval mode, new registrations should
 * have status PENDING; for AUTO mode, status should be APPROVED.
 *
 * **Validates: Requirements 11.4, 11.6**
 */
class ActivityApprovalModePropertyTest {

    // ========================================================================
    // Property 34: MANUAL mode → PENDING, AUTO mode → APPROVED
    // **Validates: Requirements 11.4, 11.6**
    // ========================================================================

    @Property(tries = 200)
    void property34_manualApprovalModeResultsInPendingStatus(
            @ForAll("activityIds") Long activityId,
            @ForAll("userIds") Long userId) {

        ActivityRepository activityRepo = mock(ActivityRepository.class);
        ActivityRegistrationRepository regRepo = mock(ActivityRegistrationRepository.class);
        ActivityServiceImpl service = createService(activityRepo, regRepo);

        Activity activity = buildActivity(activityId, ApprovalMode.MANUAL);
        when(activityRepo.findById(activityId)).thenReturn(Optional.of(activity));
        when(regRepo.existsByActivityIdAndUserId(activityId, userId)).thenReturn(false);
        when(regRepo.save(any(ActivityRegistration.class))).thenAnswer(inv -> inv.getArgument(0));

        ActivityRegistration result = service.registerForActivity(activityId, userId);

        assertThat(result.getStatus())
                .as("MANUAL approval mode must produce PENDING registration status")
                .isEqualTo(RegistrationStatus.PENDING);
    }

    @Property(tries = 200)
    void property34_autoApprovalModeResultsInApprovedStatus(
            @ForAll("activityIds") Long activityId,
            @ForAll("userIds") Long userId) {

        ActivityRepository activityRepo = mock(ActivityRepository.class);
        ActivityRegistrationRepository regRepo = mock(ActivityRegistrationRepository.class);
        ActivityServiceImpl service = createService(activityRepo, regRepo);

        Activity activity = buildActivity(activityId, ApprovalMode.AUTO);
        when(activityRepo.findById(activityId)).thenReturn(Optional.of(activity));
        when(activityRepo.save(any(Activity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(regRepo.existsByActivityIdAndUserId(activityId, userId)).thenReturn(false);
        when(regRepo.save(any(ActivityRegistration.class))).thenAnswer(inv -> inv.getArgument(0));

        ActivityRegistration result = service.registerForActivity(activityId, userId);

        assertThat(result.getStatus())
                .as("AUTO approval mode must produce APPROVED registration status")
                .isEqualTo(RegistrationStatus.APPROVED);
    }

    @Property(tries = 200)
    void property34_approvalModeDirectlyDeterminesRegistrationStatus(
            @ForAll("approvalModes") ApprovalMode mode,
            @ForAll("activityIds") Long activityId,
            @ForAll("userIds") Long userId) {

        ActivityRepository activityRepo = mock(ActivityRepository.class);
        ActivityRegistrationRepository regRepo = mock(ActivityRegistrationRepository.class);
        ActivityServiceImpl service = createService(activityRepo, regRepo);

        Activity activity = buildActivity(activityId, mode);
        when(activityRepo.findById(activityId)).thenReturn(Optional.of(activity));
        when(activityRepo.save(any(Activity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(regRepo.existsByActivityIdAndUserId(activityId, userId)).thenReturn(false);
        when(regRepo.save(any(ActivityRegistration.class))).thenAnswer(inv -> inv.getArgument(0));

        ActivityRegistration result = service.registerForActivity(activityId, userId);

        RegistrationStatus expected = (mode == ApprovalMode.MANUAL)
                ? RegistrationStatus.PENDING
                : RegistrationStatus.APPROVED;

        assertThat(result.getStatus())
                .as("ApprovalMode %s must produce %s registration status", mode, expected)
                .isEqualTo(expected);
    }

    @Property(tries = 100)
    void property34_autoModeIncrementsRegistrationCount(
            @ForAll("activityIds") Long activityId,
            @ForAll("userIds") Long userId) {

        ActivityRepository activityRepo = mock(ActivityRepository.class);
        ActivityRegistrationRepository regRepo = mock(ActivityRegistrationRepository.class);
        ActivityServiceImpl service = createService(activityRepo, regRepo);

        Activity activity = buildActivity(activityId, ApprovalMode.AUTO);
        int originalCount = activity.getRegistrationCount();
        when(activityRepo.findById(activityId)).thenReturn(Optional.of(activity));
        when(activityRepo.save(any(Activity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(regRepo.existsByActivityIdAndUserId(activityId, userId)).thenReturn(false);
        when(regRepo.save(any(ActivityRegistration.class))).thenAnswer(inv -> inv.getArgument(0));

        service.registerForActivity(activityId, userId);

        assertThat(activity.getRegistrationCount())
                .as("AUTO mode registration must increment registration count")
                .isEqualTo(originalCount + 1);
    }

    @Property(tries = 100)
    void property34_manualModeDoesNotIncrementRegistrationCount(
            @ForAll("activityIds") Long activityId,
            @ForAll("userIds") Long userId) {

        ActivityRepository activityRepo = mock(ActivityRepository.class);
        ActivityRegistrationRepository regRepo = mock(ActivityRegistrationRepository.class);
        ActivityServiceImpl service = createService(activityRepo, regRepo);

        Activity activity = buildActivity(activityId, ApprovalMode.MANUAL);
        int originalCount = activity.getRegistrationCount();
        when(activityRepo.findById(activityId)).thenReturn(Optional.of(activity));
        when(regRepo.existsByActivityIdAndUserId(activityId, userId)).thenReturn(false);
        when(regRepo.save(any(ActivityRegistration.class))).thenAnswer(inv -> inv.getArgument(0));

        service.registerForActivity(activityId, userId);

        assertThat(activity.getRegistrationCount())
                .as("MANUAL mode registration must NOT increment registration count until approved")
                .isEqualTo(originalCount);
        verify(activityRepo, never()).save(any(Activity.class));
    }

    // ========== Arbitrary Providers ==========

    @Provide
    Arbitrary<Long> activityIds() {
        return Arbitraries.longs().between(1L, 10000L);
    }

    @Provide
    Arbitrary<Long> userIds() {
        return Arbitraries.longs().between(1L, 10000L);
    }

    @Provide
    Arbitrary<ApprovalMode> approvalModes() {
        return Arbitraries.of(ApprovalMode.AUTO, ApprovalMode.MANUAL);
    }

    // ========== Helpers ==========

    private Activity buildActivity(Long id, ApprovalMode approvalMode) {
        return Activity.builder()
                .id(id)
                .name("Test Activity")
                .description("Test Description")
                .activityTime(LocalDateTime.now().plusDays(7))
                .location("Test Location")
                .createdBy(1L)
                .status(ActivityStatus.UPCOMING)
                .registrationCount(0)
                .approvalMode(approvalMode)
                .build();
    }

    private ActivityServiceImpl createService(
            ActivityRepository activityRepo,
            ActivityRegistrationRepository regRepo) {
        return new ActivityServiceImpl(
                activityRepo,
                regRepo,
                mock(ActivityGroupRepository.class),
                mock(ActivityFeedbackRepository.class),
                mock(ActivityStatisticsRepository.class),
                mock(ActivityMaterialRepository.class),
                mock(PointsService.class),
                new ObjectMapper()
        );
    }
}
