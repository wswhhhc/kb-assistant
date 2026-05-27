package com.example.kbassistant.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.kbassistant.entity.ChatMessage;

public interface ChatMessageService {
    IPage<ChatMessage> pageBySessionId(Long sessionId, int pageNum, int pageSize);
    void save(ChatMessage message);
}
