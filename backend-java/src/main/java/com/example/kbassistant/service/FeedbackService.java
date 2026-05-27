package com.example.kbassistant.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.kbassistant.dto.request.FeedbackRequest;
import com.example.kbassistant.entity.AnswerFeedback;

public interface FeedbackService {
    void submit(FeedbackRequest request, Long userId);
    IPage<AnswerFeedback> page(int pageNum, int pageSize);
}
