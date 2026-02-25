package com.pollen.management.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateGroupMembersRequest {

    @NotNull(message = "成员ID列表不能为空")
    private List<Long> memberIds;
}
