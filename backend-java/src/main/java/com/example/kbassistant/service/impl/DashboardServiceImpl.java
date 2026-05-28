package com.example.kbassistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.kbassistant.constants.MessageRoles;
import com.example.kbassistant.dto.response.DashboardStatsResponse;
import com.example.kbassistant.entity.*;
import com.example.kbassistant.enums.DocumentStatus;
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
    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper messageMapper;
    private final AnswerFeedbackMapper feedbackMapper;
    private final FailedQuestionMapper failedQuestionMapper;

    @Override
    public DashboardStatsResponse getStatistics(Long userId, boolean isAdmin) {
        if (!isAdmin && userId != null) {
            return getUserStatistics(userId);
        }
        return getAdminStatistics();
    }

    private DashboardStatsResponse getAdminStatistics() {
        DashboardStatsResponse stats = new DashboardStatsResponse();
        stats.setAdminView(true);
        stats.setUserCount(userMapper.selectCount(null));
        stats.setKnowledgeBaseCount(kbMapper.selectCount(null));

        LambdaQueryWrapper<Document> docWrapper = new LambdaQueryWrapper<>();
        docWrapper.eq(Document::getParseStatus, DocumentStatus.READY.name());
        stats.setReadyDocumentCount(documentMapper.selectCount(docWrapper));
        stats.setDocumentCount(documentMapper.selectCount(null));

        LambdaQueryWrapper<ChatMessage> msgWrapper = new LambdaQueryWrapper<>();
        msgWrapper.eq(ChatMessage::getRole, MessageRoles.USER);
        stats.setChatCount(messageMapper.selectCount(msgWrapper));

        stats.setFeedbackCount(feedbackMapper.selectCount(null));
        stats.setFailedQuestionCount(failedQuestionMapper.selectCount(null));
        return stats;
    }

    private DashboardStatsResponse getUserStatistics(Long userId) {
        DashboardStatsResponse stats = new DashboardStatsResponse();
        stats.setAdminView(false);

        LambdaQueryWrapper<KnowledgeBase> kbWrapper = new LambdaQueryWrapper<>();
        kbWrapper.eq(KnowledgeBase::getScope, "PUBLIC")
                .or(w -> w.eq(KnowledgeBase::getScope, "PRIVATE")
                        .eq(KnowledgeBase::getOwnerUserId, userId));
        stats.setKnowledgeBaseCount(kbMapper.selectCount(kbWrapper));

        LambdaQueryWrapper<Document> myDocWrapper = new LambdaQueryWrapper<>();
        myDocWrapper.eq(Document::getCreatedBy, userId);
        stats.setDocumentCount(documentMapper.selectCount(myDocWrapper));

        LambdaQueryWrapper<Document> myReadyDocWrapper = new LambdaQueryWrapper<>();
        myReadyDocWrapper.eq(Document::getCreatedBy, userId)
                .eq(Document::getParseStatus, DocumentStatus.READY.name());
        stats.setReadyDocumentCount(documentMapper.selectCount(myReadyDocWrapper));

        LambdaQueryWrapper<ChatSession> mySessionWrapper = new LambdaQueryWrapper<>();
        mySessionWrapper.eq(ChatSession::getUserId, userId);
        var sessions = chatSessionMapper.selectList(mySessionWrapper);
        stats.setUserCount(1);

        if (sessions.isEmpty()) {
            stats.setChatCount(0);
        } else {
            var sessionIds = sessions.stream().map(ChatSession::getId).toList();
            LambdaQueryWrapper<ChatMessage> myMsgWrapper = new LambdaQueryWrapper<>();
            myMsgWrapper.eq(ChatMessage::getRole, MessageRoles.USER)
                    .in(ChatMessage::getSessionId, sessionIds);
            stats.setChatCount(messageMapper.selectCount(myMsgWrapper));
        }

        LambdaQueryWrapper<AnswerFeedback> myFeedbackWrapper = new LambdaQueryWrapper<>();
        myFeedbackWrapper.eq(AnswerFeedback::getUserId, userId);
        stats.setFeedbackCount(feedbackMapper.selectCount(myFeedbackWrapper));

        LambdaQueryWrapper<FailedQuestion> myFailedWrapper = new LambdaQueryWrapper<>();
        myFailedWrapper.eq(FailedQuestion::getUserId, userId);
        stats.setFailedQuestionCount(failedQuestionMapper.selectCount(myFailedWrapper));

        return stats;
    }
}
