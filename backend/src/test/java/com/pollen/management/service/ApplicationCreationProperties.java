package com.pollen.management.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pollen.management.dto.ApplicationFormData;
import com.pollen.management.dto.ScreeningResult;
import com.pollen.management.entity.Application;
import com.pollen.management.entity.User;
import com.pollen.management.entity.enums.ApplicationStatus;
import com.pollen.management.entity.enums.EducationStage;
import com.pollen.management.entity.enums.EntryType;
import com.pollen.management.entity.enums.Role;
import com.pollen.management.repository.ApplicationRepository;
import com.pollen.management.repository.UserRepository;
import com.pollen.management.util.BusinessException;
import net.jqwik.api.*;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Feature: pollen-group-management, Property 12: 申请记录创建不变量
 * Feature: pollen-group-management, Property 15: 重复申请拒绝
 * **Validates: Requirements 4.1, 4.2, 4.6**
 *
 * Property 12: For any valid user, questionnaire response, and form data that passes screening,
 * createFromRegistration always produces an Application with status=PENDING_INITIAL_REVIEW,
 * entryType=REGISTRATION, correct userId and questionnaireResponseId.
 * When screening fails, status is AUTO_REJECTED.
 *
 * Property 15: If a user already has a pending application, calling
 * createFromRegistration again always throws BusinessException with code 409.
 */
class ApplicationCreationProperties {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Creates a fresh ApplicationServiceImpl with mocked dependencies for each test run.
     */
    private ApplicationServiceImpl createService(
            ApplicationRepository appRepo,
            UserRepository userRepo,
            ApplicationScreeningService screeningService) {
        QuestionnaireResponseService qrService = Mockito.mock(QuestionnaireResponseService.class);
        PublicLinkService publicLinkService = Mockito.mock(PublicLinkService.class);
        PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);
        return new ApplicationServiceImpl(appRepo, userRepo, qrService, publicLinkService,
                passwordEncoder, screeningService, OBJECT_MAPPER, Mockito.mock(ApplicationTimelineService.class));
    }

    /** Creates a valid form data that will pass screening */
    private ApplicationFormData validFormData() {
        return ApplicationFormData.builder()
                .pollenUid("12345")
                .birthDate(LocalDate.of(2000, 1, 1))
                .educationStage(EducationStage.UNIVERSITY)
                .weeklyAvailableDays(5)
                .dailyAvailableHours(BigDecimal.valueOf(4))
                .build();
    }

    /** Creates a screening result that passes */
    private ScreeningResult passedResult() {
        return ScreeningResult.builder()
                .passed(true).needsAttention(false)
                .rejectReason(null).attentionFlags(List.of())
                .build();
    }

    // ========== Property 12: 申请记录创建不变量 ==========

    /**
     * Property 12a: createFromRegistration always sets status to PENDING_INITIAL_REVIEW
     * when screening passes.
     */
    @Property(tries = 100)
    void applicationStatusIsPendingWhenScreeningPasses(
            @ForAll("positiveIds") Long userId,
            @ForAll("positiveIds") Long responseId) {

        ApplicationRepository appRepo = Mockito.mock(ApplicationRepository.class);
        UserRepository userRepo = Mockito.mock(UserRepository.class);
        ApplicationScreeningService screeningService = Mockito.mock(ApplicationScreeningService.class);
        ApplicationServiceImpl service = createService(appRepo, userRepo, screeningService);

        ApplicationFormData formData = validFormData();
        User user = User.builder().id(userId).username("user_" + userId).role(Role.APPLICANT).enabled(false).build();
        when(userRepo.findById(userId)).thenReturn(Optional.of(user));
        when(appRepo.existsByUserIdAndStatusIn(eq(userId), anyList())).thenReturn(false);
        when(screeningService.autoScreen(formData)).thenReturn(passedResult());
        when(screeningService.calculateAge(formData.getBirthDate())).thenReturn(24);
        when(appRepo.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        Application result = service.createFromRegistration(userId, responseId, formData);

        assertThat(result.getStatus())
                .as("Application status must be PENDING_INITIAL_REVIEW for userId=%d", userId)
                .isEqualTo(ApplicationStatus.PENDING_INITIAL_REVIEW);
    }

    /**
     * Property 12b: createFromRegistration always sets entryType to REGISTRATION.
     */
    @Property(tries = 100)
    void applicationEntryTypeIsAlwaysRegistration(
            @ForAll("positiveIds") Long userId,
            @ForAll("positiveIds") Long responseId) {

        ApplicationRepository appRepo = Mockito.mock(ApplicationRepository.class);
        UserRepository userRepo = Mockito.mock(UserRepository.class);
        ApplicationScreeningService screeningService = Mockito.mock(ApplicationScreeningService.class);
        ApplicationServiceImpl service = createService(appRepo, userRepo, screeningService);

        ApplicationFormData formData = validFormData();
        User user = User.builder().id(userId).username("user_" + userId).role(Role.APPLICANT).enabled(false).build();
        when(userRepo.findById(userId)).thenReturn(Optional.of(user));
        when(appRepo.existsByUserIdAndStatusIn(eq(userId), anyList())).thenReturn(false);
        when(screeningService.autoScreen(formData)).thenReturn(passedResult());
        when(screeningService.calculateAge(formData.getBirthDate())).thenReturn(24);
        when(appRepo.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        Application result = service.createFromRegistration(userId, responseId, formData);

        assertThat(result.getEntryType())
                .as("Application entryType must be REGISTRATION for userId=%d", userId)
                .isEqualTo(EntryType.REGISTRATION);
    }

    /**
     * Property 12c: createFromRegistration always associates the correct userId.
     */
    @Property(tries = 100)
    void applicationHasCorrectUserId(
            @ForAll("positiveIds") Long userId,
            @ForAll("positiveIds") Long responseId) {

        ApplicationRepository appRepo = Mockito.mock(ApplicationRepository.class);
        UserRepository userRepo = Mockito.mock(UserRepository.class);
        ApplicationScreeningService screeningService = Mockito.mock(ApplicationScreeningService.class);
        ApplicationServiceImpl service = createService(appRepo, userRepo, screeningService);

        ApplicationFormData formData = validFormData();
        User user = User.builder().id(userId).username("user_" + userId).role(Role.APPLICANT).enabled(false).build();
        when(userRepo.findById(userId)).thenReturn(Optional.of(user));
        when(appRepo.existsByUserIdAndStatusIn(eq(userId), anyList())).thenReturn(false);
        when(screeningService.autoScreen(formData)).thenReturn(passedResult());
        when(screeningService.calculateAge(formData.getBirthDate())).thenReturn(24);
        when(appRepo.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        Application result = service.createFromRegistration(userId, responseId, formData);

        assertThat(result.getUserId())
                .as("Application userId must match the input userId=%d", userId)
                .isEqualTo(userId);
    }

    /**
     * Property 12d: createFromRegistration always associates the correct questionnaireResponseId.
     */
    @Property(tries = 100)
    void applicationHasCorrectQuestionnaireResponseId(
            @ForAll("positiveIds") Long userId,
            @ForAll("positiveIds") Long responseId) {

        ApplicationRepository appRepo = Mockito.mock(ApplicationRepository.class);
        UserRepository userRepo = Mockito.mock(UserRepository.class);
        ApplicationScreeningService screeningService = Mockito.mock(ApplicationScreeningService.class);
        ApplicationServiceImpl service = createService(appRepo, userRepo, screeningService);

        ApplicationFormData formData = validFormData();
        User user = User.builder().id(userId).username("user_" + userId).role(Role.APPLICANT).enabled(false).build();
        when(userRepo.findById(userId)).thenReturn(Optional.of(user));
        when(appRepo.existsByUserIdAndStatusIn(eq(userId), anyList())).thenReturn(false);
        when(screeningService.autoScreen(formData)).thenReturn(passedResult());
        when(screeningService.calculateAge(formData.getBirthDate())).thenReturn(24);
        when(appRepo.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        Application result = service.createFromRegistration(userId, responseId, formData);

        assertThat(result.getQuestionnaireResponseId())
                .as("Application questionnaireResponseId must match the input responseId=%d", responseId)
                .isEqualTo(responseId);
    }

    // ========== Property 15: 重复申请拒绝 ==========

    /**
     * Property 15a: If a user already has a pending application, createFromRegistration
     * always throws BusinessException with code 409.
     */
    @Property(tries = 100)
    void duplicateApplicationAlwaysThrows409(
            @ForAll("positiveIds") Long userId,
            @ForAll("positiveIds") Long responseId) {

        ApplicationRepository appRepo = Mockito.mock(ApplicationRepository.class);
        UserRepository userRepo = Mockito.mock(UserRepository.class);
        ApplicationScreeningService screeningService = Mockito.mock(ApplicationScreeningService.class);
        ApplicationServiceImpl service = createService(appRepo, userRepo, screeningService);

        ApplicationFormData formData = validFormData();
        User user = User.builder().id(userId).username("user_" + userId).role(Role.APPLICANT).enabled(false).build();
        when(userRepo.findById(userId)).thenReturn(Optional.of(user));
        when(appRepo.existsByUserIdAndStatusIn(eq(userId), anyList())).thenReturn(true);

        assertThatThrownBy(() -> service.createFromRegistration(userId, responseId, formData))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException bex = (BusinessException) ex;
                    assertThat(bex.getCode())
                            .as("Duplicate application must return code 409 for userId=%d", userId)
                            .isEqualTo(409);
                });
    }

    /**
     * Property 15b: When a duplicate application is rejected, the repository save
     * is never called — the original application remains unaffected.
     */
    @Property(tries = 100)
    void duplicateApplicationNeverSaves(
            @ForAll("positiveIds") Long userId,
            @ForAll("positiveIds") Long responseId) {

        ApplicationRepository appRepo = Mockito.mock(ApplicationRepository.class);
        UserRepository userRepo = Mockito.mock(UserRepository.class);
        ApplicationScreeningService screeningService = Mockito.mock(ApplicationScreeningService.class);
        ApplicationServiceImpl service = createService(appRepo, userRepo, screeningService);

        ApplicationFormData formData = validFormData();
        User user = User.builder().id(userId).username("user_" + userId).role(Role.APPLICANT).enabled(false).build();
        when(userRepo.findById(userId)).thenReturn(Optional.of(user));
        when(appRepo.existsByUserIdAndStatusIn(eq(userId), anyList())).thenReturn(true);

        try {
            service.createFromRegistration(userId, responseId, formData);
        } catch (BusinessException ignored) {
            // expected
        }

        verify(appRepo, never()).save(any(Application.class));
    }

    // ========== Providers ==========

    @Provide
    Arbitrary<Long> positiveIds() {
        return Arbitraries.longs().between(1L, 100_000L);
    }
}
