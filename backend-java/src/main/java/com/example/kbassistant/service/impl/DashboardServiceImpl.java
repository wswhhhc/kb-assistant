package com.example.kbassistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.kbassistant.dto.response.DashboardStatsResponse;
import com.example.kbassistant.entity.*;
import com.example.kbassistant.mapper.*;
import com.example.kbassistant.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final SysUserMapper userMapper;
    private final KnowledgeBaseMapper kbMapper;
    private final DocumentMapper documentMapper;
    private final ChatMessageMapper messageMapper;
    private final AnswerFeedbackMapper feedbackMapper;
    private final FailedQuestionMapper failedQuestionMapper;

    @Override
    public DashboardStatsResponse getStatistics() {
        DashboardStatsResponse stats = new DashboardStatsResponse();
        stats.setUserCount(userMapper.selectCount(null));
        stats.setKnowledgeBaseCount(kbMapper.selectCount(null));

        LambdaQueryWrapper<Document> docWrapper = new LambdaQueryWrapper<>();
        docWrapper.eq(Document::getParseStatus, "READY");
        stats.setReadyDocumentCount(documentMapper.selectCount(docWrapper));
        stats.setDocumentCount(documentMapper.selectCount(null));

        LambdaQueryWrapper<ChatMessage> msgWrapper = new LambdaQueryWrapper<>();
        msgWrapper.eq(ChatMessage::getRole, "USER");
        stats.setChatCount(messageMapper.selectCount(msgWrapper));

        stats.setFeedbackCount(feedbackMapper.selectCount(null));
        stats.setFailedQuestionCount(failedQuestionMapper.selectCount(null));
        return stats;
    }
}
