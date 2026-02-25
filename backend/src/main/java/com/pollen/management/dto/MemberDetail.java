package com.pollen.management.dto;

import com.pollen.management.entity.enums.OnlineStatus;
import com.pollen.management.entity.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 成员详情数据
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberDetail {
    private Long id;
    private String username;
    private Role role;
    private OnlineStatus onlineStatus;
    private LocalDateTime lastActiveAt;
    private LocalDateTime createdAt;
    private List<WeeklyActivityHour> activityHours;
    private List<RoleChangeRecord> roleHistory;
}
