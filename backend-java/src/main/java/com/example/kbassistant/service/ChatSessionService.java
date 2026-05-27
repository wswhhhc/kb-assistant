package com.example.kbassistant.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.kbassistant.entity.ChatSession;

public interface ChatSessionService {
    ChatSession create(Long userId, Long knowledgeBaseId);
    IPage<ChatSession> pageByUserId(Long userId, int pageNum, int pageSize);
    void delete(Long id);
}
