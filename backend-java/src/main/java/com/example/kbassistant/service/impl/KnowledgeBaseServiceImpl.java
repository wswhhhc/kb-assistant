package com.example.kbassistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.kbassistant.common.BusinessException;
import com.example.kbassistant.dto.request.KnowledgeBaseCreateRequest;
import com.example.kbassistant.entity.KnowledgeBase;
import com.example.kbassistant.mapper.KnowledgeBaseMapper;
import com.example.kbassistant.service.KnowledgeBaseService;
import com.example.kbassistant.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private final KnowledgeBaseMapper kbMapper;
    private final MemberService memberService;

    @Override
    public KnowledgeBase getById(Long id) {
        return kbMapper.selectById(id);
    }

    @Override
    public IPage<KnowledgeBase> page(int pageNum, int pageSize, Long userId, boolean isAdmin) {
        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        if (!isAdmin) {
            List<Long> kbIds = memberService.getUserKnowledgeBaseIds(userId);
            if (kbIds.isEmpty()) {
                return new Page<>(pageNum, pageSize);
            }
            wrapper.in(KnowledgeBase::getId, kbIds);
        }
        wrapper.orderByDesc(KnowledgeBase::getCreatedAt);
        return kbMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public List<KnowledgeBase> listByUserId(Long userId) {
        List<Long> kbIds = memberService.getUserKnowledgeBaseIds(userId);
        if (kbIds.isEmpty()) {
            return List.of();
        }
        return kbMapper.selectBatchIds(kbIds);
    }

    @Override
    public KnowledgeBase create(KnowledgeBaseCreateRequest request, Long userId) {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setName(request.getName());
        kb.setDescription(request.getDescription());
        kb.setOwnerUserId(userId);
        kbMapper.insert(kb);
        return kb;
    }

    @Override
    public void update(Long id, KnowledgeBaseCreateRequest request) {
        KnowledgeBase kb = kbMapper.selectById(id);
        if (kb == null) {
            throw new BusinessException("知识库不存在");
        }
        kb.setName(request.getName());
        kb.setDescription(request.getDescription());
        kbMapper.updateById(kb);
    }

    @Override
    public void delete(Long id) {
        kbMapper.deleteById(id);
    }
}
