package com.pollen.management.property;

import com.pollen.management.entity.Activity;
import com.pollen.management.entity.ActivityStatistics;
import com.pollen.management.entity.enums.ActivityStatus;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for activity statistics data correctness.
 *
 * Property 35: 活动统计数据正确性
 * For any activity, statistics data should have:
 * - total_registered = total registration count
 * - total_attended = checked-in registration count
 * - check_in_rate = total_attended / total_registered (0 when no registrations)
 *
 * **Validates: Requirements 11.11**
 */
class ActivityStatisticsPropertyTest {

    // ========================================================================
    // Property 35: Statistics data correctness
    // **Validates: Requirements 11.11**
    // ========================================================================

    @Property(tries = 200)
    void property35_totalRegisteredEqualsRegistrationCount(
            @ForAll("activityIds") Long activityId,
            @ForAll("registrationCounts") long totalRegistered,
            @ForAll("attendedCounts") long totalAttended) {

        long effectiveAttended = Math.min(totalAttended, totalRegistered);

        ActivityRepository activityRepo = mock(ActivityRepository.class);
        ActivityRegistrationRepository regRepo = mock(ActivityRegistrationRepository.class);
        ActivityFeedbackRepository feedbackRepo = mock(ActivityFeedbackRepository.class);
        ActivityStatisticsRepository statsRepo = mock(ActivityStatisticsRepository.class);
        ActivityServiceImpl service = createService(activityRepo, regRepo, feedbackRepo, statsRepo);

        when(activityRepo.findById(activityId)).thenReturn(Optional.of(buildActivity(activityId)));
        when(regRepo.countByActivityId(activityId)).thenReturn(totalRegistered);
        when(regRepo.countByActivityIdAndCheckedInTrue(activityId)).thenReturn(effectiveAttended);
        when(feedbackRepo.findByActivityId(activityId)).thenReturn(Collections.emptyList());
        when(statsRepo.findByActivityId(activityId))
                .thenReturn(Optional.of(ActivityStatistics.builder().activityId(activityId).build()));
        when(statsRepo.save(any(ActivityStatistics.class))).thenAnswer(inv -> inv.getArgument(0));

        ActivityStatistics stats = service.getStatistics(activityId);

        assertThat(stats.getTotalRegistered())
                .as("total_registered must equal the registration count from repository")
                .isEqualTo((int) totalRegistered);
    }

    @Property(tries = 200)
    void property35_totalAttendedEqualsCheckedInCount(
            @ForAll("activityIds") Long activityId,
            @ForAll("registrationCounts") long totalRegistered,
            @ForAll("attendedCounts") long totalAttended) {

        long effectiveAttended = Math.min(totalAttended, totalRegistered);

        ActivityRepository activityRepo = mock(ActivityRepository.class);
        ActivityRegistrationRepository regRepo = mock(ActivityRegistrationRepository.class);
        ActivityFeedbackRepository feedbackRepo = mock(ActivityFeedbackRepository.class);
        ActivityStatisticsRepository statsRepo = mock(ActivityStatisticsRepository.class);
        ActivityServiceImpl service = createService(activityRepo, regRepo, feedbackRepo, statsRepo);

        when(activityRepo.findById(activityId)).thenReturn(Optional.of(buildActivity(activityId)));
        when(regRepo.countByActivityId(activityId)).thenReturn(totalRegistered);
        when(regRepo.countByActivityIdAndCheckedInTrue(activityId)).thenReturn(effectiveAttended);
        when(feedbackRepo.findByActivityId(activityId)).thenReturn(Collections.emptyList());
        when(statsRepo.findByActivityId(activityId))
                .thenReturn(Optional.of(ActivityStatistics.builder().activityId(activityId).build()));
        when(statsRepo.save(any(ActivityStatistics.class))).thenAnswer(inv -> inv.getArgument(0));

        ActivityStatistics stats = service.getStatistics(activityId);

        assertThat(stats.getTotalAttended())
                .as("total_attended must equal the checked-in count from repository")
                .isEqualTo((int) effectiveAttended);
    }

    @Property(tries = 200)
    void property35_checkInRateIsZeroWhenNoRegistrations(
            @ForAll("activityIds") Long activityId) {

        ActivityRepository activityRepo = mock(ActivityRepository.class);
        ActivityRegistrationRepository regRepo = mock(ActivityRegistrationRepository.class);
        ActivityFeedbackRepository feedbackRepo = mock(ActivityFeedbackRepository.class);
        ActivityStatisticsRepository statsRepo = mock(ActivityStatisticsRepository.class);
        ActivityServiceImpl service = createService(activityRepo, regRepo, feedbackRepo, statsRepo);

        when(activityRepo.findById(activityId)).thenReturn(Optional.of(buildActivity(activityId)));
        when(regRepo.countByActivityId(activityId)).thenReturn(0L);
        when(regRepo.countByActivityIdAndCheckedInTrue(activityId)).thenReturn(0L);
        when(feedbackRepo.findByActivityId(activityId)).thenReturn(Collections.emptyList());
        when(statsRepo.findByActivityId(activityId))
                .thenReturn(Optional.of(ActivityStatistics.builder().activityId(activityId).build()));
        when(statsRepo.save(any(ActivityStatistics.class))).thenAnswer(inv -> inv.getArgument(0));

        ActivityStatistics stats = service.getStatistics(activityId);

        assertThat(stats.getCheckInRate().compareTo(BigDecimal.ZERO))
                .as("check_in_rate must be 0 when there are no registrations")
                .isEqualTo(0);
    }

    @Property(tries = 200)
    void property35_checkInRateEqualsAttendedDividedByRegistered(
            @ForAll("activityIds") Long activityId,
            @ForAll("positiveRegistrationCounts") long totalRegistered,
            @ForAll("attendedCounts") long totalAttended) {

        long effectiveAttended = Math.min(totalAttended, totalRegistered);

        ActivityRepository activityRepo = mock(ActivityRepository.class);
        ActivityRegistrationRepository regRepo = mock(ActivityRegistrationRepository.class);
        ActivityFeedbackRepository feedbackRepo = mock(ActivityFeedbackRepository.class);
        ActivityStatisticsRepository statsRepo = mock(ActivityStatisticsRepository.class);
        ActivityServiceImpl service = createService(activityRepo, regRepo, feedbackRepo, statsRepo);

        when(activityRepo.findById(activityId)).thenReturn(Optional.of(buildActivity(activityId)));
        when(regRepo.countByActivityId(activityId)).thenReturn(totalRegistered);
        when(regRepo.countByActivityIdAndCheckedInTrue(activityId)).thenReturn(effectiveAttended);
        when(feedbackRepo.findByActivityId(activityId)).thenReturn(Collections.emptyList());
        when(statsRepo.findByActivityId(activityId))
                .thenReturn(Optional.of(ActivityStatistics.builder().activityId(activityId).build()));
        when(statsRepo.save(any(ActivityStatistics.class))).thenAnswer(inv -> inv.getArgument(0));

        ActivityStatistics stats = service.getStatistics(activityId);

        BigDecimal expectedRate = BigDecimal.valueOf(effectiveAttended)
                .divide(BigDecimal.valueOf(totalRegistered), 2, RoundingMode.HALF_UP);

        assertThat(stats.getCheckInRate())
                .as("check_in_rate must equal attended/registered with scale 2, HALF_UP rounding")
                .isEqualByComparingTo(expectedRate);
    }

    // ========== Arbitrary Providers ==========

    @Provide
    Arbitrary<Long> activityIds() {
        return Arbitraries.longs().between(1L, 10000L);
    }

    @Provide
    Arbitrary<Long> registrationCounts() {
        return Arbitraries.longs().between(0L, 500L);
    }

    @Provide
    Arbitrary<Long> positiveRegistrationCounts() {
        return Arbitraries.longs().between(1L, 500L);
    }

    @Provide
    Arbitrary<Long> attendedCounts() {
        return Arbitraries.longs().between(0L, 500L);
    }

    // ========== Helpers ==========

    private Activity buildActivity(Long id) {
        return Activity.builder()
                .id(id)
                .name("Test Activity")
                .description("Test Description")
                .activityTime(LocalDateTime.now().plusDays(7))
                .location("Test Location")
                .createdBy(1L)
                .status(ActivityStatus.UPCOMING)
                .registrationCount(0)
                .build();
    }

    private ActivityServiceImpl createService(
            ActivityRepository activityRepo,
            ActivityRegistrationRepository regRepo,
            ActivityFeedbackRepository feedbackRepo,
            ActivityStatisticsRepository statsRepo) {
        return new ActivityServiceImpl(
                activityRepo,
                regRepo,
                mock(ActivityGroupRepository.class),
                feedbackRepo,
                statsRepo,
                mock(ActivityMaterialRepository.class),
                mock(PointsService.class),
                new ObjectMapper()
        );
    }
}
