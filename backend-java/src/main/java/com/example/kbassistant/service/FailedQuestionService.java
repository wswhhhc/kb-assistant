package com.example.kbassistant.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.kbassistant.entity.FailedQuestion;

public interface FailedQuestionService {
    void save(FailedQuestion failedQuestion);
    IPage<FailedQuestion> page(int pageNum, int pageSize);
}
