package com.pollen.management.property;

import com.pollen.management.entity.ApplicationTimeline;
import com.pollen.management.entity.Application;
import com.pollen.management.entity.User;
import com.pollen.management.entity.enums.ApplicationStatus;
import com.pollen.management.entity.enums.EntryType;
import com.pollen.management.entity.enums.Role;
import com.pollen.management.repository.ApplicationRepository;
import com.pollen.management.repository.ApplicationTimelineRepository;
import com.pollen.management.repository.UserRepository;
import com.pollen.management.service.ApplicationServiceImpl;
import com.pollen.management.service.ApplicationTimelineService;
import com.pollen.management.service.ApplicationTimelineServiceImpl;
import net.jqwik.api.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for application timeline completeness.
 *
 * Property 33: 申请流程时间线完整性
 * For any application record, its timeline should contain all status transition
 * nodes from submission to current status, and nodes should be ordered by time ascending.
 *
 * **Validates: Requirements 7.6**
 */
class ApplicationTimelinePropertyTest {

    // ========================================================================
    // Property 33: Timeline entries are always ordered by createdAt ascending
    // For any sequence of timeline events recorded for an application,
    // getTimeline should return them in chronological (ascending) order.
    // **Validates: Requirements 7.6**
    // ========================================================================

    @Property(tries = 200)
    void property33_timelineEntriesAreOrderedByCreatedAtAscending(
            @ForAll("timelineEntryLists") List<ApplicationTimeline> entries) {

        Long applicationId = 1L;

        // Sort entries by createdAt ascending to simulate what the repository query returns
        List<ApplicationTimeline> sortedEntries = entries.stream()
                .sorted(Comparator.comparing(ApplicationTimeline::getCreatedAt))
                .collect(Collectors.toList());

        ApplicationTimelineRepository repo = mock(ApplicationTimelineRepository.class);
        when(repo.findByApplicationIdOrderByCreatedAtAsc(applicationId)).thenReturn(sortedEntries);

        ApplicationTimelineServiceImpl service = new ApplicationTimelineServiceImpl(repo);
        List<ApplicationTimeline> timeline = service.getTimeline(applicationId);

        // Verify ascending order
        for (int i = 1; i < timeline.size(); i++) {
            assertThat(timeline.get(i).getCreatedAt())
                    .as("Timeline entry %d should be >= entry %d in time", i, i - 1)
                    .isAfterOrEqualTo(timeline.get(i - 1).getCreatedAt());
        }
    }

    // ========================================================================
    // Property 33: Every status change produces a corresponding timeline entry
    // When status transitions are applied via initialReview, batchApprove,
    // batchReject, or batchNotifyInterview, a timeline event is recorded
    // for each transition.
    // **Validates: Requirements 7.6**
    // ========================================================================

    @Property(tries = 200)
    void property33_initialReviewRecordsTimelineEntry(
            @ForAll("booleans") boolean approved) {

        // Setup
        ApplicationRepository appRepo = mock(ApplicationRepository.class);
        UserRepository userRepo = mock(UserRepository.class);
        ApplicationTimelineService timelineService = mock(ApplicationTimelineService.class);

        ApplicationServiceImpl service = new ApplicationServiceImpl(
                appRepo, userRepo, null, null, null, null, null, timelineService);

        Long appId = 1L;
        Long userId = 100L;
        Application application = Application.builder()
                .id(appId)
                .userId(userId)
                .status(ApplicationStatus.PENDING_INITIAL_REVIEW)
                .entryType(EntryType.REGISTRATION)
                .build();

        when(appRepo.findById(appId)).thenReturn(Optional.of(application));
        when(appRepo.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        if (approved) {
            User user = User.builder()
                    .id(userId).username("test").password("encoded")
                    .role(Role.APPLICANT).enabled(false).build();
            when(userRepo.findById(userId)).thenReturn(Optional.of(user));
            when(userRepo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        }

        // Execute
        service.initialReview(appId, approved);

        // Verify timeline event was recorded
        String expectedStatus = approved
                ? ApplicationStatus.INITIAL_REVIEW_PASSED.name()
                : ApplicationStatus.REJECTED.name();

        verify(timelineService).recordTimelineEvent(
                eq(appId),
                eq(expectedStatus),
                any(String.class),
                any(String.class));
    }

    @Property(tries = 200)
    void property33_batchApproveRecordsTimelineForEachPendingApplication(
            @ForAll("pendingApplicationCounts") int count) {

        ApplicationRepository appRepo = mock(ApplicationRepository.class);
        UserRepository userRepo = mock(UserRepository.class);
        ApplicationTimelineService timelineService = mock(ApplicationTimelineService.class);

        ApplicationServiceImpl service = new ApplicationServiceImpl(
                appRepo, userRepo, null, null, null, null, null, timelineService);

        List<Application> applications = new ArrayList<>();
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Long id = (long) (i + 1);
            Long userId = (long) (i + 100);
            ids.add(id);
            applications.add(Application.builder()
                    .id(id).userId(userId)
                    .status(ApplicationStatus.PENDING_INITIAL_REVIEW)
                    .entryType(EntryType.REGISTRATION).build());

            User user = User.builder()
                    .id(userId).username("user_" + userId).password("encoded")
                    .role(Role.APPLICANT).enabled(false).build();
            when(userRepo.findById(userId)).thenReturn(Optional.of(user));
            when(userRepo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        }

        when(appRepo.findAllById(ids)).thenReturn(new ArrayList<>(applications));
        when(appRepo.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        service.batchApprove(ids);

        // Each pending application should have a timeline event recorded
        verify(timelineService, times(count)).recordTimelineEvent(
                any(Long.class),
                eq(ApplicationStatus.INITIAL_REVIEW_PASSED.name()),
                any(String.class),
                any(String.class));
    }

    @Property(tries = 200)
    void property33_batchRejectRecordsTimelineForEachPendingApplication(
            @ForAll("pendingApplicationCounts") int count) {

        ApplicationRepository appRepo = mock(ApplicationRepository.class);
        UserRepository userRepo = mock(UserRepository.class);
        ApplicationTimelineService timelineService = mock(ApplicationTimelineService.class);

        ApplicationServiceImpl service = new ApplicationServiceImpl(
                appRepo, userRepo, null, null, null, null, null, timelineService);

        List<Application> applications = new ArrayList<>();
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Long id = (long) (i + 1);
            ids.add(id);
            applications.add(Application.builder()
                    .id(id).userId((long) (i + 100))
                    .status(ApplicationStatus.PENDING_INITIAL_REVIEW)
                    .entryType(EntryType.REGISTRATION).build());
        }

        when(appRepo.findAllById(ids)).thenReturn(new ArrayList<>(applications));
        when(appRepo.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        service.batchReject(ids);

        verify(timelineService, times(count)).recordTimelineEvent(
                any(Long.class),
                eq(ApplicationStatus.REJECTED.name()),
                any(String.class),
                any(String.class));
    }

    @Property(tries = 200)
    void property33_batchNotifyInterviewRecordsTimelineForEachPassedApplication(
            @ForAll("pendingApplicationCounts") int count) {

        ApplicationRepository appRepo = mock(ApplicationRepository.class);
        UserRepository userRepo = mock(UserRepository.class);
        ApplicationTimelineService timelineService = mock(ApplicationTimelineService.class);

        ApplicationServiceImpl service = new ApplicationServiceImpl(
                appRepo, userRepo, null, null, null, null, null, timelineService);

        List<Application> applications = new ArrayList<>();
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Long id = (long) (i + 1);
            ids.add(id);
            applications.add(Application.builder()
                    .id(id).userId((long) (i + 100))
                    .status(ApplicationStatus.INITIAL_REVIEW_PASSED)
                    .entryType(EntryType.REGISTRATION).build());
        }

        when(appRepo.findAllById(ids)).thenReturn(new ArrayList<>(applications));
        when(appRepo.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        service.batchNotifyInterview(ids);

        verify(timelineService, times(count)).recordTimelineEvent(
                any(Long.class),
                eq(ApplicationStatus.AI_INTERVIEW_IN_PROGRESS.name()),
                any(String.class),
                any(String.class));
    }

    // ========================================================================
    // Property 33: Timeline contains at least one entry for any application
    // that has gone through creation. After recording events, the timeline
    // should never be empty.
    // **Validates: Requirements 7.6**
    // ========================================================================

    @Property(tries = 200)
    void property33_timelineContainsAtLeastOneEntryAfterRecording(
            @ForAll("statusSequences") List<String> statuses) {

        Long applicationId = 42L;
        List<ApplicationTimeline> storedEntries = new ArrayList<>();
        AtomicLong idGen = new AtomicLong(1);

        ApplicationTimelineRepository repo = mock(ApplicationTimelineRepository.class);
        when(repo.save(any(ApplicationTimeline.class))).thenAnswer(inv -> {
            ApplicationTimeline entry = inv.getArgument(0);
            entry.setId(idGen.getAndIncrement());
            entry.setCreatedAt(LocalDateTime.now());
            storedEntries.add(entry);
            return entry;
        });
        when(repo.findByApplicationIdOrderByCreatedAtAsc(applicationId)).thenAnswer(inv ->
                storedEntries.stream()
                        .sorted(Comparator.comparing(ApplicationTimeline::getCreatedAt))
                        .collect(Collectors.toList()));

        ApplicationTimelineServiceImpl service = new ApplicationTimelineServiceImpl(repo);

        // Record events for each status in the sequence
        for (String status : statuses) {
            service.recordTimelineEvent(applicationId, status, "系统", "状态变更为 " + status);
        }

        List<ApplicationTimeline> timeline = service.getTimeline(applicationId);

        // Timeline should have exactly as many entries as statuses recorded
        assertThat(timeline)
                .as("Timeline should contain %d entries after recording %d events", statuses.size(), statuses.size())
                .hasSize(statuses.size());

        // Timeline should not be empty (at least one entry)
        assertThat(timeline)
                .as("Timeline must not be empty after recording events")
                .isNotEmpty();
    }

    @Property(tries = 200)
    void property33_timelinePreservesAllStatusTransitions(
            @ForAll("statusSequences") List<String> statuses) {

        Long applicationId = 7L;
        List<ApplicationTimeline> storedEntries = new ArrayList<>();
        AtomicLong idGen = new AtomicLong(1);
        LocalDateTime baseTime = LocalDateTime.of(2024, 1, 1, 10, 0, 0);

        ApplicationTimelineRepository repo = mock(ApplicationTimelineRepository.class);
        when(repo.save(any(ApplicationTimeline.class))).thenAnswer(inv -> {
            ApplicationTimeline entry = inv.getArgument(0);
            entry.setId(idGen.getAndIncrement());
            entry.setCreatedAt(baseTime.plusMinutes(storedEntries.size()));
            storedEntries.add(entry);
            return entry;
        });
        when(repo.findByApplicationIdOrderByCreatedAtAsc(applicationId)).thenAnswer(inv ->
                storedEntries.stream()
                        .sorted(Comparator.comparing(ApplicationTimeline::getCreatedAt))
                        .collect(Collectors.toList()));

        ApplicationTimelineServiceImpl service = new ApplicationTimelineServiceImpl(repo);

        for (String status : statuses) {
            service.recordTimelineEvent(applicationId, status, "操作人", "描述");
        }

        List<ApplicationTimeline> timeline = service.getTimeline(applicationId);

        // All recorded statuses should appear in the timeline in order
        List<String> timelineStatuses = timeline.stream()
                .map(ApplicationTimeline::getStatus)
                .collect(Collectors.toList());

        assertThat(timelineStatuses)
                .as("Timeline statuses should match the recorded status sequence")
                .isEqualTo(statuses);
    }

    // ========== Arbitrary Providers ==========

    @Provide
    Arbitrary<List<ApplicationTimeline>> timelineEntryLists() {
        return Arbitraries.integers().between(1, 15)
                .flatMap(size -> {
                    Arbitrary<ApplicationTimeline> entryArb = Arbitraries.of(ApplicationStatus.values())
                            .flatMap(status -> Arbitraries.integers().between(0, 10000)
                                    .map(minuteOffset -> {
                                        LocalDateTime time = LocalDateTime.of(2024, 1, 1, 0, 0)
                                                .plusMinutes(minuteOffset);
                                        ApplicationTimeline entry = ApplicationTimeline.builder()
                                                .id((long) minuteOffset)
                                                .applicationId(1L)
                                                .status(status.name())
                                                .operator("系统")
                                                .description("状态变更")
                                                .build();
                                        entry.setCreatedAt(time);
                                        return entry;
                                    }));
                    return entryArb.list().ofSize(size);
                });
    }

    @Provide
    Arbitrary<Boolean> booleans() {
        return Arbitraries.of(true, false);
    }

    @Provide
    Arbitrary<Integer> pendingApplicationCounts() {
        return Arbitraries.integers().between(1, 10);
    }

    @Provide
    Arbitrary<List<String>> statusSequences() {
        Arbitrary<String> statusArb = Arbitraries.of(
                ApplicationStatus.PENDING_INITIAL_REVIEW.name(),
                ApplicationStatus.INITIAL_REVIEW_PASSED.name(),
                ApplicationStatus.REJECTED.name(),
                ApplicationStatus.AUTO_REJECTED.name(),
                ApplicationStatus.AI_INTERVIEW_IN_PROGRESS.name(),
                ApplicationStatus.PENDING_REVIEW.name(),
                ApplicationStatus.INTERN_OFFERED.name()
        );
        return statusArb.list().ofMinSize(1).ofMaxSize(8);
    }
}
