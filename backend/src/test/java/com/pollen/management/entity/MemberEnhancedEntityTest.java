package com.pollen.management.entity;

import com.pollen.management.entity.enums.OnlineStatus;
import com.pollen.management.entity.enums.Role;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class MemberEnhancedEntityTest {

    // --- User online_status and last_active_at ---

    @Test
    void userBuilderShouldDefaultOnlineStatusToOffline() {
        User user = User.builder()
                .username("testuser")
                .password("pass")
                .role(Role.MEMBER)
                .build();

        assertEquals(OnlineStatus.OFFLINE, user.getOnlineStatus());
        assertNull(user.getLastActiveAt());
    }

    @Test
    void userShouldAcceptOnlineStatusAndLastActiveAt() {
        LocalDateTime now = LocalDateTime.now();
        User user = User.builder()
                .username("testuser")
                .password("pass")
                .role(Role.MEMBER)
                .onlineStatus(OnlineStatus.ONLINE)
                .lastActiveAt(now)
                .build();

        assertEquals(OnlineStatus.ONLINE, user.getOnlineStatus());
        assertEquals(now, user.getLastActiveAt());
    }

    @Test
    void onlineStatusEnumShouldContainAllValues() {
        OnlineStatus[] values = OnlineStatus.values();
        assertEquals(3, values.length);
        assertNotNull(OnlineStatus.valueOf("ONLINE"));
        assertNotNull(OnlineStatus.valueOf("BUSY"));
        assertNotNull(OnlineStatus.valueOf("OFFLINE"));
    }

    // --- UserActivityLog ---

    @Test
    void userActivityLogBuilderShouldCreateWithDefaults() {
        UserActivityLog log = UserActivityLog.builder()
                .userId(1L)
                .actionType("LOGIN")
                .build();

        assertNull(log.getId());
        assertEquals(1L, log.getUserId());
        assertEquals("LOGIN", log.getActionType());
        assertNull(log.getDurationMinutes());
    }

    @Test
    void userActivityLogBuilderShouldAcceptAllFields() {
        LocalDateTime time = LocalDateTime.of(2024, 6, 15, 10, 30);
        UserActivityLog log = UserActivityLog.builder()
                .id(5L)
                .userId(1L)
                .actionType("PAGE_VIEW")
                .actionTime(time)
                .durationMinutes(45)
                .build();

        assertEquals(5L, log.getId());
        assertEquals(1L, log.getUserId());
        assertEquals("PAGE_VIEW", log.getActionType());
        assertEquals(time, log.getActionTime());
        assertEquals(45, log.getDurationMinutes());
    }

    // --- RoleChangeHistory ---

    @Test
    void roleChangeHistoryBuilderShouldCreateCorrectly() {
        RoleChangeHistory history = RoleChangeHistory.builder()
                .userId(1L)
                .oldRole(Role.INTERN)
                .newRole(Role.MEMBER)
                .changedBy("admin")
                .build();

        assertNull(history.getId());
        assertEquals(1L, history.getUserId());
        assertEquals(Role.INTERN, history.getOldRole());
        assertEquals(Role.MEMBER, history.getNewRole());
        assertEquals("admin", history.getChangedBy());
    }

    @Test
    void roleChangeHistoryBuilderShouldAcceptAllFields() {
        LocalDateTime time = LocalDateTime.of(2024, 6, 15, 14, 0);
        RoleChangeHistory history = RoleChangeHistory.builder()
                .id(10L)
                .userId(2L)
                .oldRole(Role.MEMBER)
                .newRole(Role.INTERN)
                .changedBy("leader")
                .changedAt(time)
                .build();

        assertEquals(10L, history.getId());
        assertEquals(2L, history.getUserId());
        assertEquals(Role.MEMBER, history.getOldRole());
        assertEquals(Role.INTERN, history.getNewRole());
        assertEquals("leader", history.getChangedBy());
        assertEquals(time, history.getChangedAt());
    }
}
