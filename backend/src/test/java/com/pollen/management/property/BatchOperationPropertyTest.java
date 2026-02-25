package com.pollen.management.property;

import com.pollen.management.entity.Application;
import com.pollen.management.entity.User;
import com.pollen.management.entity.enums.ApplicationStatus;
import com.pollen.management.entity.enums.EntryType;
import com.pollen.management.entity.enums.Role;
import com.pollen.management.repository.ApplicationRepository;
import com.pollen.management.repository.UserRepository;
import com.pollen.management.service.ApplicationServiceImpl;
import com.pollen.management.service.ApplicationTimelineService;
import net.jqwik.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for batch approve and batch reject operations.
 *
 * Property 31: 批量操作状态一致性
 * For any batch approve or batch reject operation with a list of application IDs,
 * after the operation completes, all selected applications that were in
 * PENDING_INITIAL_REVIEW status should have their status consistently updated
 * to the target status (INITIAL_REVIEW_PASSED for approve, REJECTED for reject).
 * Applications not in PENDING_INITIAL_REVIEW are skipped.
 *
 * **Validates: Requirements 7.2, 7.3**
 */
class BatchOperationPropertyTest {

    // ========================================================================
    // Property 31: 批量操作状态一致性 — batchApprove
    // All PENDING_INITIAL_REVIEW applications → INITIAL_REVIEW_PASSED;
    // non-PENDING_INITIAL_REVIEW applications are skipped.
    // **Validates: Requirements 7.2, 7.3**
    // ========================================================================

    @Property(tries = 200)
    void property31_batchApproveUpdatesAllPendingToInitialReviewPassed(
            @ForAll("applicationListsWithMixedStatuses") List<Application> applications) {

        // Setup mocks
        ApplicationRepository appRepo = mock(ApplicationRepository.class);
        UserRepository userRepo = mock(UserRepository.class);
        ApplicationServiceImpl service = createService(appRepo, userRepo);

        List<Long> ids = applications.stream().map(Application::getId).collect(Collectors.toList());
        when(appRepo.findAllById(ids)).thenReturn(new ArrayList<>(applications));
        when(appRepo.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        // For each PENDING application, mock user lookup
        for (Application app : applications) {
            if (app.getStatus() == ApplicationStatus.PENDING_INITIAL_REVIEW) {
                User user = User.builder()
                        .id(app.getUserId())
                        .username("user_" + app.getUserId())
                        .password("encoded")
                        .role(Role.APPLICANT)
                        .enabled(false)
                        .build();
                when(userRepo.findById(app.getUserId())).thenReturn(Optional.of(user));
                when(userRepo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            }
        }

        // Execute
        service.batchApprove(ids);

        // Verify: all PENDING_INITIAL_REVIEW applications should now be INITIAL_REVIEW_PASSED
        long pendingCount = applications.stream()
                .filter(a -> a.getStatus() == ApplicationStatus.INITIAL_REVIEW_PASSED)
                .count();
        long originalPendingCount = pendingCount;

        // Capture all saved applications
        ArgumentCaptor<Application> captor = ArgumentCaptor.forClass(Application.class);
        verify(appRepo, atLeast(0)).save(captor.capture());

        List<Application> savedApps = captor.getAllValues();
        for (Application saved : savedApps) {
            // Every saved application that was originally PENDING should now be INITIAL_REVIEW_PASSED
            assertThat(saved.getStatus())
                    .as("Batch-approved application %d must be INITIAL_REVIEW_PASSED", saved.getId())
                    .isEqualTo(ApplicationStatus.INITIAL_REVIEW_PASSED);
        }
    }

    @Property(tries = 200)
    void property31_batchRejectUpdatesAllPendingToRejected(
            @ForAll("applicationListsWithMixedStatuses") List<Application> applications) {

        // Setup mocks
        ApplicationRepository appRepo = mock(ApplicationRepository.class);
        UserRepository userRepo = mock(UserRepository.class);
        ApplicationServiceImpl service = createService(appRepo, userRepo);

        List<Long> ids = applications.stream().map(Application::getId).collect(Collectors.toList());
        when(appRepo.findAllById(ids)).thenReturn(new ArrayList<>(applications));
        when(appRepo.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        // Execute
        service.batchReject(ids);

        // Capture all saved applications
        ArgumentCaptor<Application> captor = ArgumentCaptor.forClass(Application.class);
        verify(appRepo, atLeast(0)).save(captor.capture());

        List<Application> savedApps = captor.getAllValues();
        for (Application saved : savedApps) {
            assertThat(saved.getStatus())
                    .as("Batch-rejected application %d must be REJECTED", saved.getId())
                    .isEqualTo(ApplicationStatus.REJECTED);
        }
    }

    @Property(tries = 200)
    void property31_batchApproveSkipsNonPendingApplications(
            @ForAll("nonPendingApplicationLists") List<Application> applications) {

        ApplicationRepository appRepo = mock(ApplicationRepository.class);
        UserRepository userRepo = mock(UserRepository.class);
        ApplicationServiceImpl service = createService(appRepo, userRepo);

        List<Long> ids = applications.stream().map(Application::getId).collect(Collectors.toList());
        when(appRepo.findAllById(ids)).thenReturn(new ArrayList<>(applications));

        // Record original statuses
        Map<Long, ApplicationStatus> originalStatuses = applications.stream()
                .collect(Collectors.toMap(Application::getId, Application::getStatus));

        service.batchApprove(ids);

        // No application should have been saved since none are PENDING_INITIAL_REVIEW
        verify(appRepo, never()).save(any(Application.class));

        // Statuses should remain unchanged
        for (Application app : applications) {
            assertThat(app.getStatus())
                    .as("Non-pending application %d status must remain unchanged", app.getId())
                    .isEqualTo(originalStatuses.get(app.getId()));
        }
    }

    @Property(tries = 200)
    void property31_batchRejectSkipsNonPendingApplications(
            @ForAll("nonPendingApplicationLists") List<Application> applications) {

        ApplicationRepository appRepo = mock(ApplicationRepository.class);
        UserRepository userRepo = mock(UserRepository.class);
        ApplicationServiceImpl service = createService(appRepo, userRepo);

        List<Long> ids = applications.stream().map(Application::getId).collect(Collectors.toList());
        when(appRepo.findAllById(ids)).thenReturn(new ArrayList<>(applications));

        Map<Long, ApplicationStatus> originalStatuses = applications.stream()
                .collect(Collectors.toMap(Application::getId, Application::getStatus));

        service.batchReject(ids);

        verify(appRepo, never()).save(any(Application.class));

        for (Application app : applications) {
            assertThat(app.getStatus())
                    .as("Non-pending application %d status must remain unchanged", app.getId())
                    .isEqualTo(originalStatuses.get(app.getId()));
        }
    }

    @Property(tries = 200)
    void property31_batchApproveEnablesUserAccountsForApprovedApplications(
            @ForAll("pendingOnlyApplicationLists") List<Application> applications) {

        ApplicationRepository appRepo = mock(ApplicationRepository.class);
        UserRepository userRepo = mock(UserRepository.class);
        ApplicationServiceImpl service = createService(appRepo, userRepo);

        List<Long> ids = applications.stream().map(Application::getId).collect(Collectors.toList());
        when(appRepo.findAllById(ids)).thenReturn(new ArrayList<>(applications));
        when(appRepo.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        // Create mock users for each application
        Map<Long, User> users = new HashMap<>();
        for (Application app : applications) {
            User user = User.builder()
                    .id(app.getUserId())
                    .username("user_" + app.getUserId())
                    .password("encoded")
                    .role(Role.APPLICANT)
                    .enabled(false)
                    .build();
            users.put(app.getUserId(), user);
            when(userRepo.findById(app.getUserId())).thenReturn(Optional.of(user));
            when(userRepo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        }

        service.batchApprove(ids);

        // All users should be enabled
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepo, times(applications.size())).save(userCaptor.capture());

        for (User savedUser : userCaptor.getAllValues()) {
            assertThat(savedUser.getEnabled())
                    .as("User %d must be enabled after batch approve", savedUser.getId())
                    .isTrue();
        }
    }

    @Property(tries = 100)
    void property31_batchApproveCountOfSavedMatchesPendingCount(
            @ForAll("applicationListsWithMixedStatuses") List<Application> applications) {

        ApplicationRepository appRepo = mock(ApplicationRepository.class);
        UserRepository userRepo = mock(UserRepository.class);
        ApplicationServiceImpl service = createService(appRepo, userRepo);

        long expectedPendingCount = applications.stream()
                .filter(a -> a.getStatus() == ApplicationStatus.PENDING_INITIAL_REVIEW)
                .count();

        List<Long> ids = applications.stream().map(Application::getId).collect(Collectors.toList());
        when(appRepo.findAllById(ids)).thenReturn(new ArrayList<>(applications));
        when(appRepo.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        for (Application app : applications) {
            if (app.getStatus() == ApplicationStatus.PENDING_INITIAL_REVIEW) {
                User user = User.builder()
                        .id(app.getUserId())
                        .username("user_" + app.getUserId())
                        .password("encoded")
                        .role(Role.APPLICANT)
                        .enabled(false)
                        .build();
                when(userRepo.findById(app.getUserId())).thenReturn(Optional.of(user));
                when(userRepo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            }
        }

        service.batchApprove(ids);

        // The number of application saves should equal the number of PENDING applications
        ArgumentCaptor<Application> captor = ArgumentCaptor.forClass(Application.class);
        verify(appRepo, times((int) expectedPendingCount)).save(captor.capture());
    }

    @Property(tries = 100)
    void property31_batchRejectCountOfSavedMatchesPendingCount(
            @ForAll("applicationListsWithMixedStatuses") List<Application> applications) {

        ApplicationRepository appRepo = mock(ApplicationRepository.class);
        UserRepository userRepo = mock(UserRepository.class);
        ApplicationServiceImpl service = createService(appRepo, userRepo);

        long expectedPendingCount = applications.stream()
                .filter(a -> a.getStatus() == ApplicationStatus.PENDING_INITIAL_REVIEW)
                .count();

        List<Long> ids = applications.stream().map(Application::getId).collect(Collectors.toList());
        when(appRepo.findAllById(ids)).thenReturn(new ArrayList<>(applications));
        when(appRepo.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        service.batchReject(ids);

        ArgumentCaptor<Application> captor = ArgumentCaptor.forClass(Application.class);
        verify(appRepo, times((int) expectedPendingCount)).save(captor.capture());
    }

    // ========== Arbitrary Providers ==========

    @Provide
    Arbitrary<List<Application>> applicationListsWithMixedStatuses() {
        return Arbitraries.integers().between(1, 10)
                .flatMap(size -> Arbitraries.of(ApplicationStatus.values())
                        .list().ofSize(size)
                        .map(statuses -> {
                            List<Application> apps = new ArrayList<>();
                            for (int i = 0; i < statuses.size(); i++) {
                                apps.add(buildApplication((long) (i + 1), (long) (i + 100), statuses.get(i)));
                            }
                            return apps;
                        }));
    }

    @Provide
    Arbitrary<List<Application>> nonPendingApplicationLists() {
        Arbitrary<ApplicationStatus> nonPendingStatus = Arbitraries.of(
                ApplicationStatus.INITIAL_REVIEW_PASSED,
                ApplicationStatus.REJECTED,
                ApplicationStatus.AUTO_REJECTED,
                ApplicationStatus.AI_INTERVIEW_IN_PROGRESS,
                ApplicationStatus.PENDING_REVIEW,
                ApplicationStatus.INTERN_OFFERED
        );
        return Arbitraries.integers().between(1, 8)
                .flatMap(size -> nonPendingStatus.list().ofSize(size)
                        .map(statuses -> {
                            List<Application> apps = new ArrayList<>();
                            for (int i = 0; i < statuses.size(); i++) {
                                apps.add(buildApplication((long) (i + 1), (long) (i + 100), statuses.get(i)));
                            }
                            return apps;
                        }));
    }

    @Provide
    Arbitrary<List<Application>> pendingOnlyApplicationLists() {
        return Arbitraries.integers().between(1, 8)
                .map(size -> {
                    List<Application> apps = new ArrayList<>();
                    for (int i = 0; i < size; i++) {
                        apps.add(buildApplication((long) (i + 1), (long) (i + 100),
                                ApplicationStatus.PENDING_INITIAL_REVIEW));
                    }
                    return apps;
                });
    }

    // ========== Helpers ==========

    private Application buildApplication(Long id, Long userId, ApplicationStatus status) {
        return Application.builder()
                .id(id)
                .userId(userId)
                .status(status)
                .entryType(EntryType.REGISTRATION)
                .build();
    }

    /**
     * Creates an ApplicationServiceImpl with mocked repositories.
     * Other dependencies are set to null since batch operations don't use them.
     */
    private ApplicationServiceImpl createService(ApplicationRepository appRepo, UserRepository userRepo) {
        return new ApplicationServiceImpl(
                appRepo,
                userRepo,
                null, // questionnaireResponseService
                null, // publicLinkService
                null, // passwordEncoder
                null, // applicationScreeningService
                null, // objectMapper
                Mockito.mock(ApplicationTimelineService.class)
        );
    }

    private long countOriginalPending(List<Application> apps, List<Long> ids,
                                       ApplicationRepository appRepo) {
        // This is a helper — the original pending count was already computed before batchApprove mutated them
        return 0; // Not used in final assertions
    }
}
