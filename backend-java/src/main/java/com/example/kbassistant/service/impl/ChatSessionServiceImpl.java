package com.example.kbassistant.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.kbassistant.dto.response.ChatSessionListItemVO;
import com.example.kbassistant.entity.ChatSession;
import com.example.kbassistant.mapper.ChatSessionMapper;
import com.example.kbassistant.service.ChatMessageService;
import com.example.kbassistant.service.ChatSessionService;
import com.example.kbassistant.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatSessionServiceImpl implements ChatSessionService {

    private static final String DEFAULT_TITLE = "新会话";
    private static final int MAX_TITLE_LENGTH = 20;

    private final ChatSessionMapper sessionMapper;
    private final ChatMessageService chatMessageService;
    private final KnowledgeBaseService knowledgeBaseService;

    @Override
    public ChatSession getById(Long id) {
        return sessionMapper.selectById(id);
    }

    @Override
    public ChatSession create(Long userId, Long knowledgeBaseId, boolean isAdmin) {
        // 检查用户对该知识库有读取权限
        knowledgeBaseService.getById(knowledgeBaseId, userId, isAdmin);

        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setKnowledgeBaseId(knowledgeBaseId);
        session.setTitle(DEFAULT_TITLE);
        sessionMapper.insert(session);
        return session;
    }

    @Override
    public IPage<ChatSessionListItemVO> pageByUserId(Long userId, Long knowledgeBaseId, int pageNum, int pageSize) {
        return sessionMapper.selectSessionListPage(new Page<>(pageNum, pageSize), userId, knowledgeBaseId);
    }

    @Override
    public void updateTitleIfDefault(Long sessionId, String question) {
        if (sessionId == null || question == null || question.isBlank()) {
            return;
        }

        ChatSession session = sessionMapper.selectById(sessionId);
        if (session == null || !DEFAULT_TITLE.equals(session.getTitle())) {
            return;
        }

        ChatSession update = new ChatSession();
        update.setId(sessionId);
        update.setTitle(buildTitle(question));
        sessionMapper.updateById(update);
    }

    @Override
    public void delete(Long id) {
        chatMessageService.deleteBySessionId(id);
        sessionMapper.deleteById(id);
    }

    private String buildTitle(String question) {
        String title = question
                .replaceAll("\\s+", " ")
                .replaceAll("[\\r\\n]+", " ")
                .trim();

        if (title.isEmpty()) {
            return DEFAULT_TITLE;
        }

        if (title.length() > MAX_TITLE_LENGTH) {
            return title.substring(0, MAX_TITLE_LENGTH) + "...";
        }
        return title;
    }
}
