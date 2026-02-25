package com.pollen.management.service;

import com.pollen.management.dto.MemberCardItem;
import com.pollen.management.dto.MemberDetail;
import com.pollen.management.dto.RoleChangeRecord;
import com.pollen.management.dto.WeeklyActivityHour;
import com.pollen.management.entity.RoleChangeHistory;
import com.pollen.management.entity.User;
import com.pollen.management.entity.UserActivityLog;
import com.pollen.management.entity.enums.OnlineStatus;
import com.pollen.management.repository.RoleChangeHistoryRepository;
import com.pollen.management.repository.UserActivityLogRepository;
import com.pollen.management.repository.UserRepository;
import com.pollen.management.config.RedisConfig;
import com.pollen.management.util.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberServiceImpl implements MemberService {

    private final UserRepository userRepository;
    private final UserActivityLogRepository userActivityLogRepository;
    private final RoleChangeHistoryRepository roleChangeHistoryRepository;

    @Override
    @Cacheable(value = RedisConfig.CACHE_MEMBERS, key = "'list'", unless = "#result == null || #result.isEmpty()")
    public List<MemberCardItem> listMembers() {
        return userRepository.findAll().stream()
                .filter(user -> user.getEnabled())
                .map(user -> MemberCardItem.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .role(user.getRole())
                        .onlineStatus(user.getOnlineStatus())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = RedisConfig.CACHE_MEMBERS, key = "'detail:' + #id", unless = "#result == null")
    public MemberDetail getMemberDetail(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "成员不存在"));

        List<WeeklyActivityHour> activityHours = getActivityHours(id);
        List<RoleChangeRecord> roleHistory = getRoleHistory(id);

        return MemberDetail.builder()
                .id(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .onlineStatus(user.getOnlineStatus())
                .lastActiveAt(user.getLastActiveAt())
                .createdAt(user.getCreatedAt())
                .activityHours(activityHours)
                .roleHistory(roleHistory)
                .build();
    }

    @Override
    public List<WeeklyActivityHour> getActivityHours(Long memberId) {
        userRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(404, "成员不存在"));

        List<WeeklyActivityHour> result = new ArrayList<>();
        LocalDate today = LocalDate.now();

        // Calculate activity hours for the last 4 weeks
        for (int i = 0; i < 4; i++) {
            LocalDate weekEnd = today.minusWeeks(i).with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
            LocalDate weekStart = weekEnd.minusDays(6);

            if (weekEnd.isAfter(today)) {
                weekEnd = today;
            }

            LocalDateTime startTime = weekStart.atStartOfDay();
            LocalDateTime endTime = weekEnd.plusDays(1).atStartOfDay();

            List<UserActivityLog> logs = userActivityLogRepository
                    .findByUserIdAndActionTimeBetween(memberId, startTime, endTime);

            int totalMinutes = logs.stream()
                    .mapToInt(log -> log.getDurationMinutes() != null ? log.getDurationMinutes() : 0)
                    .sum();

            result.add(WeeklyActivityHour.builder()
                    .weekStart(weekStart)
                    .weekEnd(weekEnd)
                    .totalMinutes(totalMinutes)
                    .build());
        }

        return result;
    }

    @Override
    public List<RoleChangeRecord> getRoleHistory(Long memberId) {
        userRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(404, "成员不存在"));

        List<RoleChangeHistory> histories = roleChangeHistoryRepository
                .findByUserIdOrderByChangedAtDesc(memberId);

        return histories.stream()
                .map(h -> RoleChangeRecord.builder()
                        .id(h.getId())
                        .oldRole(h.getOldRole())
                        .newRole(h.getNewRole())
                        .changedBy(h.getChangedBy())
                        .changedAt(h.getChangedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @CacheEvict(value = RedisConfig.CACHE_MEMBERS, allEntries = true)
    public void updateOnlineStatus(Long userId, OnlineStatus status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));

        user.setOnlineStatus(status);
        user.setLastActiveAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    @Transactional
    @CacheEvict(value = RedisConfig.CACHE_MEMBERS, allEntries = true)
    public void heartbeat(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));

        user.setLastActiveAt(LocalDateTime.now());
        if (user.getOnlineStatus() == OnlineStatus.OFFLINE) {
            user.setOnlineStatus(OnlineStatus.ONLINE);
        }
        userRepository.save(user);
    }

    @Override
    @Scheduled(fixedRate = 60000) // Check every minute
    @Transactional
    public void checkAndUpdateOfflineStatus() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(30);

        List<User> inactiveUsers = userRepository
                .findByOnlineStatusNotAndLastActiveAtBefore(OnlineStatus.OFFLINE, threshold);

        for (User user : inactiveUsers) {
            user.setOnlineStatus(OnlineStatus.OFFLINE);
            userRepository.save(user);
            log.info("用户超时离线: userId={}, username={}", user.getId(), user.getUsername());
        }
    }
}
