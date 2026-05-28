package com.example.kbassistant.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.kbassistant.dto.request.FeedbackRequest;
import com.example.kbassistant.entity.AnswerFeedback;

import java.util.Map;

public interface FeedbackService {
    void submit(FeedbackRequest request, Long userId);
    IPage<AnswerFeedback> page(int pageNum, int pageSize);
    Map<Long, String> getMyFeedbackMap(String messageIds, Long userId);
}
