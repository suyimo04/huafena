package com.pollen.management.property;

import com.pollen.management.dto.RoleChangeRecord;
import com.pollen.management.entity.RoleChangeHistory;
import com.pollen.management.entity.User;
import com.pollen.management.entity.enums.OnlineStatus;
import com.pollen.management.entity.enums.Role;
import com.pollen.management.repository.RoleChangeHistoryRepository;
import com.pollen.management.repository.UserActivityLogRepository;
import com.pollen.management.repository.UserRepository;
import com.pollen.management.service.MemberServiceImpl;
import net.jqwik.api.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for member online status transitions and role change history.
 *
 * Property 36: 成员在线状态转换
 * For any member, heartbeat sets OFFLINE→ONLINE, keeps BUSY as BUSY;
 * updateOnlineStatus sets any status; checkAndUpdateOfflineStatus sets
 * inactive users to OFFLINE.
 *
 * Property 37: 角色变更历史记录
 * For any role change operation, the history record preserves all fields
 * (oldRole, newRole, changedBy, changedAt) and records are ordered by time desc.
 *
 * **Validates: Requirements 12.2, 12.4**
 */
class MemberStatusPropertyTest {

    // ========================================================================
    // Property 36: 成员在线状态转换
    // **Validates: Requirements 12.2**
    // ========================================================================

    @Property(tries = 200)
    void property36_heartbeatSetsOfflineToOnline(
            @ForAll("userIds") Long userId) {

        UserRepository userRepo = mock(UserRepository.class);
        MemberServiceImpl service = createService(userRepo);

        User user = buildUser(userId, OnlineStatus.OFFLINE);
        when(userRepo.findById(userId)).thenReturn(Optional.of(user));
        when(userRepo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        service.heartbeat(userId);

        assertThat(user.getOnlineStatus())
                .as("Heartbeat must transition OFFLINE to ONLINE")
                .isEqualTo(OnlineStatus.ONLINE);
        assertThat(user.getLastActiveAt())
                .as("Heartbeat must update lastActiveAt")
                .isNotNull();
    }

    @Property(tries = 200)
    void property36_heartbeatKeepsBusyAsBusy(
            @ForAll("userIds") Long userId) {

        UserRepository userRepo = mock(UserRepository.class);
        MemberServiceImpl service = createService(userRepo);

        User user = buildUser(userId, OnlineStatus.BUSY);
        when(userRepo.findById(userId)).thenReturn(Optional.of(user));
        when(userRepo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        service.heartbeat(userId);

        assertThat(user.getOnlineStatus())
                .as("Heartbeat must keep BUSY status unchanged")
                .isEqualTo(OnlineStatus.BUSY);
    }

    @Property(tries = 200)
    void property36_heartbeatKeepsOnlineAsOnline(
            @ForAll("userIds") Long userId) {

        UserRepository userRepo = mock(UserRepository.class);
        MemberServiceImpl service = createService(userRepo);

        User user = buildUser(userId, OnlineStatus.ONLINE);
        when(userRepo.findById(userId)).thenReturn(Optional.of(user));
        when(userRepo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        service.heartbeat(userId);

        assertThat(user.getOnlineStatus())
                .as("Heartbeat must keep ONLINE status unchanged")
                .isEqualTo(OnlineStatus.ONLINE);
    }

    @Property(tries = 200)
    void property36_updateOnlineStatusSetsAnyStatus(
            @ForAll("userIds") Long userId,
            @ForAll("onlineStatuses") OnlineStatus targetStatus) {

        UserRepository userRepo = mock(UserRepository.class);
        MemberServiceImpl service = createService(userRepo);

        // Start from any initial status
        User user = buildUser(userId, OnlineStatus.OFFLINE);
        when(userRepo.findById(userId)).thenReturn(Optional.of(user));
        when(userRepo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        service.updateOnlineStatus(userId, targetStatus);

        assertThat(user.getOnlineStatus())
                .as("updateOnlineStatus must set status to %s", targetStatus)
                .isEqualTo(targetStatus);
        assertThat(user.getLastActiveAt())
                .as("updateOnlineStatus must update lastActiveAt")
                .isNotNull();
    }

    @Property(tries = 200)
    void property36_updateOnlineStatusFromAnyInitialState(
            @ForAll("userIds") Long userId,
            @ForAll("onlineStatuses") OnlineStatus initialStatus,
            @ForAll("onlineStatuses") OnlineStatus targetStatus) {

        UserRepository userRepo = mock(UserRepository.class);
        MemberServiceImpl service = createService(userRepo);

        User user = buildUser(userId, initialStatus);
        when(userRepo.findById(userId)).thenReturn(Optional.of(user));
        when(userRepo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        service.updateOnlineStatus(userId, targetStatus);

        assertThat(user.getOnlineStatus())
                .as("updateOnlineStatus(%s→%s) must set target status", initialStatus, targetStatus)
                .isEqualTo(targetStatus);
    }

    @Property(tries = 100)
    void property36_checkAndUpdateOfflineStatusSetsInactiveUsersOffline(
            @ForAll("userCounts") int userCount) {

        UserRepository userRepo = mock(UserRepository.class);
        MemberServiceImpl service = createService(userRepo);

        // Create inactive users (last active > 30 min ago) with non-OFFLINE status
        List<User> inactiveUsers = IntStream.range(0, userCount)
                .mapToObj(i -> {
                    User u = buildUser((long) (i + 1), i % 2 == 0 ? OnlineStatus.ONLINE : OnlineStatus.BUSY);
                    u.setLastActiveAt(LocalDateTime.now().minusMinutes(45));
                    return u;
                })
                .collect(Collectors.toList());

        when(userRepo.findByOnlineStatusNotAndLastActiveAtBefore(
                eq(OnlineStatus.OFFLINE), any(LocalDateTime.class)))
                .thenReturn(inactiveUsers);
        when(userRepo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        service.checkAndUpdateOfflineStatus();

        for (User user : inactiveUsers) {
            assertThat(user.getOnlineStatus())
                    .as("Inactive user %d must be set to OFFLINE", user.getId())
                    .isEqualTo(OnlineStatus.OFFLINE);
        }
    }

    // ========================================================================
    // Property 37: 角色变更历史记录
    // **Validates: Requirements 12.4**
    // ========================================================================

    @Property(tries = 200)
    void property37_roleChangeHistoryPreservesAllFields(
            @ForAll("userIds") Long userId,
            @ForAll("roles") Role oldRole,
            @ForAll("roles") Role newRole,
            @ForAll("operatorNames") String changedBy) {

        UserRepository userRepo = mock(UserRepository.class);
        RoleChangeHistoryRepository historyRepo = mock(RoleChangeHistoryRepository.class);
        MemberServiceImpl service = createService(userRepo, historyRepo);

        LocalDateTime changedAt = LocalDateTime.now();
        RoleChangeHistory history = RoleChangeHistory.builder()
                .id(1L)
                .userId(userId)
                .oldRole(oldRole)
                .newRole(newRole)
                .changedBy(changedBy)
                .changedAt(changedAt)
                .build();

        when(userRepo.findById(userId)).thenReturn(Optional.of(buildUser(userId, OnlineStatus.ONLINE)));
        when(historyRepo.findByUserIdOrderByChangedAtDesc(userId))
                .thenReturn(List.of(history));

        List<RoleChangeRecord> records = service.getRoleHistory(userId);

        assertThat(records).hasSize(1);
        RoleChangeRecord record = records.get(0);
        assertThat(record.getOldRole())
                .as("Role change history must preserve oldRole")
                .isEqualTo(oldRole);
        assertThat(record.getNewRole())
                .as("Role change history must preserve newRole")
                .isEqualTo(newRole);
        assertThat(record.getChangedBy())
                .as("Role change history must preserve changedBy")
                .isEqualTo(changedBy);
        assertThat(record.getChangedAt())
                .as("Role change history must preserve changedAt")
                .isEqualTo(changedAt);
    }

    @Property(tries = 100)
    void property37_roleChangeHistoryOrderedByTimeDesc(
            @ForAll("userIds") Long userId,
            @ForAll("historyLengths") int historyLength) {

        UserRepository userRepo = mock(UserRepository.class);
        RoleChangeHistoryRepository historyRepo = mock(RoleChangeHistoryRepository.class);
        MemberServiceImpl service = createService(userRepo, historyRepo);

        // Create history records with descending timestamps (as the repo returns)
        LocalDateTime baseTime = LocalDateTime.now();
        List<RoleChangeHistory> histories = IntStream.range(0, historyLength)
                .mapToObj(i -> RoleChangeHistory.builder()
                        .id((long) (i + 1))
                        .userId(userId)
                        .oldRole(Role.INTERN)
                        .newRole(Role.MEMBER)
                        .changedBy("admin")
                        .changedAt(baseTime.minusHours(i)) // most recent first
                        .build())
                .collect(Collectors.toList());

        when(userRepo.findById(userId)).thenReturn(Optional.of(buildUser(userId, OnlineStatus.ONLINE)));
        when(historyRepo.findByUserIdOrderByChangedAtDesc(userId)).thenReturn(histories);

        List<RoleChangeRecord> records = service.getRoleHistory(userId);

        assertThat(records).hasSize(historyLength);

        // Verify descending order
        for (int i = 1; i < records.size(); i++) {
            assertThat(records.get(i - 1).getChangedAt())
                    .as("Record at index %d must be after or equal to record at index %d", i - 1, i)
                    .isAfterOrEqualTo(records.get(i).getChangedAt());
        }
    }

    @Property(tries = 200)
    void property37_roleChangeHistoryDistinctRolesRecorded(
            @ForAll("userIds") Long userId,
            @ForAll("rolePairs") Role[] rolePair) {

        Role oldRole = rolePair[0];
        Role newRole = rolePair[1];

        UserRepository userRepo = mock(UserRepository.class);
        RoleChangeHistoryRepository historyRepo = mock(RoleChangeHistoryRepository.class);
        MemberServiceImpl service = createService(userRepo, historyRepo);

        RoleChangeHistory history = RoleChangeHistory.builder()
                .id(1L)
                .userId(userId)
                .oldRole(oldRole)
                .newRole(newRole)
                .changedBy("leader")
                .changedAt(LocalDateTime.now())
                .build();

        when(userRepo.findById(userId)).thenReturn(Optional.of(buildUser(userId, OnlineStatus.ONLINE)));
        when(historyRepo.findByUserIdOrderByChangedAtDesc(userId)).thenReturn(List.of(history));

        List<RoleChangeRecord> records = service.getRoleHistory(userId);

        assertThat(records).hasSize(1);
        assertThat(records.get(0).getOldRole())
                .as("Old role must be correctly recorded as %s", oldRole)
                .isEqualTo(oldRole);
        assertThat(records.get(0).getNewRole())
                .as("New role must be correctly recorded as %s", newRole)
                .isEqualTo(newRole);
    }

    // ========== Arbitrary Providers ==========

    @Provide
    Arbitrary<Long> userIds() {
        return Arbitraries.longs().between(1L, 10000L);
    }

    @Provide
    Arbitrary<OnlineStatus> onlineStatuses() {
        return Arbitraries.of(OnlineStatus.ONLINE, OnlineStatus.BUSY, OnlineStatus.OFFLINE);
    }

    @Provide
    Arbitrary<Role> roles() {
        return Arbitraries.of(Role.values());
    }

    @Provide
    Arbitrary<String> operatorNames() {
        return Arbitraries.of("admin", "leader", "system", "vice_leader");
    }

    @Provide
    Arbitrary<Integer> userCounts() {
        return Arbitraries.integers().between(0, 10);
    }

    @Provide
    Arbitrary<Integer> historyLengths() {
        return Arbitraries.integers().between(0, 10);
    }

    @Provide
    Arbitrary<Role[]> rolePairs() {
        return Arbitraries.of(Role.values())
                .tuple2()
                .filter(t -> t.get1() != t.get2())
                .map(t -> new Role[]{t.get1(), t.get2()});
    }

    // ========== Helpers ==========

    private User buildUser(Long id, OnlineStatus status) {
        return User.builder()
                .id(id)
                .username("user_" + id)
                .password("encoded_password")
                .role(Role.MEMBER)
                .enabled(true)
                .onlineStatus(status)
                .lastActiveAt(LocalDateTime.now())
                .build();
    }

    private MemberServiceImpl createService(UserRepository userRepo) {
        return new MemberServiceImpl(
                userRepo,
                mock(UserActivityLogRepository.class),
                mock(RoleChangeHistoryRepository.class)
        );
    }

    private MemberServiceImpl createService(UserRepository userRepo, RoleChangeHistoryRepository historyRepo) {
        return new MemberServiceImpl(
                userRepo,
                mock(UserActivityLogRepository.class),
                historyRepo
        );
    }
}
