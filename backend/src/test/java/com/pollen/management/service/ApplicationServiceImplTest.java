package com.pollen.management.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pollen.management.dto.ApplicationFormData;
import com.pollen.management.dto.ScreeningResult;
import com.pollen.management.entity.Application;
import com.pollen.management.entity.ApplicationTimeline;
import com.pollen.management.entity.PublicLink;
import com.pollen.management.entity.QuestionnaireResponse;
import com.pollen.management.entity.User;
import com.pollen.management.entity.enums.ApplicationStatus;
import com.pollen.management.entity.enums.EducationStage;
import com.pollen.management.entity.enums.EntryType;
import com.pollen.management.entity.enums.Role;
import com.pollen.management.repository.ApplicationRepository;
import com.pollen.management.repository.UserRepository;
import com.pollen.management.util.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceImplTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private QuestionnaireResponseService questionnaireResponseService;

    @Mock
    private PublicLinkService publicLinkService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ApplicationScreeningService applicationScreeningService;

    @Mock
    private ApplicationTimelineService applicationTimelineService;

    private ApplicationServiceImpl applicationService;

    /** Default form data that passes screening */
    private ApplicationFormData validFormData;

    /** Default screening result that passes */
    private ScreeningResult passedScreeningResult;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        applicationService = new ApplicationServiceImpl(
                applicationRepository, userRepository, questionnaireResponseService,
                publicLinkService, passwordEncoder, applicationScreeningService, objectMapper,
                applicationTimelineService);

        validFormData = ApplicationFormData.builder()
                .pollenUid("12345")
                .birthDate(LocalDate.of(2000, 1, 1))
                .educationStage(EducationStage.UNIVERSITY)
                .weeklyAvailableDays(5)
                .dailyAvailableHours(BigDecimal.valueOf(4))
                .build();

        passedScreeningResult = ScreeningResult.builder()
                .passed(true)
                .needsAttention(false)
                .rejectReason(null)
                .attentionFlags(List.of())
                .build();
    }

    // --- createFromRegistration tests ---

    @Test
    void createFromRegistration_shouldCreateApplicationWithPendingStatusWhenScreeningPasses() {
        User user = User.builder().id(1L).username("testuser").role(Role.APPLICANT).enabled(false).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(applicationRepository.existsByUserIdAndStatusIn(eq(1L), anyList())).thenReturn(false);
        when(applicationScreeningService.autoScreen(validFormData)).thenReturn(passedScreeningResult);
        when(applicationScreeningService.calculateAge(validFormData.getBirthDate())).thenReturn(24);
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> {
            Application app = inv.getArgument(0);
            app.setId(10L);
            return app;
        });

        Application result = applicationService.createFromRegistration(1L, 100L, validFormData);

        assertNotNull(result);
        assertEquals(ApplicationStatus.PENDING_INITIAL_REVIEW, result.getStatus());
        assertTrue(result.getScreeningPassed());
    }

    @Test
    void createFromRegistration_shouldSetAutoRejectedWhenScreeningFails() {
        User user = User.builder().id(1L).username("testuser").role(Role.APPLICANT).enabled(false).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(applicationRepository.existsByUserIdAndStatusIn(eq(1L), anyList())).thenReturn(false);
        ScreeningResult rejected = ScreeningResult.builder()
                .passed(false).needsAttention(false)
                .rejectReason("年龄不符合要求，申请者未满18岁")
                .attentionFlags(List.of()).build();
        when(applicationScreeningService.autoScreen(validFormData)).thenReturn(rejected);
        when(applicationScreeningService.calculateAge(validFormData.getBirthDate())).thenReturn(17);
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        Application result = applicationService.createFromRegistration(1L, 100L, validFormData);

        assertEquals(ApplicationStatus.AUTO_REJECTED, result.getStatus());
        assertFalse(result.getScreeningPassed());
        assertEquals("年龄不符合要求，申请者未满18岁", result.getScreeningRejectReason());
    }

    @Test
    void createFromRegistration_shouldSetEntryTypeRegistration() {
        User user = User.builder().id(1L).username("testuser").role(Role.APPLICANT).enabled(false).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(applicationRepository.existsByUserIdAndStatusIn(eq(1L), anyList())).thenReturn(false);
        when(applicationScreeningService.autoScreen(validFormData)).thenReturn(passedScreeningResult);
        when(applicationScreeningService.calculateAge(validFormData.getBirthDate())).thenReturn(24);
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        Application result = applicationService.createFromRegistration(1L, 100L, validFormData);

        assertEquals(EntryType.REGISTRATION, result.getEntryType());
    }

    @Test
    void createFromRegistration_shouldCopyFormDataFields() {
        User user = User.builder().id(1L).username("testuser").role(Role.APPLICANT).enabled(false).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(applicationRepository.existsByUserIdAndStatusIn(eq(1L), anyList())).thenReturn(false);
        when(applicationScreeningService.autoScreen(validFormData)).thenReturn(passedScreeningResult);
        when(applicationScreeningService.calculateAge(validFormData.getBirthDate())).thenReturn(24);
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        Application result = applicationService.createFromRegistration(1L, 100L, validFormData);

        assertEquals("12345", result.getPollenUid());
        assertEquals(LocalDate.of(2000, 1, 1), result.getBirthDate());
        assertEquals(24, result.getCalculatedAge());
        assertEquals(EducationStage.UNIVERSITY, result.getEducationStage());
        assertEquals(5, result.getWeeklyAvailableDays());
        assertEquals(BigDecimal.valueOf(4), result.getDailyAvailableHours());
    }

    @Test
    void createFromRegistration_shouldSetNeedsAttentionFromScreeningResult() {
        User user = User.builder().id(1L).username("testuser").role(Role.APPLICANT).enabled(false).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(applicationRepository.existsByUserIdAndStatusIn(eq(1L), anyList())).thenReturn(false);
        ScreeningResult attentionResult = ScreeningResult.builder()
                .passed(true).needsAttention(true)
                .attentionFlags(List.of("年龄刚满18岁（18岁），建议重点审核")).build();
        when(applicationScreeningService.autoScreen(validFormData)).thenReturn(attentionResult);
        when(applicationScreeningService.calculateAge(validFormData.getBirthDate())).thenReturn(18);
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        Application result = applicationService.createFromRegistration(1L, 100L, validFormData);

        assertEquals(ApplicationStatus.PENDING_INITIAL_REVIEW, result.getStatus());
        assertTrue(result.getNeedsAttention());
        assertNotNull(result.getAttentionFlags());
    }

    @Test
    void createFromRegistration_shouldAssociateQuestionnaireResponse() {
        User user = User.builder().id(1L).username("testuser").role(Role.APPLICANT).enabled(false).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(applicationRepository.existsByUserIdAndStatusIn(eq(1L), anyList())).thenReturn(false);
        when(applicationScreeningService.autoScreen(validFormData)).thenReturn(passedScreeningResult);
        when(applicationScreeningService.calculateAge(validFormData.getBirthDate())).thenReturn(24);
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        Application result = applicationService.createFromRegistration(1L, 100L, validFormData);

        assertEquals(100L, result.getQuestionnaireResponseId());
        assertEquals(1L, result.getUserId());
    }

    @Test
    void createFromRegistration_shouldThrowWhenUserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> applicationService.createFromRegistration(999L, 100L, validFormData));

        assertEquals(404, ex.getCode());
        assertEquals("用户不存在", ex.getMessage());
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void createFromRegistration_shouldRejectDuplicateApplication() {
        User user = User.builder().id(1L).username("testuser").role(Role.APPLICANT).enabled(false).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(applicationRepository.existsByUserIdAndStatusIn(eq(1L), anyList())).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> applicationService.createFromRegistration(1L, 100L, validFormData));

        assertEquals(409, ex.getCode());
        assertEquals("已有未处理的申请", ex.getMessage());
        verify(applicationRepository, never()).save(any());
    }

    // --- createFromPublicLink tests ---

    @Test
    void createFromPublicLink_shouldCreateUserWithApplicantRoleAndDisabled() {
        PublicLink link = PublicLink.builder().id(1L).linkToken("token123").versionId(5L).active(true).build();
        when(publicLinkService.getActiveLink("token123")).thenReturn(link);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(2L);
            return u;
        });
        QuestionnaireResponse qr = QuestionnaireResponse.builder().id(50L).versionId(5L).userId(2L).build();
        when(questionnaireResponseService.submit(eq(5L), eq(2L), anyMap())).thenReturn(qr);
        when(applicationScreeningService.autoScreen(validFormData)).thenReturn(passedScreeningResult);
        when(applicationScreeningService.calculateAge(validFormData.getBirthDate())).thenReturn(24);
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> {
            Application app = inv.getArgument(0);
            app.setId(20L);
            return app;
        });

        applicationService.createFromPublicLink("token123", Map.of("q1", "a1"), validFormData);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals(Role.APPLICANT, savedUser.getRole());
        assertFalse(savedUser.getEnabled());
    }

    @Test
    void createFromPublicLink_shouldCreateApplicationWithCorrectFieldsWhenScreeningPasses() {
        PublicLink link = PublicLink.builder().id(1L).linkToken("token123").versionId(5L).active(true).build();
        when(publicLinkService.getActiveLink("token123")).thenReturn(link);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(2L);
            return u;
        });
        QuestionnaireResponse qr = QuestionnaireResponse.builder().id(50L).versionId(5L).userId(2L).build();
        when(questionnaireResponseService.submit(eq(5L), eq(2L), anyMap())).thenReturn(qr);
        when(applicationScreeningService.autoScreen(validFormData)).thenReturn(passedScreeningResult);
        when(applicationScreeningService.calculateAge(validFormData.getBirthDate())).thenReturn(24);
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        Application result = applicationService.createFromPublicLink("token123", Map.of("q1", "a1"), validFormData);

        assertEquals(ApplicationStatus.PENDING_INITIAL_REVIEW, result.getStatus());
        assertEquals(EntryType.PUBLIC_LINK, result.getEntryType());
        assertEquals(2L, result.getUserId());
        assertEquals(50L, result.getQuestionnaireResponseId());
        assertTrue(result.getScreeningPassed());
    }

    @Test
    void createFromPublicLink_shouldSetAutoRejectedWhenScreeningFails() {
        PublicLink link = PublicLink.builder().id(1L).linkToken("token123").versionId(5L).active(true).build();
        when(publicLinkService.getActiveLink("token123")).thenReturn(link);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(2L);
            return u;
        });
        QuestionnaireResponse qr = QuestionnaireResponse.builder().id(50L).versionId(5L).userId(2L).build();
        when(questionnaireResponseService.submit(eq(5L), eq(2L), anyMap())).thenReturn(qr);
        ScreeningResult rejected = ScreeningResult.builder()
                .passed(false).needsAttention(false)
                .rejectReason("花粉社区UID格式不合法").attentionFlags(List.of()).build();
        when(applicationScreeningService.autoScreen(validFormData)).thenReturn(rejected);
        when(applicationScreeningService.calculateAge(validFormData.getBirthDate())).thenReturn(24);
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        Application result = applicationService.createFromPublicLink("token123", Map.of("q1", "a1"), validFormData);

        assertEquals(ApplicationStatus.AUTO_REJECTED, result.getStatus());
        assertFalse(result.getScreeningPassed());
        assertEquals("花粉社区UID格式不合法", result.getScreeningRejectReason());
    }

    @Test
    void createFromPublicLink_shouldSubmitQuestionnaireResponse() {
        PublicLink link = PublicLink.builder().id(1L).linkToken("token123").versionId(5L).active(true).build();
        when(publicLinkService.getActiveLink("token123")).thenReturn(link);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(2L);
            return u;
        });
        Map<String, Object> answers = Map.of("name", "张三", "age", 25);
        QuestionnaireResponse qr = QuestionnaireResponse.builder().id(50L).versionId(5L).userId(2L).build();
        when(questionnaireResponseService.submit(eq(5L), eq(2L), eq(answers))).thenReturn(qr);
        when(applicationScreeningService.autoScreen(validFormData)).thenReturn(passedScreeningResult);
        when(applicationScreeningService.calculateAge(validFormData.getBirthDate())).thenReturn(24);
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        applicationService.createFromPublicLink("token123", answers, validFormData);

        verify(questionnaireResponseService).submit(5L, 2L, answers);
    }

    @Test
    void createFromPublicLink_shouldGenerateUniqueUsername() {
        PublicLink link = PublicLink.builder().id(1L).linkToken("token123").versionId(5L).active(true).build();
        when(publicLinkService.getActiveLink("token123")).thenReturn(link);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(2L);
            return u;
        });
        QuestionnaireResponse qr = QuestionnaireResponse.builder().id(50L).versionId(5L).userId(2L).build();
        when(questionnaireResponseService.submit(anyLong(), anyLong(), anyMap())).thenReturn(qr);
        when(applicationScreeningService.autoScreen(validFormData)).thenReturn(passedScreeningResult);
        when(applicationScreeningService.calculateAge(validFormData.getBirthDate())).thenReturn(24);
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        applicationService.createFromPublicLink("token123", Map.of("q1", "a1"), validFormData);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertTrue(userCaptor.getValue().getUsername().startsWith("public_"));
    }

    @Test
    void createFromPublicLink_shouldEncodePassword() {
        PublicLink link = PublicLink.builder().id(1L).linkToken("token123").versionId(5L).active(true).build();
        when(publicLinkService.getActiveLink("token123")).thenReturn(link);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(2L);
            return u;
        });
        QuestionnaireResponse qr = QuestionnaireResponse.builder().id(50L).versionId(5L).userId(2L).build();
        when(questionnaireResponseService.submit(anyLong(), anyLong(), anyMap())).thenReturn(qr);
        when(applicationScreeningService.autoScreen(validFormData)).thenReturn(passedScreeningResult);
        when(applicationScreeningService.calculateAge(validFormData.getBirthDate())).thenReturn(24);
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        applicationService.createFromPublicLink("token123", Map.of("q1", "a1"), validFormData);

        verify(passwordEncoder).encode(anyString());
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("$2a$10$encoded", userCaptor.getValue().getPassword());
    }

    @Test
    void createFromPublicLink_shouldPropagateInvalidLinkException() {
        when(publicLinkService.getActiveLink("invalid")).thenThrow(new BusinessException(404, "链接无效或已过期"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> applicationService.createFromPublicLink("invalid", Map.of(), validFormData));

        assertEquals(404, ex.getCode());
        assertEquals("链接无效或已过期", ex.getMessage());
        verify(userRepository, never()).save(any());
        verify(applicationRepository, never()).save(any());
    }

    // --- initialReview tests ---

    @Test
    void initialReview_approved_shouldSetStatusToInitialReviewPassed() {
        Application app = Application.builder()
                .id(1L).userId(10L).status(ApplicationStatus.PENDING_INITIAL_REVIEW).build();
        User user = User.builder().id(10L).username("applicant").role(Role.APPLICANT).enabled(false).build();

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        applicationService.initialReview(1L, true);

        assertEquals(ApplicationStatus.INITIAL_REVIEW_PASSED, app.getStatus());
    }

    @Test
    void initialReview_approved_shouldEnableUserAccount() {
        Application app = Application.builder()
                .id(1L).userId(10L).status(ApplicationStatus.PENDING_INITIAL_REVIEW).build();
        User user = User.builder().id(10L).username("applicant").role(Role.APPLICANT).enabled(false).build();

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        applicationService.initialReview(1L, true);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertTrue(userCaptor.getValue().getEnabled());
    }

    @Test
    void initialReview_approved_shouldSaveBothApplicationAndUser() {
        Application app = Application.builder()
                .id(1L).userId(10L).status(ApplicationStatus.PENDING_INITIAL_REVIEW).build();
        User user = User.builder().id(10L).username("applicant").role(Role.APPLICANT).enabled(false).build();

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        applicationService.initialReview(1L, true);

        verify(applicationRepository).save(app);
        verify(userRepository).save(user);
    }

    @Test
    void initialReview_rejected_shouldSetStatusToRejected() {
        Application app = Application.builder()
                .id(1L).userId(10L).status(ApplicationStatus.PENDING_INITIAL_REVIEW).build();

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        applicationService.initialReview(1L, false);

        assertEquals(ApplicationStatus.REJECTED, app.getStatus());
    }

    @Test
    void initialReview_rejected_shouldNotEnableUserAccount() {
        Application app = Application.builder()
                .id(1L).userId(10L).status(ApplicationStatus.PENDING_INITIAL_REVIEW).build();

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        applicationService.initialReview(1L, false);

        verify(userRepository, never()).save(any());
    }

    @Test
    void initialReview_shouldThrowWhenApplicationNotFound() {
        when(applicationRepository.findById(999L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> applicationService.initialReview(999L, true));

        assertEquals(404, ex.getCode());
        assertEquals("申请记录不存在", ex.getMessage());
    }

    @Test
    void initialReview_shouldThrowWhenStatusNotPendingInitialReview() {
        Application app = Application.builder()
                .id(1L).userId(10L).status(ApplicationStatus.REJECTED).build();

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> applicationService.initialReview(1L, true));

        assertEquals(400, ex.getCode());
        assertEquals("当前申请状态不允许此操作", ex.getMessage());
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void initialReview_shouldThrowWhenStatusIsAlreadyPassed() {
        Application app = Application.builder()
                .id(1L).userId(10L).status(ApplicationStatus.INITIAL_REVIEW_PASSED).build();

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> applicationService.initialReview(1L, false));

        assertEquals(400, ex.getCode());
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void initialReview_approved_shouldThrowWhenUserNotFound() {
        Application app = Application.builder()
                .id(1L).userId(10L).status(ApplicationStatus.PENDING_INITIAL_REVIEW).build();

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));
        when(userRepository.findById(10L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> applicationService.initialReview(1L, true));

        assertEquals(404, ex.getCode());
        assertEquals("用户不存在", ex.getMessage());
    }

    // --- listAll tests ---

    @Test
    void listAll_shouldReturnApplicationsOrderedByCreatedAtDesc() {
        Application app1 = Application.builder().id(1L).userId(1L)
                .status(ApplicationStatus.PENDING_INITIAL_REVIEW).entryType(EntryType.REGISTRATION).build();
        Application app2 = Application.builder().id(2L).userId(2L)
                .status(ApplicationStatus.REJECTED).entryType(EntryType.PUBLIC_LINK).build();
        Application app3 = Application.builder().id(3L).userId(3L)
                .status(ApplicationStatus.INITIAL_REVIEW_PASSED).entryType(EntryType.REGISTRATION).build();

        when(applicationRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(app3, app2, app1));

        List<Application> result = applicationService.listAll();

        assertEquals(3, result.size());
        assertEquals(3L, result.get(0).getId());
        assertEquals(2L, result.get(1).getId());
        assertEquals(1L, result.get(2).getId());
    }

    @Test
    void listAll_shouldReturnEmptyListWhenNoApplications() {
        when(applicationRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

        List<Application> result = applicationService.listAll();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void listAll_shouldDelegateToRepository() {
        when(applicationRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

        applicationService.listAll();

        verify(applicationRepository).findAllByOrderByCreatedAtDesc();
    }

    // --- batchApprove tests ---

    @Test
    void batchApprove_shouldApproveAllPendingApplicationsAndEnableUsers() {
        Application app1 = Application.builder().id(1L).userId(10L).status(ApplicationStatus.PENDING_INITIAL_REVIEW).build();
        Application app2 = Application.builder().id(2L).userId(20L).status(ApplicationStatus.PENDING_INITIAL_REVIEW).build();
        User user1 = User.builder().id(10L).username("u1").role(Role.APPLICANT).enabled(false).build();
        User user2 = User.builder().id(20L).username("u2").role(Role.APPLICANT).enabled(false).build();

        when(applicationRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(app1, app2));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user1));
        when(userRepository.findById(20L)).thenReturn(Optional.of(user2));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        applicationService.batchApprove(List.of(1L, 2L));

        assertEquals(ApplicationStatus.INITIAL_REVIEW_PASSED, app1.getStatus());
        assertEquals(ApplicationStatus.INITIAL_REVIEW_PASSED, app2.getStatus());
        assertTrue(user1.getEnabled());
        assertTrue(user2.getEnabled());
        verify(applicationRepository, times(2)).save(any(Application.class));
        verify(userRepository, times(2)).save(any(User.class));
    }

    @Test
    void batchApprove_shouldSkipNonPendingApplications() {
        Application pending = Application.builder().id(1L).userId(10L).status(ApplicationStatus.PENDING_INITIAL_REVIEW).build();
        Application rejected = Application.builder().id(2L).userId(20L).status(ApplicationStatus.REJECTED).build();
        Application passed = Application.builder().id(3L).userId(30L).status(ApplicationStatus.INITIAL_REVIEW_PASSED).build();
        User user1 = User.builder().id(10L).username("u1").role(Role.APPLICANT).enabled(false).build();

        when(applicationRepository.findAllById(List.of(1L, 2L, 3L))).thenReturn(List.of(pending, rejected, passed));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user1));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        applicationService.batchApprove(List.of(1L, 2L, 3L));

        assertEquals(ApplicationStatus.INITIAL_REVIEW_PASSED, pending.getStatus());
        assertEquals(ApplicationStatus.REJECTED, rejected.getStatus());
        assertEquals(ApplicationStatus.INITIAL_REVIEW_PASSED, passed.getStatus());
        verify(applicationRepository, times(1)).save(any(Application.class));
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void batchApprove_shouldHandleEmptyResultGracefully() {
        when(applicationRepository.findAllById(List.of(999L))).thenReturn(List.of());

        applicationService.batchApprove(List.of(999L));

        verify(applicationRepository, never()).save(any(Application.class));
        verify(userRepository, never()).save(any(User.class));
    }

    // --- batchReject tests ---

    @Test
    void batchReject_shouldRejectAllPendingApplications() {
        Application app1 = Application.builder().id(1L).userId(10L).status(ApplicationStatus.PENDING_INITIAL_REVIEW).build();
        Application app2 = Application.builder().id(2L).userId(20L).status(ApplicationStatus.PENDING_INITIAL_REVIEW).build();

        when(applicationRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(app1, app2));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        applicationService.batchReject(List.of(1L, 2L));

        assertEquals(ApplicationStatus.REJECTED, app1.getStatus());
        assertEquals(ApplicationStatus.REJECTED, app2.getStatus());
        verify(applicationRepository, times(2)).save(any(Application.class));
    }

    @Test
    void batchReject_shouldSkipNonPendingApplications() {
        Application pending = Application.builder().id(1L).userId(10L).status(ApplicationStatus.PENDING_INITIAL_REVIEW).build();
        Application alreadyPassed = Application.builder().id(2L).userId(20L).status(ApplicationStatus.INITIAL_REVIEW_PASSED).build();

        when(applicationRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(pending, alreadyPassed));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        applicationService.batchReject(List.of(1L, 2L));

        assertEquals(ApplicationStatus.REJECTED, pending.getStatus());
        assertEquals(ApplicationStatus.INITIAL_REVIEW_PASSED, alreadyPassed.getStatus());
        verify(applicationRepository, times(1)).save(any(Application.class));
    }

    @Test
    void batchReject_shouldNotEnableUserAccounts() {
        Application app = Application.builder().id(1L).userId(10L).status(ApplicationStatus.PENDING_INITIAL_REVIEW).build();

        when(applicationRepository.findAllById(List.of(1L))).thenReturn(List.of(app));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        applicationService.batchReject(List.of(1L));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void batchReject_shouldHandleEmptyResultGracefully() {
        when(applicationRepository.findAllById(List.of(999L))).thenReturn(List.of());

        applicationService.batchReject(List.of(999L));

        verify(applicationRepository, never()).save(any(Application.class));
    }

    // --- batchNotifyInterview tests ---

    @Test
    void batchNotifyInterview_shouldSetStatusToAiInterviewInProgress() {
        Application app1 = Application.builder().id(1L).userId(10L).status(ApplicationStatus.INITIAL_REVIEW_PASSED).build();
        Application app2 = Application.builder().id(2L).userId(20L).status(ApplicationStatus.INITIAL_REVIEW_PASSED).build();

        when(applicationRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(app1, app2));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        applicationService.batchNotifyInterview(List.of(1L, 2L));

        assertEquals(ApplicationStatus.AI_INTERVIEW_IN_PROGRESS, app1.getStatus());
        assertEquals(ApplicationStatus.AI_INTERVIEW_IN_PROGRESS, app2.getStatus());
        verify(applicationRepository, times(2)).save(any(Application.class));
    }

    @Test
    void batchNotifyInterview_shouldSkipNonPassedApplications() {
        Application passed = Application.builder().id(1L).userId(10L).status(ApplicationStatus.INITIAL_REVIEW_PASSED).build();
        Application pending = Application.builder().id(2L).userId(20L).status(ApplicationStatus.PENDING_INITIAL_REVIEW).build();
        Application rejected = Application.builder().id(3L).userId(30L).status(ApplicationStatus.REJECTED).build();

        when(applicationRepository.findAllById(List.of(1L, 2L, 3L))).thenReturn(List.of(passed, pending, rejected));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        applicationService.batchNotifyInterview(List.of(1L, 2L, 3L));

        assertEquals(ApplicationStatus.AI_INTERVIEW_IN_PROGRESS, passed.getStatus());
        assertEquals(ApplicationStatus.PENDING_INITIAL_REVIEW, pending.getStatus());
        assertEquals(ApplicationStatus.REJECTED, rejected.getStatus());
        verify(applicationRepository, times(1)).save(any(Application.class));
    }

    @Test
    void batchNotifyInterview_shouldHandleEmptyResultGracefully() {
        when(applicationRepository.findAllById(List.of(999L))).thenReturn(List.of());

        applicationService.batchNotifyInterview(List.of(999L));

        verify(applicationRepository, never()).save(any(Application.class));
    }

    // --- exportToExcel tests ---

    @Test
    void exportToExcel_shouldReturnValidExcelBytesForAllApplications() throws Exception {
        Application app1 = Application.builder()
                .id(1L).userId(10L).status(ApplicationStatus.PENDING_INITIAL_REVIEW)
                .entryType(EntryType.REGISTRATION).pollenUid("12345")
                .birthDate(LocalDate.of(2000, 1, 1)).calculatedAge(25)
                .educationStage(EducationStage.UNIVERSITY).examFlag(false)
                .weeklyAvailableDays(5).dailyAvailableHours(BigDecimal.valueOf(4))
                .screeningPassed(true).needsAttention(false)
                .build();
        Application app2 = Application.builder()
                .id(2L).userId(20L).status(ApplicationStatus.REJECTED)
                .entryType(EntryType.PUBLIC_LINK).pollenUid("67890")
                .birthDate(LocalDate.of(2005, 6, 15)).calculatedAge(19)
                .educationStage(EducationStage.HIGH_SCHOOL).examFlag(true)
                .screeningPassed(false).screeningRejectReason("年龄不符")
                .needsAttention(true)
                .build();

        when(applicationRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(app1, app2));

        byte[] result = applicationService.exportToExcel(null);

        assertNotNull(result);
        assertTrue(result.length > 0);

        // Verify it's a valid Excel file by reading it back
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook =
                     new org.apache.poi.xssf.usermodel.XSSFWorkbook(new java.io.ByteArrayInputStream(result))) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(0);
            assertEquals("报名数据", sheet.getSheetName());
            // Header + 2 data rows
            assertEquals(2, sheet.getLastRowNum());
            // Verify header
            assertEquals("ID", sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals("状态", sheet.getRow(0).getCell(2).getStringCellValue());
            // Verify data row 1
            assertEquals(1.0, sheet.getRow(1).getCell(0).getNumericCellValue());
            assertEquals("PENDING_INITIAL_REVIEW", sheet.getRow(1).getCell(2).getStringCellValue());
            assertEquals("12345", sheet.getRow(1).getCell(4).getStringCellValue());
            // Verify data row 2
            assertEquals(2.0, sheet.getRow(2).getCell(0).getNumericCellValue());
            assertEquals("REJECTED", sheet.getRow(2).getCell(2).getStringCellValue());
            assertEquals("年龄不符", sheet.getRow(2).getCell(14).getStringCellValue());
        }
    }

    @Test
    void exportToExcel_withStatusFilter_shouldQueryByStatus() throws Exception {
        Application app = Application.builder()
                .id(1L).userId(10L).status(ApplicationStatus.PENDING_INITIAL_REVIEW)
                .entryType(EntryType.REGISTRATION).pollenUid("12345")
                .screeningPassed(true).needsAttention(false).examFlag(false)
                .build();

        when(applicationRepository.findByStatusOrderByCreatedAtDesc(ApplicationStatus.PENDING_INITIAL_REVIEW))
                .thenReturn(List.of(app));

        byte[] result = applicationService.exportToExcel(ApplicationStatus.PENDING_INITIAL_REVIEW);

        assertNotNull(result);
        assertTrue(result.length > 0);
        verify(applicationRepository).findByStatusOrderByCreatedAtDesc(ApplicationStatus.PENDING_INITIAL_REVIEW);
        verify(applicationRepository, never()).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void exportToExcel_withEmptyData_shouldReturnExcelWithHeaderOnly() throws Exception {
        when(applicationRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

        byte[] result = applicationService.exportToExcel(null);

        assertNotNull(result);
        assertTrue(result.length > 0);

        try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook =
                     new org.apache.poi.xssf.usermodel.XSSFWorkbook(new java.io.ByteArrayInputStream(result))) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(0);
            // Only header row
            assertEquals(0, sheet.getLastRowNum());
            assertEquals("ID", sheet.getRow(0).getCell(0).getStringCellValue());
        }
    }

    @Test
    void exportToExcel_shouldHandleNullFieldsGracefully() throws Exception {
        Application app = Application.builder()
                .id(1L).userId(10L).status(ApplicationStatus.PENDING_INITIAL_REVIEW)
                .entryType(EntryType.REGISTRATION)
                // All optional fields are null
                .build();

        when(applicationRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(app));

        byte[] result = applicationService.exportToExcel(null);

        assertNotNull(result);
        assertTrue(result.length > 0);

        try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook =
                     new org.apache.poi.xssf.usermodel.XSSFWorkbook(new java.io.ByteArrayInputStream(result))) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(0);
            assertEquals(1, sheet.getLastRowNum());
            // Null pollenUid should be empty string
            assertEquals("", sheet.getRow(1).getCell(4).getStringCellValue());
            // Null birthDate should be empty string
            assertEquals("", sheet.getRow(1).getCell(5).getStringCellValue());
        }
    }

    // --- Timeline integration tests ---

    @Test
    void createFromRegistration_shouldRecordTimelineEventOnSuccess() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(User.builder().id(1L).build()));
        when(applicationRepository.existsByUserIdAndStatusIn(eq(1L), anyList())).thenReturn(false);
        when(applicationScreeningService.autoScreen(any())).thenReturn(passedScreeningResult);
        when(applicationScreeningService.calculateAge(any())).thenReturn(24);
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> {
            Application a = inv.getArgument(0);
            a.setId(100L);
            return a;
        });

        applicationService.createFromRegistration(1L, 10L, validFormData);

        verify(applicationTimelineService).recordTimelineEvent(
                eq(100L),
                eq("PENDING_INITIAL_REVIEW"),
                eq("系统"),
                contains("等待初审"));
    }

    @Test
    void createFromRegistration_shouldRecordTimelineEventOnAutoRejection() {
        ScreeningResult rejected = ScreeningResult.builder()
                .passed(false).needsAttention(false)
                .rejectReason("年龄不满18岁").attentionFlags(List.of()).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(User.builder().id(1L).build()));
        when(applicationRepository.existsByUserIdAndStatusIn(eq(1L), anyList())).thenReturn(false);
        when(applicationScreeningService.autoScreen(any())).thenReturn(rejected);
        when(applicationScreeningService.calculateAge(any())).thenReturn(17);
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> {
            Application a = inv.getArgument(0);
            a.setId(101L);
            return a;
        });

        applicationService.createFromRegistration(1L, 10L, validFormData);

        verify(applicationTimelineService).recordTimelineEvent(
                eq(101L),
                eq("AUTO_REJECTED"),
                eq("系统"),
                contains("自动筛选拒绝"));
    }

    @Test
    void initialReview_approve_shouldRecordTimelineEvent() {
        Application app = Application.builder()
                .id(5L).userId(1L).status(ApplicationStatus.PENDING_INITIAL_REVIEW).build();
        User user = User.builder().id(1L).enabled(false).build();

        when(applicationRepository.findById(5L)).thenReturn(Optional.of(app));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(applicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        applicationService.initialReview(5L, true);

        verify(applicationTimelineService).recordTimelineEvent(
                eq(5L),
                eq("INITIAL_REVIEW_PASSED"),
                eq("审核人员"),
                contains("初审通过"));
    }

    @Test
    void initialReview_reject_shouldRecordTimelineEvent() {
        Application app = Application.builder()
                .id(6L).userId(1L).status(ApplicationStatus.PENDING_INITIAL_REVIEW).build();

        when(applicationRepository.findById(6L)).thenReturn(Optional.of(app));
        when(applicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        applicationService.initialReview(6L, false);

        verify(applicationTimelineService).recordTimelineEvent(
                eq(6L),
                eq("REJECTED"),
                eq("审核人员"),
                contains("初审拒绝"));
    }

    @Test
    void batchApprove_shouldRecordTimelineForEachApprovedApplication() {
        Application app1 = Application.builder()
                .id(1L).userId(10L).status(ApplicationStatus.PENDING_INITIAL_REVIEW).build();
        Application app2 = Application.builder()
                .id(2L).userId(20L).status(ApplicationStatus.PENDING_INITIAL_REVIEW).build();

        when(applicationRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(app1, app2));
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(User.builder().id(10L).build()));
        when(applicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        applicationService.batchApprove(List.of(1L, 2L));

        verify(applicationTimelineService, times(2)).recordTimelineEvent(
                anyLong(),
                eq("INITIAL_REVIEW_PASSED"),
                eq("审核人员"),
                contains("批量初审通过"));
    }

    @Test
    void batchReject_shouldRecordTimelineForEachRejectedApplication() {
        Application app1 = Application.builder()
                .id(1L).userId(10L).status(ApplicationStatus.PENDING_INITIAL_REVIEW).build();

        when(applicationRepository.findAllById(List.of(1L))).thenReturn(List.of(app1));
        when(applicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        applicationService.batchReject(List.of(1L));

        verify(applicationTimelineService).recordTimelineEvent(
                eq(1L),
                eq("REJECTED"),
                eq("审核人员"),
                contains("批量初审拒绝"));
    }

    @Test
    void batchNotifyInterview_shouldRecordTimelineForEachNotifiedApplication() {
        Application app1 = Application.builder()
                .id(1L).userId(10L).status(ApplicationStatus.INITIAL_REVIEW_PASSED).build();

        when(applicationRepository.findAllById(List.of(1L))).thenReturn(List.of(app1));
        when(applicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        applicationService.batchNotifyInterview(List.of(1L));

        verify(applicationTimelineService).recordTimelineEvent(
                eq(1L),
                eq("AI_INTERVIEW_IN_PROGRESS"),
                eq("审核人员"),
                contains("AI面试通知"));
    }

    @Test
    void getTimeline_shouldReturnTimelineFromService() {
        Application app = Application.builder().id(1L).userId(10L).build();
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));

        ApplicationTimeline t1 = ApplicationTimeline.builder()
                .id(1L).applicationId(1L).status("PENDING_INITIAL_REVIEW")
                .operator("系统").description("申请已提交").build();
        when(applicationTimelineService.getTimeline(1L)).thenReturn(List.of(t1));

        java.util.List<ApplicationTimeline> result = applicationService.getTimeline(1L);

        assertEquals(1, result.size());
        assertEquals("PENDING_INITIAL_REVIEW", result.get(0).getStatus());
    }

    @Test
    void getTimeline_shouldThrowWhenApplicationNotFound() {
        when(applicationRepository.findById(999L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> applicationService.getTimeline(999L));
        assertEquals(404, ex.getCode());
    }
}
