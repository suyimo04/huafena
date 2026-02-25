package com.pollen.management.controller;

import com.pollen.management.dto.*;
import com.pollen.management.entity.Internship;
import com.pollen.management.entity.InternshipTask;
import com.pollen.management.entity.User;
import com.pollen.management.entity.enums.InternshipStatus;
import com.pollen.management.entity.enums.Role;
import com.pollen.management.repository.InternshipRepository;
import com.pollen.management.repository.InternshipTaskRepository;
import com.pollen.management.repository.UserRepository;
import com.pollen.management.service.InternshipService;
import com.pollen.management.util.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InternshipControllerTest {

    @Mock private InternshipService internshipService;
    @Mock private InternshipRepository internshipRepository;
    @Mock private InternshipTaskRepository internshipTaskRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private InternshipController controller;

    // --- POST /api/internships ---

    @Test
    void createInternship_shouldDelegateToServiceAndReturnSuccess() {
        Internship internship = Internship.builder()
                .id(1L).userId(10L)
                .startDate(LocalDate.now())
                .expectedEndDate(LocalDate.now().plusDays(30))
                .status(InternshipStatus.IN_PROGRESS)
                .build();
        when(internshipService.createForNewIntern(10L)).thenReturn(internship);

        ApiResponse<Internship> response = controller.createInternship(Map.of("userId", 10L));

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData().getUserId()).isEqualTo(10L);
        verify(internshipService).createForNewIntern(10L);
    }

    // --- GET /api/internships ---

    @Test
    void listInternships_noFilter_shouldReturnAllMappedToListItems() {
        Internship i1 = Internship.builder().id(1L).userId(10L).mentorId(20L)
                .startDate(LocalDate.of(2024, 1, 1))
                .expectedEndDate(LocalDate.of(2024, 1, 31))
                .status(InternshipStatus.IN_PROGRESS).build();
        User intern = User.builder().id(10L).username("intern1").role(Role.INTERN).build();
        User mentor = User.builder().id(20L).username("mentor1").role(Role.MEMBER).build();

        when(internshipRepository.findAll()).thenReturn(List.of(i1));
        when(userRepository.findAllById(any())).thenReturn(List.of(intern, mentor));
        when(internshipTaskRepository.findByInternshipId(1L)).thenReturn(List.of(
                InternshipTask.builder().id(1L).internshipId(1L).taskName("T1").deadline(LocalDate.now()).completed(true).build(),
                InternshipTask.builder().id(2L).internshipId(1L).taskName("T2").deadline(LocalDate.now()).completed(false).build()
        ));

        ApiResponse<List<InternshipListItem>> response = controller.listInternships(null);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).hasSize(1);
        InternshipListItem item = response.getData().get(0);
        assertThat(item.getUsername()).isEqualTo("intern1");
        assertThat(item.getMentorName()).isEqualTo("mentor1");
        assertThat(item.getTaskCompletionRate()).isEqualTo(0.5);
    }

    @Test
    void listInternships_withStatusFilter_shouldFilterByStatus() {
        when(internshipRepository.findByStatus(InternshipStatus.CONVERTED)).thenReturn(List.of());

        ApiResponse<List<InternshipListItem>> response = controller.listInternships(InternshipStatus.CONVERTED);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).isEmpty();
        verify(internshipRepository).findByStatus(InternshipStatus.CONVERTED);
        verify(internshipRepository, never()).findAll();
    }

    // --- GET /api/internships/{id} ---

    @Test
    void getInternship_shouldReturnInternship() {
        Internship internship = Internship.builder().id(1L).userId(10L)
                .status(InternshipStatus.IN_PROGRESS).build();
        when(internshipService.getById(1L)).thenReturn(internship);

        ApiResponse<Internship> response = controller.getInternship(1L);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData().getId()).isEqualTo(1L);
    }

    @Test
    void getInternship_notFound_shouldPropagateException() {
        when(internshipService.getById(999L))
                .thenThrow(new BusinessException(404, "实习记录不存在"));

        assertThatThrownBy(() -> controller.getInternship(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("实习记录不存在");
    }

    // --- POST /api/internships/{id}/tasks ---

    @Test
    void createTask_shouldDelegateAndReturnTask() {
        CreateInternshipTaskRequest request = CreateInternshipTaskRequest.builder()
                .taskName("学习群规").taskDescription("熟悉花小楼群规").deadline(LocalDate.of(2024, 2, 15)).build();
        InternshipTask task = InternshipTask.builder()
                .id(1L).internshipId(1L).taskName("学习群规").completed(false).build();
        when(internshipService.createTask(1L, request)).thenReturn(task);

        ApiResponse<InternshipTask> response = controller.createTask(1L, request);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData().getTaskName()).isEqualTo("学习群规");
    }

    // --- PUT /api/internships/{id}/tasks/{taskId} ---

    @Test
    void completeTask_shouldDelegateToService() {
        ApiResponse<Void> response = controller.completeTask(1L, 2L);

        assertThat(response.getCode()).isEqualTo(200);
        verify(internshipService).completeTask(1L, 2L);
    }

    // --- GET /api/internships/{id}/tasks ---

    @Test
    void getTasks_shouldReturnTaskList() {
        Internship internship = Internship.builder().id(1L).userId(10L)
                .status(InternshipStatus.IN_PROGRESS).build();
        when(internshipService.getById(1L)).thenReturn(internship);
        List<InternshipTask> tasks = List.of(
                InternshipTask.builder().id(1L).internshipId(1L).taskName("T1").deadline(LocalDate.now()).completed(false).build()
        );
        when(internshipTaskRepository.findByInternshipId(1L)).thenReturn(tasks);

        ApiResponse<List<InternshipTask>> response = controller.getTasks(1L);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).hasSize(1);
    }

    // --- PUT /api/internships/{id}/mentor ---

    @Test
    void assignMentor_shouldDelegateToService() {
        ApiResponse<Void> response = controller.assignMentor(1L, Map.of("mentorId", 20L));

        assertThat(response.getCode()).isEqualTo(200);
        verify(internshipService).assignMentor(1L, 20L);
    }

    // --- GET /api/internships/{id}/progress ---

    @Test
    void getProgress_shouldReturnProgressData() {
        InternshipProgress progress = InternshipProgress.builder()
                .taskCompletionRate(0.75).totalPoints(85).remainingDays(10)
                .tasks(List.of()).build();
        when(internshipService.getProgress(1L)).thenReturn(progress);

        ApiResponse<InternshipProgress> response = controller.getProgress(1L);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData().getTaskCompletionRate()).isEqualTo(0.75);
        assertThat(response.getData().getRemainingDays()).isEqualTo(10);
    }

    // --- POST /api/internships/{id}/convert ---

    @Test
    void approveConversion_shouldDelegateToService() {
        ApiResponse<Void> response = controller.approveConversion(1L);

        assertThat(response.getCode()).isEqualTo(200);
        verify(internshipService).approveConversion(1L);
    }

    // --- POST /api/internships/{id}/extend ---

    @Test
    void extendInternship_shouldDelegateWithDays() {
        ApiResponse<Void> response = controller.extendInternship(1L, Map.of("additionalDays", 15));

        assertThat(response.getCode()).isEqualTo(200);
        verify(internshipService).extendInternship(1L, 15);
    }

    // --- POST /api/internships/{id}/terminate ---

    @Test
    void terminateInternship_shouldDelegateToService() {
        ApiResponse<Void> response = controller.terminateInternship(1L);

        assertThat(response.getCode()).isEqualTo(200);
        verify(internshipService).terminateInternship(1L);
    }

    @Test
    void terminateInternship_invalidState_shouldPropagateException() {
        doThrow(new BusinessException(400, "只有进行中或待评估状态的实习记录才能终止"))
                .when(internshipService).terminateInternship(1L);

        assertThatThrownBy(() -> controller.terminateInternship(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("只有进行中或待评估状态的实习记录才能终止");
    }
}
