package com.example.kbassistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.kbassistant.entity.ChatMessage;
import com.example.kbassistant.mapper.ChatMessageMapper;
import com.example.kbassistant.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatMessageServiceImpl implements ChatMessageService {

    private final ChatMessageMapper messageMapper;

    @Override
    public IPage<ChatMessage> pageBySessionId(Long sessionId, int pageNum, int pageSize) {
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getSessionId, sessionId)
                .orderByAsc(ChatMessage::getCreatedAt);
        return messageMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public ChatMessage save(ChatMessage message) {
        messageMapper.insert(message);
        return message;
    }

    @Override
    public ChatMessage getById(Long id) {
        return messageMapper.selectById(id);
    }

    @Override
    public ChatMessage findPreviousUserMessage(Long sessionId, Long beforeMessageId) {
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getSessionId, sessionId)
                .eq(ChatMessage::getRole, "USER")
                .lt(ChatMessage::getId, beforeMessageId)
                .orderByDesc(ChatMessage::getId)
                .last("LIMIT 1");
        return messageMapper.selectOne(wrapper);
    }

    @Override
    public List<ChatMessage> findRecentBySessionId(Long sessionId, int limit) {
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getSessionId, sessionId)
                .orderByDesc(ChatMessage::getCreatedAt)
                .last("LIMIT " + limit);
        return messageMapper.selectList(wrapper);
    }

    @Override
    public void deleteBySessionId(Long sessionId) {
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getSessionId, sessionId);
        messageMapper.delete(wrapper);
    }
}
