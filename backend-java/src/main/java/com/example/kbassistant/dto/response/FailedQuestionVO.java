package com.example.kbassistant.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FailedQuestionVO {
    private Long id;
    private Long userId;
    private Long knowledgeBaseId;
    private String kbName;
    private Long sessionId;
    private String question;
    private String failureType;
    private String remark;
    private String status;
    private String resolution;
    private LocalDateTime resolvedAt;
    private Long resolvedBy;
    private LocalDateTime createdAt;
}
