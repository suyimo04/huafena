package com.pollen.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateGroupRequest {

    @NotBlank(message = "分组名称不能为空")
    private String groupName;

    @NotNull(message = "成员ID列表不能为空")
    private List<Long> memberIds;
}
