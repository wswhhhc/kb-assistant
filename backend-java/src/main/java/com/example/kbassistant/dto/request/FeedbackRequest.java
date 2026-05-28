package com.example.kbassistant.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FeedbackRequest {
    @NotNull(message = "消息ID不能为空")
    private Long messageId;
    @NotBlank(message = "反馈类型不能为空")
    private String feedbackType;
    private String reasonType;
    private String reason;
}
