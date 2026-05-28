package com.example.kbassistant.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatSessionListItemVO {
    private Long id;
    private Long userId;
    private Long knowledgeBaseId;
    private String title;
    private String kbName;
    private Long msgCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
