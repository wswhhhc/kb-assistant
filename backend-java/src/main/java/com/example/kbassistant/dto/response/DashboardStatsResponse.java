package com.example.kbassistant.dto.response;

import lombok.Data;

@Data
public class DashboardStatsResponse {
    private boolean adminView;
    private long userCount;
    private long knowledgeBaseCount;
    private long documentCount;
    private long readyDocumentCount;
    private long chatCount;
    private long feedbackCount;
    private long failedQuestionCount;
}
