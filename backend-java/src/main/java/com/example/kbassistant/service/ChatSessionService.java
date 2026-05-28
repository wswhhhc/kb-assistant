package com.example.kbassistant.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.kbassistant.dto.response.ChatSessionListItemVO;
import com.example.kbassistant.entity.ChatSession;

public interface ChatSessionService {
    ChatSession create(Long userId, Long knowledgeBaseId, boolean isAdmin);
    ChatSession getById(Long id);
    IPage<ChatSessionListItemVO> pageByUserId(Long userId, Long knowledgeBaseId, int pageNum, int pageSize);
    void updateTitleIfDefault(Long sessionId, String question);
    void delete(Long id);
}
