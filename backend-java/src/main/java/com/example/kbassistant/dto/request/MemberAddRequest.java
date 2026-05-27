package com.example.kbassistant.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MemberAddRequest {
    @NotNull(message = "用户ID不能为空")
    private Long userId;
}
