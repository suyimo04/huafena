package com.pollen.management.dto;

import com.pollen.management.entity.enums.OnlineStatus;
import com.pollen.management.entity.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 成员卡片展示数据
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberCardItem {
    private Long id;
    private String username;
    private Role role;
    private OnlineStatus onlineStatus;
}
