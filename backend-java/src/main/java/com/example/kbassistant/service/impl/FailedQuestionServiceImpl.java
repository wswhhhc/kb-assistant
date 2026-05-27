package com.example.kbassistant.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
    public IPage<FailedQuestion> page(int pageNum, int pageSize) {
        return failedQuestionMapper.selectPage(new Page<>(pageNum, pageSize), null);
    }
}
