package com.pollen.management.service;

import com.pollen.management.entity.Application;
import com.pollen.management.entity.User;
import com.pollen.management.entity.enums.ApplicationStatus;
import com.pollen.management.entity.enums.EntryType;
import com.pollen.management.entity.enums.Role;
import com.pollen.management.repository.ApplicationRepository;
import com.pollen.management.repository.UserRepository;
import com.pollen.management.util.BusinessException;
import net.jqwik.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Feature: pollen-group-management, Property 14: 初审状态转换正确性
 * **Validates: Requirements 4.4, 4.5**
 *
 * Property 14: For any application with status PENDING_INITIAL_REVIEW:
 * - When approved=true: status always becomes INITIAL_REVIEW_PASSED, user.enabled always becomes true
 * - When approved=false: status always becomes REJECTED, user account is never modified
 * - Only applications with status PENDING_INITIAL_REVIEW can be reviewed (others throw 400)
 */
class InitialReviewProperties {

    private ApplicationServiceImpl createService(
            ApplicationRepository appRepo,
            UserRepository userRepo) {
        QuestionnaireResponseService qrService = Mockito.mock(QuestionnaireResponseService.class);
        PublicLinkService publicLinkService = Mockito.mock(PublicLinkService.class);
        PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);
        ApplicationScreeningService screeningService = Mockito.mock(ApplicationScreeningService.class);
        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        return new ApplicationServiceImpl(appRepo, userRepo, qrService, publicLinkService,
                passwordEncoder, screeningService, objectMapper, Mockito.mock(ApplicationTimelineService.class));
    }

    // ========== Approved: status → INITIAL_REVIEW_PASSED ==========

    /**
     * Property 14a: When approved=true, application status always becomes INITIAL_REVIEW_PASSED.
     */
    @Property(tries = 100)
    void approvedApplicationStatusIsAlwaysInitialReviewPassed(
            @ForAll("positiveIds") Long appId,
            @ForAll("positiveIds") Long userId) {

        ApplicationRepository appRepo = Mockito.mock(ApplicationRepository.class);
        UserRepository userRepo = Mockito.mock(UserRepository.class);
        ApplicationServiceImpl service = createService(appRepo, userRepo);

        Application app = Application.builder()
                .id(appId).userId(userId).status(ApplicationStatus.PENDING_INITIAL_REVIEW)
                .entryType(EntryType.REGISTRATION).build();
        User user = User.builder()
                .id(userId).username("user_" + userId).role(Role.APPLICANT).enabled(false).build();

        when(appRepo.findById(appId)).thenReturn(Optional.of(app));
        when(userRepo.findById(userId)).thenReturn(Optional.of(user));
        when(userRepo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(appRepo.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        service.initialReview(appId, true);

        assertThat(app.getStatus())
                .as("Approved application status must be INITIAL_REVIEW_PASSED for appId=%d", appId)
                .isEqualTo(ApplicationStatus.INITIAL_REVIEW_PASSED);
    }

    // ========== Approved: user.enabled → true ==========

    /**
     * Property 14b: When approved=true, user account is always enabled.
     */
    @Property(tries = 100)
    void approvedApplicationAlwaysEnablesUser(
            @ForAll("positiveIds") Long appId,
            @ForAll("positiveIds") Long userId) {

        ApplicationRepository appRepo = Mockito.mock(ApplicationRepository.class);
        UserRepository userRepo = Mockito.mock(UserRepository.class);
        ApplicationServiceImpl service = createService(appRepo, userRepo);

        Application app = Application.builder()
                .id(appId).userId(userId).status(ApplicationStatus.PENDING_INITIAL_REVIEW)
                .entryType(EntryType.REGISTRATION).build();
        User user = User.builder()
                .id(userId).username("user_" + userId).role(Role.APPLICANT).enabled(false).build();

        when(appRepo.findById(appId)).thenReturn(Optional.of(app));
        when(userRepo.findById(userId)).thenReturn(Optional.of(user));
        when(userRepo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(appRepo.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        service.initialReview(appId, true);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepo).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEnabled())
                .as("User must be enabled after approval for userId=%d", userId)
                .isTrue();
    }

    // ========== Rejected: status → REJECTED ==========

    /**
     * Property 14c: When approved=false, application status always becomes REJECTED.
     */
    @Property(tries = 100)
    void rejectedApplicationStatusIsAlwaysRejected(
            @ForAll("positiveIds") Long appId,
            @ForAll("positiveIds") Long userId) {

        ApplicationRepository appRepo = Mockito.mock(ApplicationRepository.class);
        UserRepository userRepo = Mockito.mock(UserRepository.class);
        ApplicationServiceImpl service = createService(appRepo, userRepo);

        Application app = Application.builder()
                .id(appId).userId(userId).status(ApplicationStatus.PENDING_INITIAL_REVIEW)
                .entryType(EntryType.REGISTRATION).build();

        when(appRepo.findById(appId)).thenReturn(Optional.of(app));
        when(appRepo.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        service.initialReview(appId, false);

        assertThat(app.getStatus())
                .as("Rejected application status must be REJECTED for appId=%d", appId)
                .isEqualTo(ApplicationStatus.REJECTED);
    }

    // ========== Rejected: user account never modified ==========

    /**
     * Property 14d: When approved=false, user account is never modified (userRepo.save never called).
     */
    @Property(tries = 100)
    void rejectedApplicationNeverModifiesUser(
            @ForAll("positiveIds") Long appId,
            @ForAll("positiveIds") Long userId) {

        ApplicationRepository appRepo = Mockito.mock(ApplicationRepository.class);
        UserRepository userRepo = Mockito.mock(UserRepository.class);
        ApplicationServiceImpl service = createService(appRepo, userRepo);

        Application app = Application.builder()
                .id(appId).userId(userId).status(ApplicationStatus.PENDING_INITIAL_REVIEW)
                .entryType(EntryType.REGISTRATION).build();

        when(appRepo.findById(appId)).thenReturn(Optional.of(app));
        when(appRepo.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        service.initialReview(appId, false);

        verify(userRepo, never()).save(any(User.class));
    }

    // ========== Non-PENDING_INITIAL_REVIEW status throws 400 ==========

    /**
     * Property 14e: Only applications with status PENDING_INITIAL_REVIEW can be reviewed.
     * Any other status always throws BusinessException with code 400.
     */
    @Property(tries = 100)
    void nonPendingInitialReviewStatusAlwaysThrows400(
            @ForAll("positiveIds") Long appId,
            @ForAll("positiveIds") Long userId,
            @ForAll("nonPendingStatuses") ApplicationStatus status,
            @ForAll boolean approved) {

        ApplicationRepository appRepo = Mockito.mock(ApplicationRepository.class);
        UserRepository userRepo = Mockito.mock(UserRepository.class);
        ApplicationServiceImpl service = createService(appRepo, userRepo);

        Application app = Application.builder()
                .id(appId).userId(userId).status(status)
                .entryType(EntryType.REGISTRATION).build();

        when(appRepo.findById(appId)).thenReturn(Optional.of(app));

        assertThatThrownBy(() -> service.initialReview(appId, approved))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException bex = (BusinessException) ex;
                    assertThat(bex.getCode())
                            .as("Non-PENDING_INITIAL_REVIEW status %s must return code 400", status)
                            .isEqualTo(400);
                });

        verify(appRepo, never()).save(any(Application.class));
        verify(userRepo, never()).save(any(User.class));
    }

    // ========== Providers ==========

    @Provide
    Arbitrary<Long> positiveIds() {
        return Arbitraries.longs().between(1L, 100_000L);
    }

    @Provide
    Arbitrary<ApplicationStatus> nonPendingStatuses() {
        return Arbitraries.of(
                ApplicationStatus.INITIAL_REVIEW_PASSED,
                ApplicationStatus.REJECTED,
                ApplicationStatus.AI_INTERVIEW_IN_PROGRESS,
                ApplicationStatus.PENDING_REVIEW,
                ApplicationStatus.INTERN_OFFERED
        );
    }
}
