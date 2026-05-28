package com.example.kbassistant.dto.request;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AiAskRequest {
    private Long userId;
    private Long knowledgeBaseId;
    private Long sessionId;
    private String question;
    private List<Map<String, String>> history;
    private boolean retrievalOnly;
}
