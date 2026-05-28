package com.example.kbassistant.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.kbassistant.entity.ChatSession;

public interface ChatSessionService {
    ChatSession create(Long userId, Long knowledgeBaseId, boolean isAdmin);
    ChatSession getById(Long id);
    IPage<ChatSession> pageByUserId(Long userId, Long knowledgeBaseId, int pageNum, int pageSize);
    void updateTitleIfDefault(Long sessionId, String question);
    void delete(Long id);
}
