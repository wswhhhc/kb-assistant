package com.example.kbassistant.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.kbassistant.dto.response.FailedQuestionStatsVO;
import com.example.kbassistant.dto.response.FailedQuestionVO;
import com.example.kbassistant.entity.FailedQuestion;

public interface FailedQuestionService {
    void save(FailedQuestion failedQuestion);
    void record(Long userId, Long knowledgeBaseId, Long sessionId, String question, String failureType, String remark);
    IPage<FailedQuestionVO> page(int pageNum, int pageSize, Long kbId, String failureType, String status);
    FailedQuestionStatsVO getStats();
    void updateStatus(Long id, String status, String resolution, Long resolvedBy);
}
