package com.example.kbassistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.kbassistant.entity.ChatSession;
import com.example.kbassistant.mapper.ChatSessionMapper;
import com.example.kbassistant.service.ChatSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatSessionServiceImpl implements ChatSessionService {

    private final ChatSessionMapper sessionMapper;

    @Override
    public ChatSession create(Long userId, Long knowledgeBaseId) {
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setKnowledgeBaseId(knowledgeBaseId);
        session.setTitle("新会话");
        sessionMapper.insert(session);
        return session;
    }

    @Override
    public IPage<ChatSession> pageByUserId(Long userId, int pageNum, int pageSize) {
        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatSession::getUserId, userId)
                .orderByDesc(ChatSession::getUpdatedAt);
        return sessionMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public void delete(Long id) {
        sessionMapper.deleteById(id);
    }
}
