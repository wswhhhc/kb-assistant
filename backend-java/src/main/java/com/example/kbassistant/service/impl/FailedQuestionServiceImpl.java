package com.example.kbassistant.service.impl;

import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.kbassistant.dto.response.FailedQuestionStatsVO;
import com.example.kbassistant.dto.response.FailedQuestionVO;
import com.example.kbassistant.entity.FailedQuestion;
import com.example.kbassistant.mapper.FailedQuestionMapper;
import com.example.kbassistant.service.FailedQuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FailedQuestionServiceImpl implements FailedQuestionService {

    private final FailedQuestionMapper failedQuestionMapper;

    @Override
    public void save(FailedQuestion failedQuestion) {
        failedQuestionMapper.insert(failedQuestion);
    }

    @Override
    public void record(Long userId, Long knowledgeBaseId, Long sessionId, String question, String failureType, String remark) {
        FailedQuestion failedQuestion = new FailedQuestion();
        failedQuestion.setUserId(userId);
        failedQuestion.setKnowledgeBaseId(knowledgeBaseId);
        failedQuestion.setSessionId(sessionId);
        failedQuestion.setQuestion(question);
        failedQuestion.setFailureType(failureType);
        failedQuestion.setRemark(remark);
        failedQuestionMapper.insert(failedQuestion);
    }

    @Override
    public IPage<FailedQuestionVO> page(int pageNum, int pageSize, Long kbId, String failureType, String status) {
        Page<FailedQuestionVO> page = new Page<>(pageNum, pageSize);
        page.addOrder(OrderItem.desc("created_at"));

        if (kbId != null || (failureType != null && !failureType.isEmpty()) || (status != null && !status.isEmpty())) {
            return failedQuestionMapper.selectVoPageWithFilter(page, kbId, failureType, status);
        }
        return failedQuestionMapper.selectVoPage(page);
    }

    @Override
    public FailedQuestionStatsVO getStats() {
        FailedQuestionStatsVO stats = new FailedQuestionStatsVO();
        stats.setTotalCount(failedQuestionMapper.countTotal());
        stats.setNoHitCount(failedQuestionMapper.countByType("NO_HIT"));
        stats.setLowQualityCount(failedQuestionMapper.countByType("LOW_QUALITY"));
        stats.setModelErrorCount(failedQuestionMapper.countByType("MODEL_ERROR"));
        stats.setInsufficientCitationCount(failedQuestionMapper.countByType("INSUFFICIENT_CITATION"));
        stats.setPendingCount(failedQuestionMapper.countPending());
        return stats;
    }

    @Override
    public void updateStatus(Long id, String status, String resolution, Long resolvedBy) {
        FailedQuestion entity = new FailedQuestion();
        entity.setId(id);
        entity.setStatus(status);
        entity.setResolution(resolution);
        entity.setResolvedBy(resolvedBy);
        if ("RESOLVED".equals(status) || "DISMISSED".equals(status)) {
            entity.setResolvedAt(java.time.LocalDateTime.now());
        }
        failedQuestionMapper.updateById(entity);
    }
}
