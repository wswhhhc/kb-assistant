package com.example.kbassistant.service;

import com.example.kbassistant.dto.response.ChatAskContext;
import com.example.kbassistant.entity.ChatMessage;

import java.util.List;
import java.util.Map;

public interface ChatAskService {
    ChatAskContext prepareContext(Long userId, Long knowledgeBaseId, Long sessionId, String question, boolean isAdmin);

    ChatMessage saveAiMessage(Long sessionId, String answer, List<Map<String, Object>> citations, int retrievalCount, String modelName);

    void recordFailedQuestion(Long userId, Long knowledgeBaseId, Long sessionId, String question, int retrievalCount, boolean success, String answer);
}
