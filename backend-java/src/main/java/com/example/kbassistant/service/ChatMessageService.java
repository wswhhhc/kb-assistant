package com.example.kbassistant.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.kbassistant.entity.ChatMessage;

import java.util.List;

public interface ChatMessageService {
    IPage<ChatMessage> pageBySessionId(Long sessionId, int pageNum, int pageSize);
    ChatMessage save(ChatMessage message);
    ChatMessage getById(Long id);
    ChatMessage findPreviousUserMessage(Long sessionId, Long beforeMessageId);
    List<ChatMessage> findRecentBySessionId(Long sessionId, int limit);
    void deleteBySessionId(Long sessionId);
}
