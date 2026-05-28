package com.example.kbassistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.kbassistant.common.BusinessException;
import com.example.kbassistant.dto.request.FeedbackRequest;
import com.example.kbassistant.entity.AnswerFeedback;
import com.example.kbassistant.entity.ChatMessage;
import com.example.kbassistant.entity.ChatSession;
import com.example.kbassistant.mapper.AnswerFeedbackMapper;
import com.example.kbassistant.mapper.ChatSessionMapper;
import com.example.kbassistant.service.ChatMessageService;
import com.example.kbassistant.service.FailedQuestionService;
import com.example.kbassistant.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    private final AnswerFeedbackMapper feedbackMapper;
    private final ChatMessageService chatMessageService;
    private final ChatSessionMapper chatSessionMapper;
    private final FailedQuestionService failedQuestionService;

    @Override
    public void submit(FeedbackRequest request, Long userId) {
        ChatMessage aiMessage = chatMessageService.getById(request.getMessageId());
        if (aiMessage == null || !"AI".equals(aiMessage.getRole())) {
            throw new BusinessException(400, "反馈消息不存在");
        }

        LambdaQueryWrapper<AnswerFeedback> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AnswerFeedback::getMessageId, request.getMessageId())
                .eq(AnswerFeedback::getUserId, userId)
                .last("LIMIT 1");

        AnswerFeedback feedback = feedbackMapper.selectOne(wrapper);
        if (feedback == null) {
            feedback = new AnswerFeedback();
            feedback.setMessageId(request.getMessageId());
            feedback.setUserId(userId);
            feedback.setFeedbackType(request.getFeedbackType());
            feedback.setReason(request.getReason());
            feedbackMapper.insert(feedback);
        } else {
            feedback.setFeedbackType(request.getFeedbackType());
            feedback.setReason(request.getReason());
            feedbackMapper.updateById(feedback);
        }

        if ("DISLIKE".equalsIgnoreCase(request.getFeedbackType())) {
            recordLowQualityQuestion(aiMessage, request.getReasonType(), request.getReason(), userId);
        }
    }

    @Override
    public IPage<AnswerFeedback> page(int pageNum, int pageSize) {
        Page<AnswerFeedback> page = new Page<>(pageNum, pageSize);
        page.addOrder(OrderItem.desc("created_at"));
        return feedbackMapper.selectPage(page, null);
    }

    @Override
    public Map<Long, String> getMyFeedbackMap(String messageIds, Long userId) {
        if (messageIds == null || messageIds.isBlank()) {
            return Collections.emptyMap();
        }

        List<Long> ids = Arrays.stream(messageIds.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .map(Long::valueOf)
                .toList();

        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }

        LambdaQueryWrapper<AnswerFeedback> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AnswerFeedback::getUserId, userId)
                .in(AnswerFeedback::getMessageId, ids);

        return feedbackMapper.selectList(wrapper).stream()
                .collect(Collectors.toMap(
                        AnswerFeedback::getMessageId,
                        AnswerFeedback::getFeedbackType,
                        (left, right) -> right,
                        LinkedHashMap::new
                ));
    }

    private void recordLowQualityQuestion(ChatMessage aiMessage, String reasonType, String reason, Long userId) {
        ChatSession session = chatSessionMapper.selectById(aiMessage.getSessionId());
        if (session == null) {
            return;
        }

        ChatMessage userMessage = chatMessageService.findPreviousUserMessage(session.getId(), aiMessage.getId());
        String question = userMessage != null ? userMessage.getContent() : "未知问题";

        failedQuestionService.record(
                userId,
                session.getKnowledgeBaseId(),
                session.getId(),
                question,
                resolveFailureType(reasonType),
                reason
        );
    }

    private String resolveFailureType(String reasonType) {
        if (reasonType == null || reasonType.isBlank()) {
            return "LOW_QUALITY";
        }

        String normalized = reasonType.trim().toUpperCase();
        if ("NO_HIT".equals(normalized)) {
            return "NO_HIT";
        }
        if ("INSUFFICIENT_CITATION".equals(normalized)) {
            return "INSUFFICIENT_CITATION";
        }
        if ("MODEL_ERROR".equals(normalized)) {
            return "MODEL_ERROR";
        }
        if ("LOW_QUALITY".equals(normalized)) {
            return "LOW_QUALITY";
        }
        return "LOW_QUALITY";
    }
}
