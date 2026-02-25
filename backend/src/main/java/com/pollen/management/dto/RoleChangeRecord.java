package com.pollen.management.dto;

import com.pollen.management.entity.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 角色变更记录
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleChangeRecord {
    private Long id;
    private Role oldRole;
    private Role newRole;
    private String changedBy;
    private LocalDateTime changedAt;
}
