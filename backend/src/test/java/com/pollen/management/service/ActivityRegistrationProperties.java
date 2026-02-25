package com.pollen.management.service;

import com.pollen.management.entity.Activity;
import com.pollen.management.entity.ActivityRegistration;
import com.pollen.management.entity.enums.ActivityStatus;
import com.pollen.management.repository.ActivityRegistrationRepository;
import com.pollen.management.repository.ActivityRepository;
import com.pollen.management.util.BusinessException;
import net.jqwik.api.*;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Feature: pollen-group-management, Property 23: 活动重复报名拒绝
 * Feature: pollen-group-management, Property 24: 未报名成员签到拒绝
 * **Validates: Requirements 9.5, 9.6**
 */
class ActivityRegistrationProperties {

    private ActivityServiceImpl createService(ActivityRepository activityRepo,
                                              ActivityRegistrationRepository registrationRepo) {
        PointsService pointsService = Mockito.mock(PointsService.class);
        return new ActivityServiceImpl(activityRepo, registrationRepo, pointsService);
    }

    private Activity buildActivity(Long activityId, ActivityStatus status) {
        return Activity.builder()
                .id(activityId)
                .name("活动_" + activityId)
                .description("描述")
                .activityTime(LocalDateTime.now().plusDays(1))
                .location("地点")
                .status(status)
                .registrationCount(1)
                .createdBy(999L)
                .build();
    }

    // ========== Property 23: 活动重复报名拒绝 ==========

    /**
     * For any member who has already registered for an activity, attempting to
     * register again should be rejected with BusinessException code 409.
     */
    @Property(tries = 100)
    void duplicateRegistrationIsRejected(
            @ForAll("activityIds") Long activityId,
            @ForAll("userIds") Long userId,
            @ForAll("nonArchivedStatuses") ActivityStatus status) {

        ActivityRepository activityRepo = Mockito.mock(ActivityRepository.class);
        ActivityRegistrationRepository registrationRepo = Mockito.mock(ActivityRegistrationRepository.class);

        Activity activity = buildActivity(activityId, status);
        when(activityRepo.findById(activityId)).thenReturn(Optional.of(activity));

        // Simulate that the user has already registered
        when(registrationRepo.existsByActivityIdAndUserId(activityId, userId)).thenReturn(true);

        ActivityServiceImpl service = createService(activityRepo, registrationRepo);

        assertThatThrownBy(() -> service.registerForActivity(activityId, userId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException bex = (BusinessException) ex;
                    assertThat(bex.getCode()).isEqualTo(409);
                });
    }

    // ========== Property 24: 未报名成员签到拒绝 ==========

    /**
     * For any member who has NOT registered for an activity, attempting to check in
     * should be rejected with BusinessException code 403.
     */
    @Property(tries = 100)
    void unregisteredMemberCheckInIsRejected(
            @ForAll("activityIds") Long activityId,
            @ForAll("userIds") Long userId) {

        ActivityRepository activityRepo = Mockito.mock(ActivityRepository.class);
        ActivityRegistrationRepository registrationRepo = Mockito.mock(ActivityRegistrationRepository.class);

        Activity activity = buildActivity(activityId, ActivityStatus.ONGOING);
        when(activityRepo.findById(activityId)).thenReturn(Optional.of(activity));

        // Simulate that the user has NOT registered
        when(registrationRepo.findByActivityIdAndUserId(activityId, userId)).thenReturn(Optional.empty());

        ActivityServiceImpl service = createService(activityRepo, registrationRepo);

        assertThatThrownBy(() -> service.checkIn(activityId, userId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException bex = (BusinessException) ex;
                    assertThat(bex.getCode()).isEqualTo(403);
                });
    }

    // ========== Providers ==========

    @Provide
    Arbitrary<Long> activityIds() {
        return Arbitraries.longs().between(1, 1000);
    }

    @Provide
    Arbitrary<Long> userIds() {
        return Arbitraries.longs().between(1, 1000);
    }

    @Provide
    Arbitrary<ActivityStatus> nonArchivedStatuses() {
        return Arbitraries.of(ActivityStatus.UPCOMING, ActivityStatus.ONGOING);
    }
}
