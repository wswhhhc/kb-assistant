package com.example.kbassistant.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChatAskRequest {
    @NotNull(message = "知识库ID不能为空")
    private Long knowledgeBaseId;
    private Long sessionId;
    @NotBlank(message = "问题不能为空")
    private String question;
}
