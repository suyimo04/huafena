package com.pollen.management.service;

import com.pollen.management.dto.CreateInternshipTaskRequest;
import com.pollen.management.dto.InternshipProgress;
import com.pollen.management.entity.Internship;
import com.pollen.management.entity.InternshipTask;
import com.pollen.management.entity.User;
import com.pollen.management.entity.enums.InternshipStatus;
import com.pollen.management.entity.enums.Role;
import com.pollen.management.repository.InternshipRepository;
import com.pollen.management.repository.InternshipTaskRepository;
import com.pollen.management.repository.UserRepository;
import com.pollen.management.util.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InternshipServiceImplTest {

    @Mock
    private InternshipRepository internshipRepository;

    @Mock
    private InternshipTaskRepository internshipTaskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PointsService pointsService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private InternshipServiceImpl internshipService;

    // --- createForNewIntern tests ---

    @Test
    void createForNewIntern_shouldCreateWithDefaults() {
        User user = User.builder().id(1L).username("intern1").role(Role.INTERN).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(internshipRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(internshipRepository.save(any(Internship.class))).thenAnswer(inv -> {
            Internship i = inv.getArgument(0);
            i.setId(1L);
            return i;
        });

        Internship result = internshipService.createForNewIntern(1L);

        assertEquals(1L, result.getUserId());
        assertEquals(LocalDate.now(), result.getStartDate());
        assertEquals(LocalDate.now().plusDays(30), result.getExpectedEndDate());
        assertEquals(InternshipStatus.IN_PROGRESS, result.getStatus());
        assertNull(result.getMentorId());
    }

    @Test
    void createForNewIntern_shouldRejectIfUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> internshipService.createForNewIntern(99L));
        assertEquals(404, ex.getCode());
    }

    @Test
    void createForNewIntern_shouldRejectIfActiveInternshipExists() {
        User user = User.builder().id(1L).username("intern1").role(Role.INTERN).build();
        Internship existing = Internship.builder().id(1L).userId(1L).status(InternshipStatus.IN_PROGRESS).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(internshipRepository.findByUserId(1L)).thenReturn(Optional.of(existing));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> internshipService.createForNewIntern(1L));
        assertEquals(409, ex.getCode());
    }

    // --- getById tests ---

    @Test
    void getById_shouldReturnInternship() {
        Internship internship = Internship.builder().id(1L).userId(1L).status(InternshipStatus.IN_PROGRESS).build();
        when(internshipRepository.findById(1L)).thenReturn(Optional.of(internship));

        Internship result = internshipService.getById(1L);
        assertEquals(1L, result.getId());
    }

    @Test
    void getById_shouldThrowIfNotFound() {
        when(internshipRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> internshipService.getById(99L));
        assertEquals(404, ex.getCode());
    }

    // --- createTask tests ---

    @Test
    void createTask_shouldCreateTaskForInProgressInternship() {
        Internship internship = Internship.builder().id(1L).userId(1L).status(InternshipStatus.IN_PROGRESS).build();
        when(internshipRepository.findById(1L)).thenReturn(Optional.of(internship));
        when(internshipTaskRepository.save(any(InternshipTask.class))).thenAnswer(inv -> {
            InternshipTask t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        CreateInternshipTaskRequest request = CreateInternshipTaskRequest.builder()
                .taskName("学习群规")
                .taskDescription("熟悉花小楼群规")
                .deadline(LocalDate.now().plusDays(7))
                .build();

        InternshipTask result = internshipService.createTask(1L, request);

        assertEquals(1L, result.getInternshipId());
        assertEquals("学习群规", result.getTaskName());
        assertEquals("熟悉花小楼群规", result.getTaskDescription());
        assertFalse(result.getCompleted());
    }

    @Test
    void createTask_shouldRejectIfInternshipNotInProgress() {
        Internship internship = Internship.builder().id(1L).userId(1L).status(InternshipStatus.CONVERTED).build();
        when(internshipRepository.findById(1L)).thenReturn(Optional.of(internship));

        CreateInternshipTaskRequest request = CreateInternshipTaskRequest.builder()
                .taskName("任务").deadline(LocalDate.now().plusDays(7)).build();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> internshipService.createTask(1L, request));
        assertEquals(400, ex.getCode());
    }

    // --- completeTask tests ---

    @Test
    void completeTask_shouldMarkTaskAsCompleted() {
        Internship internship = Internship.builder().id(1L).userId(1L).status(InternshipStatus.IN_PROGRESS).build();
        InternshipTask task = InternshipTask.builder().id(10L).internshipId(1L).completed(false).build();
        when(internshipRepository.findById(1L)).thenReturn(Optional.of(internship));
        when(internshipTaskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(internshipTaskRepository.save(any(InternshipTask.class))).thenAnswer(inv -> inv.getArgument(0));

        internshipService.completeTask(1L, 10L);

        assertTrue(task.getCompleted());
        assertNotNull(task.getCompletedAt());
    }

    @Test
    void completeTask_shouldRejectIfTaskNotFound() {
        Internship internship = Internship.builder().id(1L).userId(1L).status(InternshipStatus.IN_PROGRESS).build();
        when(internshipRepository.findById(1L)).thenReturn(Optional.of(internship));
        when(internshipTaskRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> internshipService.completeTask(1L, 99L));
        assertEquals(404, ex.getCode());
    }

    @Test
    void completeTask_shouldRejectIfTaskBelongsToDifferentInternship() {
        Internship internship = Internship.builder().id(1L).userId(1L).status(InternshipStatus.IN_PROGRESS).build();
        InternshipTask task = InternshipTask.builder().id(10L).internshipId(2L).completed(false).build();
        when(internshipRepository.findById(1L)).thenReturn(Optional.of(internship));
        when(internshipTaskRepository.findById(10L)).thenReturn(Optional.of(task));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> internshipService.completeTask(1L, 10L));
        assertEquals(400, ex.getCode());
    }

    @Test
    void completeTask_shouldRejectIfAlreadyCompleted() {
        Internship internship = Internship.builder().id(1L).userId(1L).status(InternshipStatus.IN_PROGRESS).build();
        InternshipTask task = InternshipTask.builder().id(10L).internshipId(1L).completed(true).build();
        when(internshipRepository.findById(1L)).thenReturn(Optional.of(internship));
        when(internshipTaskRepository.findById(10L)).thenReturn(Optional.of(task));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> internshipService.completeTask(1L, 10L));
        assertEquals(400, ex.getCode());
    }

    // --- assignMentor tests ---

    @Test
    void assignMentor_shouldAssignMemberAsMentor() {
        Internship internship = Internship.builder().id(1L).userId(1L).status(InternshipStatus.IN_PROGRESS).build();
        User mentor = User.builder().id(5L).username("mentor1").role(Role.MEMBER).build();
        when(internshipRepository.findById(1L)).thenReturn(Optional.of(internship));
        when(userRepository.findById(5L)).thenReturn(Optional.of(mentor));
        when(internshipRepository.save(any(Internship.class))).thenAnswer(inv -> inv.getArgument(0));

        internshipService.assignMentor(1L, 5L);

        assertEquals(5L, internship.getMentorId());
    }

    @Test
    void assignMentor_shouldAssignViceLeaderAsMentor() {
        Internship internship = Internship.builder().id(1L).userId(1L).status(InternshipStatus.IN_PROGRESS).build();
        User mentor = User.builder().id(6L).username("vice").role(Role.VICE_LEADER).build();
        when(internshipRepository.findById(1L)).thenReturn(Optional.of(internship));
        when(userRepository.findById(6L)).thenReturn(Optional.of(mentor));
        when(internshipRepository.save(any(Internship.class))).thenAnswer(inv -> inv.getArgument(0));

        internshipService.assignMentor(1L, 6L);

        assertEquals(6L, internship.getMentorId());
    }

    @Test
    void assignMentor_shouldRejectIfMentorNotFound() {
        Internship internship = Internship.builder().id(1L).userId(1L).status(InternshipStatus.IN_PROGRESS).build();
        when(internshipRepository.findById(1L)).thenReturn(Optional.of(internship));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> internshipService.assignMentor(1L, 99L));
        assertEquals(404, ex.getCode());
    }

    @Test
    void assignMentor_shouldRejectIfMentorRoleInvalid() {
        Internship internship = Internship.builder().id(1L).userId(1L).status(InternshipStatus.IN_PROGRESS).build();
        User mentor = User.builder().id(7L).username("intern2").role(Role.INTERN).build();
        when(internshipRepository.findById(1L)).thenReturn(Optional.of(internship));
        when(userRepository.findById(7L)).thenReturn(Optional.of(mentor));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> internshipService.assignMentor(1L, 7L));
        assertEquals(400, ex.getCode());
    }

    @Test
    void assignMentor_shouldRejectLeaderAsMentor() {
        Internship internship = Internship.builder().id(1L).userId(1L).status(InternshipStatus.IN_PROGRESS).build();
        User mentor = User.builder().id(8L).username("leader").role(Role.LEADER).build();
        when(internshipRepository.findById(1L)).thenReturn(Optional.of(internship));
        when(userRepository.findById(8L)).thenReturn(Optional.of(mentor));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> internshipService.assignMentor(1L, 8L));
        assertEquals(400, ex.getCode());
    }

    // --- getProgress tests ---

    @Test
    void getProgress_shouldCalculateCorrectly() {
        Internship internship = Internship.builder()
                .id(1L).userId(1L).mentorId(5L)
                .startDate(LocalDate.now().minusDays(10))
                .expectedEndDate(LocalDate.now().plusDays(20))
                .status(InternshipStatus.IN_PROGRESS).build();
        when(internshipRepository.findById(1L)).thenReturn(Optional.of(internship));

        InternshipTask task1 = InternshipTask.builder().id(1L).internshipId(1L).completed(true).build();
        InternshipTask task2 = InternshipTask.builder().id(2L).internshipId(1L).completed(false).build();
        when(internshipTaskRepository.findByInternshipId(1L)).thenReturn(List.of(task1, task2));
        when(pointsService.getTotalPoints(1L)).thenReturn(50);

        InternshipProgress progress = internshipService.getProgress(1L);

        assertEquals(0.5, progress.getTaskCompletionRate(), 0.001);
        assertEquals(50, progress.getTotalPoints());
        assertEquals(20, progress.getRemainingDays());
        assertEquals(2, progress.getTasks().size());
    }

    @Test
    void getProgress_shouldReturnZeroRateWhenNoTasks() {
        Internship internship = Internship.builder()
                .id(1L).userId(1L)
                .startDate(LocalDate.now())
                .expectedEndDate(LocalDate.now().plusDays(30))
                .status(InternshipStatus.IN_PROGRESS).build();
        when(internshipRepository.findById(1L)).thenReturn(Optional.of(internship));
        when(internshipTaskRepository.findByInternshipId(1L)).thenReturn(List.of());
        when(pointsService.getTotalPoints(1L)).thenReturn(0);

        InternshipProgress progress = internshipService.getProgress(1L);

        assertEquals(0.0, progress.getTaskCompletionRate(), 0.001);
        assertEquals(0, progress.getTotalPoints());
        assertEquals(30, progress.getRemainingDays());
        assertTrue(progress.getTasks().isEmpty());
    }

    @Test
    void getProgress_shouldReturnZeroRemainingDaysWhenExpired() {
        Internship internship = Internship.builder()
                .id(1L).userId(1L)
                .startDate(LocalDate.now().minusDays(35))
                .expectedEndDate(LocalDate.now().minusDays(5))
                .status(InternshipStatus.IN_PROGRESS).build();
        when(internshipRepository.findById(1L)).thenReturn(Optional.of(internship));
        when(internshipTaskRepository.findByInternshipId(1L)).thenReturn(List.of());
        when(pointsService.getTotalPoints(1L)).thenReturn(100);

        InternshipProgress progress = internshipService.getProgress(1L);

        assertEquals(0, progress.getRemainingDays());
        assertEquals(100, progress.getTotalPoints());
    }

    @Test
    void getProgress_shouldCalculateFullCompletionRate() {
        Internship internship = Internship.builder()
                .id(1L).userId(1L)
                .startDate(LocalDate.now())
                .expectedEndDate(LocalDate.now().plusDays(30))
                .status(InternshipStatus.IN_PROGRESS).build();
        when(internshipRepository.findById(1L)).thenReturn(Optional.of(internship));

        InternshipTask task1 = InternshipTask.builder().id(1L).internshipId(1L).completed(true).build();
        InternshipTask task2 = InternshipTask.builder().id(2L).internshipId(1L).completed(true).build();
        InternshipTask task3 = InternshipTask.builder().id(3L).internshipId(1L).completed(true).build();
        when(internshipTaskRepository.findByInternshipId(1L)).thenReturn(List.of(task1, task2, task3));
        when(pointsService.getTotalPoints(1L)).thenReturn(200);

        InternshipProgress progress = internshipService.getProgress(1L);

        assertEquals(1.0, progress.getTaskCompletionRate(), 0.001);
    }

    // --- approveConversion tests ---

    @Test
    void approveConversion_shouldConvertInternToMember() {
        Internship internship = Internship.builder()
                .id(1L).userId(1L).status(InternshipStatus.PENDING_CONVERSION).build();
        User user = User.builder().id(1L).username("intern1").role(Role.INTERN).build();
        when(internshipRepository.findById(1L)).thenReturn(Optional.of(internship));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(internshipRepository.save(any(Internship.class))).thenAnswer(inv -> inv.getArgument(0));

        internshipService.approveConversion(1L);

        assertEquals(Role.MEMBER, user.getRole());
        assertEquals(InternshipStatus.CONVERTED, internship.getStatus());
        verify(emailService).sendTemplateEmail(eq("CONVERSION_NOTIFICATION"), anyMap(), eq("intern1"));
    }

    @Test
    void approveConversion_shouldRejectIfNotPendingConversion() {
        Internship internship = Internship.builder()
                .id(1L).userId(1L).status(InternshipStatus.IN_PROGRESS).build();
        when(internshipRepository.findById(1L)).thenReturn(Optional.of(internship));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> internshipService.approveConversion(1L));
        assertEquals(400, ex.getCode());
    }

    @Test
    void approveConversion_shouldNotFailIfEmailFails() {
        Internship internship = Internship.builder()
                .id(1L).userId(1L).status(InternshipStatus.PENDING_CONVERSION).build();
        User user = User.builder().id(1L).username("intern1").role(Role.INTERN).build();
        when(internshipRepository.findById(1L)).thenReturn(Optional.of(internship));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(internshipRepository.save(any(Internship.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("SMTP error")).when(emailService)
                .sendTemplateEmail(anyString(), anyMap(), anyString());

        assertDoesNotThrow(() -> internshipService.approveConversion(1L));
        assertEquals(Role.MEMBER, user.getRole());
        assertEquals(InternshipStatus.CONVERTED, internship.getStatus());
    }

    // --- extendInternship tests ---

    @Test
    void extendInternship_shouldExtendFromInProgress() {
        LocalDate originalEnd = LocalDate.now().plusDays(5);
        Internship internship = Internship.builder()
                .id(1L).userId(1L).status(InternshipStatus.IN_PROGRESS)
                .expectedEndDate(originalEnd).build();
        when(internshipRepository.findById(1L)).thenReturn(Optional.of(internship));
        when(internshipRepository.save(any(Internship.class))).thenAnswer(inv -> inv.getArgument(0));

        internshipService.extendInternship(1L, 15);

        assertEquals(originalEnd.plusDays(15), internship.getExpectedEndDate());
        assertEquals(InternshipStatus.IN_PROGRESS, internship.getStatus());
    }

    @Test
    void extendInternship_shouldExtendFromPendingEvaluation() {
        LocalDate originalEnd = LocalDate.now().minusDays(1);
        Internship internship = Internship.builder()
                .id(1L).userId(1L).status(InternshipStatus.PENDING_EVALUATION)
                .expectedEndDate(originalEnd).build();
        when(internshipRepository.findById(1L)).thenReturn(Optional.of(internship));
        when(internshipRepository.save(any(Internship.class))).thenAnswer(inv -> inv.getArgument(0));

        internshipService.extendInternship(1L, 30);

        assertEquals(originalEnd.plusDays(30), internship.getExpectedEndDate());
        assertEquals(InternshipStatus.IN_PROGRESS, internship.getStatus());
    }

    @Test
    void extendInternship_shouldRejectIfStatusInvalid() {
        Internship internship = Internship.builder()
                .id(1L).userId(1L).status(InternshipStatus.CONVERTED).build();
        when(internshipRepository.findById(1L)).thenReturn(Optional.of(internship));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> internshipService.extendInternship(1L, 15));
        assertEquals(400, ex.getCode());
    }

    @Test
    void extendInternship_shouldRejectIfAdditionalDaysNotPositive() {
        Internship internship = Internship.builder()
                .id(1L).userId(1L).status(InternshipStatus.IN_PROGRESS)
                .expectedEndDate(LocalDate.now().plusDays(10)).build();
        when(internshipRepository.findById(1L)).thenReturn(Optional.of(internship));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> internshipService.extendInternship(1L, 0));
        assertEquals(400, ex.getCode());
    }

    // --- terminateInternship tests ---

    @Test
    void terminateInternship_shouldTerminateFromInProgress() {
        Internship internship = Internship.builder()
                .id(1L).userId(1L).status(InternshipStatus.IN_PROGRESS).build();
        when(internshipRepository.findById(1L)).thenReturn(Optional.of(internship));
        when(internshipRepository.save(any(Internship.class))).thenAnswer(inv -> inv.getArgument(0));

        internshipService.terminateInternship(1L);

        assertEquals(InternshipStatus.TERMINATED, internship.getStatus());
    }

    @Test
    void terminateInternship_shouldTerminateFromPendingEvaluation() {
        Internship internship = Internship.builder()
                .id(1L).userId(1L).status(InternshipStatus.PENDING_EVALUATION).build();
        when(internshipRepository.findById(1L)).thenReturn(Optional.of(internship));
        when(internshipRepository.save(any(Internship.class))).thenAnswer(inv -> inv.getArgument(0));

        internshipService.terminateInternship(1L);

        assertEquals(InternshipStatus.TERMINATED, internship.getStatus());
    }

    @Test
    void terminateInternship_shouldRejectIfStatusInvalid() {
        Internship internship = Internship.builder()
                .id(1L).userId(1L).status(InternshipStatus.CONVERTED).build();
        when(internshipRepository.findById(1L)).thenReturn(Optional.of(internship));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> internshipService.terminateInternship(1L));
        assertEquals(400, ex.getCode());
    }

    // --- checkAndTriggerConversion tests ---

    @Test
    void checkAndTriggerConversion_shouldSetPendingConversionWhenRateAbove80() {
        Internship internship = Internship.builder()
                .id(1L).userId(1L).status(InternshipStatus.IN_PROGRESS)
                .expectedEndDate(LocalDate.now().minusDays(1)).build();
        when(internshipRepository.findByStatus(InternshipStatus.IN_PROGRESS))
                .thenReturn(List.of(internship));

        // 4 out of 5 completed = 80%
        List<InternshipTask> tasks = List.of(
                InternshipTask.builder().id(1L).internshipId(1L).completed(true).build(),
                InternshipTask.builder().id(2L).internshipId(1L).completed(true).build(),
                InternshipTask.builder().id(3L).internshipId(1L).completed(true).build(),
                InternshipTask.builder().id(4L).internshipId(1L).completed(true).build(),
                InternshipTask.builder().id(5L).internshipId(1L).completed(false).build()
        );
        when(internshipTaskRepository.findByInternshipId(1L)).thenReturn(tasks);
        when(internshipRepository.save(any(Internship.class))).thenAnswer(inv -> inv.getArgument(0));

        internshipService.checkAndTriggerConversion();

        assertEquals(InternshipStatus.PENDING_CONVERSION, internship.getStatus());
    }

    @Test
    void checkAndTriggerConversion_shouldSetPendingEvaluationWhenRateBelow80() {
        Internship internship = Internship.builder()
                .id(1L).userId(1L).status(InternshipStatus.IN_PROGRESS)
                .expectedEndDate(LocalDate.now().minusDays(1)).build();
        when(internshipRepository.findByStatus(InternshipStatus.IN_PROGRESS))
                .thenReturn(List.of(internship));

        // 1 out of 5 completed = 20%
        List<InternshipTask> tasks = List.of(
                InternshipTask.builder().id(1L).internshipId(1L).completed(true).build(),
                InternshipTask.builder().id(2L).internshipId(1L).completed(false).build(),
                InternshipTask.builder().id(3L).internshipId(1L).completed(false).build(),
                InternshipTask.builder().id(4L).internshipId(1L).completed(false).build(),
                InternshipTask.builder().id(5L).internshipId(1L).completed(false).build()
        );
        when(internshipTaskRepository.findByInternshipId(1L)).thenReturn(tasks);
        when(internshipRepository.save(any(Internship.class))).thenAnswer(inv -> inv.getArgument(0));

        internshipService.checkAndTriggerConversion();

        assertEquals(InternshipStatus.PENDING_EVALUATION, internship.getStatus());
    }

    @Test
    void checkAndTriggerConversion_shouldSkipNotExpiredInternships() {
        Internship internship = Internship.builder()
                .id(1L).userId(1L).status(InternshipStatus.IN_PROGRESS)
                .expectedEndDate(LocalDate.now().plusDays(10)).build();
        when(internshipRepository.findByStatus(InternshipStatus.IN_PROGRESS))
                .thenReturn(List.of(internship));

        internshipService.checkAndTriggerConversion();

        assertEquals(InternshipStatus.IN_PROGRESS, internship.getStatus());
        verify(internshipRepository, never()).save(any());
    }

    @Test
    void checkAndTriggerConversion_shouldSetPendingEvaluationWhenNoTasks() {
        Internship internship = Internship.builder()
                .id(1L).userId(1L).status(InternshipStatus.IN_PROGRESS)
                .expectedEndDate(LocalDate.now().minusDays(1)).build();
        when(internshipRepository.findByStatus(InternshipStatus.IN_PROGRESS))
                .thenReturn(List.of(internship));
        when(internshipTaskRepository.findByInternshipId(1L)).thenReturn(List.of());
        when(internshipRepository.save(any(Internship.class))).thenAnswer(inv -> inv.getArgument(0));

        internshipService.checkAndTriggerConversion();

        assertEquals(InternshipStatus.PENDING_EVALUATION, internship.getStatus());
    }

    @Test
    void checkAndTriggerConversion_shouldHandleExactly80PercentAsConversion() {
        Internship internship = Internship.builder()
                .id(1L).userId(1L).status(InternshipStatus.IN_PROGRESS)
                .expectedEndDate(LocalDate.now()).build();
        when(internshipRepository.findByStatus(InternshipStatus.IN_PROGRESS))
                .thenReturn(List.of(internship));

        // Exactly 80%: 4 out of 5
        List<InternshipTask> tasks = List.of(
                InternshipTask.builder().id(1L).internshipId(1L).completed(true).build(),
                InternshipTask.builder().id(2L).internshipId(1L).completed(true).build(),
                InternshipTask.builder().id(3L).internshipId(1L).completed(true).build(),
                InternshipTask.builder().id(4L).internshipId(1L).completed(true).build(),
                InternshipTask.builder().id(5L).internshipId(1L).completed(false).build()
        );
        when(internshipTaskRepository.findByInternshipId(1L)).thenReturn(tasks);
        when(internshipRepository.save(any(Internship.class))).thenAnswer(inv -> inv.getArgument(0));

        internshipService.checkAndTriggerConversion();

        assertEquals(InternshipStatus.PENDING_CONVERSION, internship.getStatus());
    }
}
