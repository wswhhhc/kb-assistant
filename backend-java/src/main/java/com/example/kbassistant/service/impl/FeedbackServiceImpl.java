package com.example.kbassistant.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.kbassistant.dto.request.FeedbackRequest;
import com.example.kbassistant.entity.AnswerFeedback;
import com.example.kbassistant.mapper.AnswerFeedbackMapper;
import com.example.kbassistant.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    private final AnswerFeedbackMapper feedbackMapper;

    @Override
    public void submit(FeedbackRequest request, Long userId) {
        AnswerFeedback feedback = new AnswerFeedback();
        feedback.setMessageId(request.getMessageId());
        feedback.setUserId(userId);
        feedback.setFeedbackType(request.getFeedbackType());
        feedback.setReason(request.getReason());
        feedbackMapper.insert(feedback);
    }

    @Override
    public IPage<AnswerFeedback> page(int pageNum, int pageSize) {
        return feedbackMapper.selectPage(new Page<>(pageNum, pageSize), null);
    }
}
