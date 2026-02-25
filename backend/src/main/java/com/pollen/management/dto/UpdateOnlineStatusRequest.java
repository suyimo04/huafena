package com.pollen.management.dto;

import com.pollen.management.entity.enums.OnlineStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新成员在线状态请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOnlineStatusRequest {

    @NotNull(message = "在线状态不能为空")
    private OnlineStatus status;
}
