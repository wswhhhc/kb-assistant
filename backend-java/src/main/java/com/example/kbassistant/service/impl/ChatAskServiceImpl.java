package com.example.kbassistant.service.impl;

import com.example.kbassistant.common.BusinessException;
import com.example.kbassistant.constants.MessageRoles;
import com.example.kbassistant.dto.response.ChatAskContext;
import com.example.kbassistant.entity.ChatMessage;
import com.example.kbassistant.entity.ChatSession;
import com.example.kbassistant.enums.FailureType;
import com.example.kbassistant.service.ChatAskService;
import com.example.kbassistant.service.ChatMessageService;
import com.example.kbassistant.service.ChatSessionService;
import com.example.kbassistant.service.FailedQuestionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatAskServiceImpl implements ChatAskService {

    private final ChatSessionService sessionService;
    private final ChatMessageService messageService;
    private final FailedQuestionService failedQuestionService;
    private final ObjectMapper objectMapper;

    @Override
    public ChatAskContext prepareContext(Long userId, Long knowledgeBaseId, Long sessionId, String question, boolean isAdmin) {
        // 1. 会话懒创建
        if (sessionId == null) {
            ChatSession session = sessionService.create(userId, knowledgeBaseId, isAdmin);
            sessionId = session.getId();
        }
        final Long finalSessionId = sessionId;

        // 2. 加载历史消息
        List<ChatMessage> recentMessages = messageService.findRecentBySessionId(finalSessionId, 50);
        List<Map<String, String>> history = new ArrayList<>();
        for (int i = recentMessages.size() - 1; i >= 0; i--) {
            ChatMessage msg = recentMessages.get(i);
            Map<String, String> entry = new HashMap<>();
            entry.put("role", MessageRoles.AI.equals(msg.getRole()) ? MessageRoles.AI : MessageRoles.USER);
            entry.put("content", msg.getContent());
            history.add(entry);
        }

        // 3. 保存用户消息
        ChatMessage userMsg = new ChatMessage();
        userMsg.setSessionId(finalSessionId);
        userMsg.setRole(MessageRoles.USER);
        userMsg.setContent(question);
        messageService.save(userMsg);

        // 4. 更新默认标题
        sessionService.updateTitleIfDefault(finalSessionId, question);

        return new ChatAskContext(finalSessionId, history);
    }

    @Override
    public ChatMessage saveAiMessage(Long sessionId, String answer, List<Map<String, Object>> citations, int retrievalCount, String modelName) {
        ChatMessage aiMsg = new ChatMessage();
        aiMsg.setSessionId(sessionId);
        aiMsg.setRole(MessageRoles.AI);
        aiMsg.setContent(answer);
        aiMsg.setModelName(modelName);
        aiMsg.setRetrievalCount(retrievalCount);
        try {
            aiMsg.setCitationJson(objectMapper.writeValueAsString(citations));
        } catch (Exception e) {
            log.warn("序列化引用JSON失败", e);
            aiMsg.setCitationJson("[]");
        }
        messageService.save(aiMsg);
        return aiMsg;
    }

    @Override
    public void recordFailedQuestion(Long userId, Long knowledgeBaseId, Long sessionId, String question, int retrievalCount, boolean success, String answer) {
        if (question == null || question.isBlank()) {
            return;
        }

        if (retrievalCount == 0) {
            failedQuestionService.record(userId, knowledgeBaseId, sessionId, question, FailureType.NO_HIT.name(), answer);
            return;
        }

        if (!success) {
            failedQuestionService.record(userId, knowledgeBaseId, sessionId, question, FailureType.MODEL_ERROR.name(), answer);
        }
    }
}
