package com.pollen.management.service;

import com.pollen.management.dto.MemberCardItem;
import com.pollen.management.dto.MemberDetail;
import com.pollen.management.dto.RoleChangeRecord;
import com.pollen.management.dto.WeeklyActivityHour;
import com.pollen.management.entity.RoleChangeHistory;
import com.pollen.management.entity.User;
import com.pollen.management.entity.UserActivityLog;
import com.pollen.management.entity.enums.OnlineStatus;
import com.pollen.management.entity.enums.Role;
import com.pollen.management.repository.RoleChangeHistoryRepository;
import com.pollen.management.repository.UserActivityLogRepository;
import com.pollen.management.repository.UserRepository;
import com.pollen.management.util.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserActivityLogRepository userActivityLogRepository;

    @Mock
    private RoleChangeHistoryRepository roleChangeHistoryRepository;

    @InjectMocks
    private MemberServiceImpl memberService;

    // --- listMembers tests ---

    @Test
    void listMembers_shouldReturnOnlyEnabledUsers() {
        User enabled = User.builder().id(1L).username("admin").role(Role.ADMIN)
                .enabled(true).onlineStatus(OnlineStatus.ONLINE).build();
        User disabled = User.builder().id(2L).username("applicant").role(Role.APPLICANT)
                .enabled(false).onlineStatus(OnlineStatus.OFFLINE).build();
        when(userRepository.findAll()).thenReturn(List.of(enabled, disabled));

        List<MemberCardItem> result = memberService.listMembers();

        assertEquals(1, result.size());
        assertEquals("admin", result.get(0).getUsername());
        assertEquals(Role.ADMIN, result.get(0).getRole());
        assertEquals(OnlineStatus.ONLINE, result.get(0).getOnlineStatus());
    }

    @Test
    void listMembers_shouldReturnEmptyListWhenNoEnabledUsers() {
        User disabled = User.builder().id(1L).username("user1").role(Role.APPLICANT)
                .enabled(false).onlineStatus(OnlineStatus.OFFLINE).build();
        when(userRepository.findAll()).thenReturn(List.of(disabled));

        List<MemberCardItem> result = memberService.listMembers();

        assertTrue(result.isEmpty());
    }

    @Test
    void listMembers_shouldMapAllFields() {
        User user = User.builder().id(5L).username("leader").role(Role.LEADER)
                .enabled(true).onlineStatus(OnlineStatus.BUSY).build();
        when(userRepository.findAll()).thenReturn(List.of(user));

        List<MemberCardItem> result = memberService.listMembers();

        assertEquals(1, result.size());
        MemberCardItem card = result.get(0);
        assertEquals(5L, card.getId());
        assertEquals("leader", card.getUsername());
        assertEquals(Role.LEADER, card.getRole());
        assertEquals(OnlineStatus.BUSY, card.getOnlineStatus());
    }

    // --- getMemberDetail tests ---

    @Test
    void getMemberDetail_shouldReturnDetailWithActivityAndHistory() {
        User user = User.builder().id(1L).username("member1").role(Role.MEMBER)
                .enabled(true).onlineStatus(OnlineStatus.ONLINE)
                .lastActiveAt(LocalDateTime.of(2024, 1, 15, 10, 0))
                .createdAt(LocalDateTime.of(2024, 1, 1, 0, 0))
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userActivityLogRepository.findByUserIdAndActionTimeBetween(eq(1L), any(), any()))
                .thenReturn(Collections.emptyList());
        when(roleChangeHistoryRepository.findByUserIdOrderByChangedAtDesc(1L))
                .thenReturn(Collections.emptyList());

        MemberDetail detail = memberService.getMemberDetail(1L);

        assertEquals(1L, detail.getId());
        assertEquals("member1", detail.getUsername());
        assertEquals(Role.MEMBER, detail.getRole());
        assertEquals(OnlineStatus.ONLINE, detail.getOnlineStatus());
        assertNotNull(detail.getActivityHours());
        assertNotNull(detail.getRoleHistory());
    }

    @Test
    void getMemberDetail_shouldThrowWhenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> memberService.getMemberDetail(99L));
        assertEquals(404, ex.getCode());
    }

    // --- getActivityHours tests ---

    @Test
    void getActivityHours_shouldReturnFourWeeks() {
        User user = User.builder().id(1L).username("user1").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userActivityLogRepository.findByUserIdAndActionTimeBetween(eq(1L), any(), any()))
                .thenReturn(Collections.emptyList());

        List<WeeklyActivityHour> result = memberService.getActivityHours(1L);

        assertEquals(4, result.size());
        for (WeeklyActivityHour wah : result) {
            assertNotNull(wah.getWeekStart());
            assertNotNull(wah.getWeekEnd());
            assertEquals(0, wah.getTotalMinutes());
        }
    }

    @Test
    void getActivityHours_shouldSumDurationMinutes() {
        User user = User.builder().id(1L).username("user1").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserActivityLog log1 = UserActivityLog.builder().userId(1L).durationMinutes(30).build();
        UserActivityLog log2 = UserActivityLog.builder().userId(1L).durationMinutes(45).build();

        // Return logs for the first week query, empty for others
        when(userActivityLogRepository.findByUserIdAndActionTimeBetween(eq(1L), any(), any()))
                .thenReturn(List.of(log1, log2))
                .thenReturn(Collections.emptyList())
                .thenReturn(Collections.emptyList())
                .thenReturn(Collections.emptyList());

        List<WeeklyActivityHour> result = memberService.getActivityHours(1L);

        assertEquals(75, result.get(0).getTotalMinutes());
    }

    @Test
    void getActivityHours_shouldHandleNullDuration() {
        User user = User.builder().id(1L).username("user1").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserActivityLog logWithNull = UserActivityLog.builder().userId(1L).durationMinutes(null).build();
        UserActivityLog logWithValue = UserActivityLog.builder().userId(1L).durationMinutes(20).build();

        when(userActivityLogRepository.findByUserIdAndActionTimeBetween(eq(1L), any(), any()))
                .thenReturn(List.of(logWithNull, logWithValue))
                .thenReturn(Collections.emptyList())
                .thenReturn(Collections.emptyList())
                .thenReturn(Collections.emptyList());

        List<WeeklyActivityHour> result = memberService.getActivityHours(1L);

        assertEquals(20, result.get(0).getTotalMinutes());
    }

    @Test
    void getActivityHours_shouldThrowWhenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> memberService.getActivityHours(99L));
        assertEquals(404, ex.getCode());
    }

    // --- getRoleHistory tests ---

    @Test
    void getRoleHistory_shouldReturnMappedRecords() {
        User user = User.builder().id(1L).username("user1").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        RoleChangeHistory history = RoleChangeHistory.builder()
                .id(10L).userId(1L).oldRole(Role.INTERN).newRole(Role.MEMBER)
                .changedBy("admin").changedAt(LocalDateTime.of(2024, 6, 1, 12, 0))
                .build();
        when(roleChangeHistoryRepository.findByUserIdOrderByChangedAtDesc(1L))
                .thenReturn(List.of(history));

        List<RoleChangeRecord> result = memberService.getRoleHistory(1L);

        assertEquals(1, result.size());
        RoleChangeRecord record = result.get(0);
        assertEquals(10L, record.getId());
        assertEquals(Role.INTERN, record.getOldRole());
        assertEquals(Role.MEMBER, record.getNewRole());
        assertEquals("admin", record.getChangedBy());
    }

    @Test
    void getRoleHistory_shouldReturnEmptyListWhenNoHistory() {
        User user = User.builder().id(1L).username("user1").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(roleChangeHistoryRepository.findByUserIdOrderByChangedAtDesc(1L))
                .thenReturn(Collections.emptyList());

        List<RoleChangeRecord> result = memberService.getRoleHistory(1L);

        assertTrue(result.isEmpty());
    }

    @Test
    void getRoleHistory_shouldThrowWhenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> memberService.getRoleHistory(99L));
        assertEquals(404, ex.getCode());
    }

    // --- updateOnlineStatus tests ---

    @Test
    void updateOnlineStatus_shouldSetStatusAndUpdateLastActive() {
        User user = User.builder().id(1L).username("user1")
                .onlineStatus(OnlineStatus.OFFLINE).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        memberService.updateOnlineStatus(1L, OnlineStatus.ONLINE);

        assertEquals(OnlineStatus.ONLINE, user.getOnlineStatus());
        assertNotNull(user.getLastActiveAt());
        verify(userRepository).save(user);
    }

    @Test
    void updateOnlineStatus_shouldSetBusyStatus() {
        User user = User.builder().id(1L).username("user1")
                .onlineStatus(OnlineStatus.ONLINE).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        memberService.updateOnlineStatus(1L, OnlineStatus.BUSY);

        assertEquals(OnlineStatus.BUSY, user.getOnlineStatus());
        verify(userRepository).save(user);
    }

    @Test
    void updateOnlineStatus_shouldThrowWhenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> memberService.updateOnlineStatus(99L, OnlineStatus.ONLINE));
        assertEquals(404, ex.getCode());
    }

    // --- heartbeat tests ---

    @Test
    void heartbeat_shouldUpdateLastActiveAndKeepOnlineStatus() {
        User user = User.builder().id(1L).username("user1")
                .onlineStatus(OnlineStatus.ONLINE).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        memberService.heartbeat(1L);

        assertEquals(OnlineStatus.ONLINE, user.getOnlineStatus());
        assertNotNull(user.getLastActiveAt());
        verify(userRepository).save(user);
    }

    @Test
    void heartbeat_shouldSetOnlineIfCurrentlyOffline() {
        User user = User.builder().id(1L).username("user1")
                .onlineStatus(OnlineStatus.OFFLINE).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        memberService.heartbeat(1L);

        assertEquals(OnlineStatus.ONLINE, user.getOnlineStatus());
        assertNotNull(user.getLastActiveAt());
    }

    @Test
    void heartbeat_shouldKeepBusyStatusIfCurrentlyBusy() {
        User user = User.builder().id(1L).username("user1")
                .onlineStatus(OnlineStatus.BUSY).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        memberService.heartbeat(1L);

        assertEquals(OnlineStatus.BUSY, user.getOnlineStatus());
        assertNotNull(user.getLastActiveAt());
    }

    @Test
    void heartbeat_shouldThrowWhenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> memberService.heartbeat(99L));
        assertEquals(404, ex.getCode());
    }

    // --- checkAndUpdateOfflineStatus tests ---

    @Test
    void checkAndUpdateOfflineStatus_shouldSetInactiveUsersToOffline() {
        User onlineInactive = User.builder().id(1L).username("user1")
                .onlineStatus(OnlineStatus.ONLINE)
                .lastActiveAt(LocalDateTime.now().minusMinutes(45))
                .build();
        User busyInactive = User.builder().id(2L).username("user2")
                .onlineStatus(OnlineStatus.BUSY)
                .lastActiveAt(LocalDateTime.now().minusMinutes(60))
                .build();

        when(userRepository.findByOnlineStatusNotAndLastActiveAtBefore(
                eq(OnlineStatus.OFFLINE), any(LocalDateTime.class)))
                .thenReturn(List.of(onlineInactive, busyInactive));

        memberService.checkAndUpdateOfflineStatus();

        assertEquals(OnlineStatus.OFFLINE, onlineInactive.getOnlineStatus());
        assertEquals(OnlineStatus.OFFLINE, busyInactive.getOnlineStatus());
        verify(userRepository, times(2)).save(any(User.class));
    }

    @Test
    void checkAndUpdateOfflineStatus_shouldDoNothingWhenNoInactiveUsers() {
        when(userRepository.findByOnlineStatusNotAndLastActiveAtBefore(
                eq(OnlineStatus.OFFLINE), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        memberService.checkAndUpdateOfflineStatus();

        verify(userRepository, never()).save(any(User.class));
    }
}
