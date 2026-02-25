package com.pollen.management.controller;

import com.pollen.management.dto.*;
import com.pollen.management.entity.enums.OnlineStatus;
import com.pollen.management.entity.enums.Role;
import com.pollen.management.service.MemberService;
import com.pollen.management.util.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberControllerTest {

    @Mock
    private MemberService memberService;

    @InjectMocks
    private MemberController controller;

    // --- GET /api/members ---

    @Test
    void listMembers_shouldReturnMemberCardList() {
        List<MemberCardItem> members = List.of(
                MemberCardItem.builder().id(1L).username("admin").role(Role.ADMIN).onlineStatus(OnlineStatus.ONLINE).build(),
                MemberCardItem.builder().id(2L).username("leader").role(Role.LEADER).onlineStatus(OnlineStatus.BUSY).build(),
                MemberCardItem.builder().id(3L).username("member1").role(Role.MEMBER).onlineStatus(OnlineStatus.OFFLINE).build()
        );
        when(memberService.listMembers()).thenReturn(members);

        ApiResponse<List<MemberCardItem>> response = controller.listMembers();

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).hasSize(3);
        assertThat(response.getData().get(0).getUsername()).isEqualTo("admin");
        assertThat(response.getData().get(1).getOnlineStatus()).isEqualTo(OnlineStatus.BUSY);
        verify(memberService).listMembers();
    }

    @Test
    void listMembers_emptyList_shouldReturnEmptyData() {
        when(memberService.listMembers()).thenReturn(List.of());

        ApiResponse<List<MemberCardItem>> response = controller.listMembers();

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).isEmpty();
    }

    // --- GET /api/members/{id} ---

    @Test
    void getMemberDetail_shouldReturnDetailWithActivityHoursAndRoleHistory() {
        MemberDetail detail = MemberDetail.builder()
                .id(1L)
                .username("member1")
                .role(Role.MEMBER)
                .onlineStatus(OnlineStatus.ONLINE)
                .lastActiveAt(LocalDateTime.now())
                .createdAt(LocalDateTime.of(2024, 1, 1, 0, 0))
                .activityHours(List.of(
                        WeeklyActivityHour.builder().weekStart(LocalDate.of(2024, 6, 3)).weekEnd(LocalDate.of(2024, 6, 9)).totalMinutes(120).build()
                ))
                .roleHistory(List.of(
                        RoleChangeRecord.builder().id(1L).oldRole(Role.INTERN).newRole(Role.MEMBER).changedBy("leader").changedAt(LocalDateTime.of(2024, 3, 1, 10, 0)).build()
                ))
                .build();
        when(memberService.getMemberDetail(1L)).thenReturn(detail);

        ApiResponse<MemberDetail> response = controller.getMemberDetail(1L);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData().getUsername()).isEqualTo("member1");
        assertThat(response.getData().getActivityHours()).hasSize(1);
        assertThat(response.getData().getRoleHistory()).hasSize(1);
        assertThat(response.getData().getRoleHistory().get(0).getOldRole()).isEqualTo(Role.INTERN);
    }

    @Test
    void getMemberDetail_notFound_shouldPropagateException() {
        when(memberService.getMemberDetail(999L))
                .thenThrow(new BusinessException(404, "成员不存在"));

        assertThatThrownBy(() -> controller.getMemberDetail(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("成员不存在");
    }

    // --- GET /api/members/{id}/activity-hours ---

    @Test
    void getActivityHours_shouldReturnWeeklyHours() {
        List<WeeklyActivityHour> hours = List.of(
                WeeklyActivityHour.builder().weekStart(LocalDate.of(2024, 6, 3)).weekEnd(LocalDate.of(2024, 6, 9)).totalMinutes(180).build(),
                WeeklyActivityHour.builder().weekStart(LocalDate.of(2024, 5, 27)).weekEnd(LocalDate.of(2024, 6, 2)).totalMinutes(90).build()
        );
        when(memberService.getActivityHours(1L)).thenReturn(hours);

        ApiResponse<List<WeeklyActivityHour>> response = controller.getActivityHours(1L);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).hasSize(2);
        assertThat(response.getData().get(0).getTotalMinutes()).isEqualTo(180);
    }

    @Test
    void getActivityHours_memberNotFound_shouldPropagateException() {
        when(memberService.getActivityHours(999L))
                .thenThrow(new BusinessException(404, "成员不存在"));

        assertThatThrownBy(() -> controller.getActivityHours(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("成员不存在");
    }

    // --- GET /api/members/{id}/role-history ---

    @Test
    void getRoleHistory_shouldReturnRoleChangeRecords() {
        List<RoleChangeRecord> history = List.of(
                RoleChangeRecord.builder().id(1L).oldRole(Role.APPLICANT).newRole(Role.INTERN).changedBy("admin").changedAt(LocalDateTime.of(2024, 1, 15, 10, 0)).build(),
                RoleChangeRecord.builder().id(2L).oldRole(Role.INTERN).newRole(Role.MEMBER).changedBy("leader").changedAt(LocalDateTime.of(2024, 3, 1, 10, 0)).build()
        );
        when(memberService.getRoleHistory(1L)).thenReturn(history);

        ApiResponse<List<RoleChangeRecord>> response = controller.getRoleHistory(1L);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).hasSize(2);
        assertThat(response.getData().get(0).getNewRole()).isEqualTo(Role.INTERN);
        assertThat(response.getData().get(1).getNewRole()).isEqualTo(Role.MEMBER);
    }

    @Test
    void getRoleHistory_emptyHistory_shouldReturnEmptyList() {
        when(memberService.getRoleHistory(1L)).thenReturn(List.of());

        ApiResponse<List<RoleChangeRecord>> response = controller.getRoleHistory(1L);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).isEmpty();
    }

    // --- PUT /api/members/{id}/status ---

    @Test
    void updateOnlineStatus_toOnline_shouldDelegateToService() {
        UpdateOnlineStatusRequest request = new UpdateOnlineStatusRequest(OnlineStatus.ONLINE);

        ApiResponse<Void> response = controller.updateOnlineStatus(1L, request);

        assertThat(response.getCode()).isEqualTo(200);
        verify(memberService).updateOnlineStatus(1L, OnlineStatus.ONLINE);
    }

    @Test
    void updateOnlineStatus_toBusy_shouldDelegateToService() {
        UpdateOnlineStatusRequest request = new UpdateOnlineStatusRequest(OnlineStatus.BUSY);

        ApiResponse<Void> response = controller.updateOnlineStatus(2L, request);

        assertThat(response.getCode()).isEqualTo(200);
        verify(memberService).updateOnlineStatus(2L, OnlineStatus.BUSY);
    }

    @Test
    void updateOnlineStatus_toOffline_shouldDelegateToService() {
        UpdateOnlineStatusRequest request = new UpdateOnlineStatusRequest(OnlineStatus.OFFLINE);

        ApiResponse<Void> response = controller.updateOnlineStatus(3L, request);

        assertThat(response.getCode()).isEqualTo(200);
        verify(memberService).updateOnlineStatus(3L, OnlineStatus.OFFLINE);
    }

    @Test
    void updateOnlineStatus_userNotFound_shouldPropagateException() {
        doThrow(new BusinessException(404, "用户不存在"))
                .when(memberService).updateOnlineStatus(999L, OnlineStatus.ONLINE);

        UpdateOnlineStatusRequest request = new UpdateOnlineStatusRequest(OnlineStatus.ONLINE);

        assertThatThrownBy(() -> controller.updateOnlineStatus(999L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户不存在");
    }

    // --- POST /api/members/{id}/heartbeat ---

    @Test
    void heartbeat_shouldDelegateToService() {
        ApiResponse<Void> response = controller.heartbeat(1L);

        assertThat(response.getCode()).isEqualTo(200);
        verify(memberService).heartbeat(1L);
    }

    @Test
    void heartbeat_userNotFound_shouldPropagateException() {
        doThrow(new BusinessException(404, "用户不存在"))
                .when(memberService).heartbeat(999L);

        assertThatThrownBy(() -> controller.heartbeat(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户不存在");
    }
}
