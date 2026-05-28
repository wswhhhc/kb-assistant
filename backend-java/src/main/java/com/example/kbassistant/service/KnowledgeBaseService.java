package com.example.kbassistant.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.kbassistant.dto.request.KnowledgeBaseCreateRequest;
import com.example.kbassistant.entity.KnowledgeBase;

import java.util.List;

public interface KnowledgeBaseService {
    KnowledgeBase getById(Long id, Long userId, boolean isAdmin);
    IPage<KnowledgeBase> page(int pageNum, int pageSize, Long userId, boolean isAdmin);
    List<KnowledgeBase> listByUserId(Long userId);
    KnowledgeBase create(KnowledgeBaseCreateRequest request, Long userId, boolean isAdmin);
    void update(Long id, KnowledgeBaseCreateRequest request, Long userId, boolean isAdmin);
    void delete(Long id, Long userId, boolean isAdmin);
}
