package com.example.kbassistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.kbassistant.entity.ChatMessage;
import com.example.kbassistant.mapper.ChatMessageMapper;
import com.example.kbassistant.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
    public void save(ChatMessage message) {
        messageMapper.insert(message);
    }
}
