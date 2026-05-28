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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private final KnowledgeBaseMapper kbMapper;
    private final MemberService memberService;

    @Override
    public KnowledgeBase getById(Long id, Long userId, boolean isAdmin) {
        KnowledgeBase kb = kbMapper.selectById(id);
        if (kb == null) {
            throw new BusinessException("知识库不存在");
        }
        if (!canAccess(kb, userId, isAdmin)) {
            throw new BusinessException("无权限访问该知识库");
        }
        return kb;
    }

    @Override
    public IPage<KnowledgeBase> page(int pageNum, int pageSize, Long userId, boolean isAdmin) {
        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        if (!isAdmin) {
            wrapper.and(w -> w.eq(KnowledgeBase::getScope, "PUBLIC")
                    .or(inner -> inner.eq(KnowledgeBase::getScope, "PRIVATE")
                            .eq(KnowledgeBase::getOwnerUserId, userId)));
        }
        wrapper.orderByDesc(KnowledgeBase::getCreatedAt);
        return kbMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public List<KnowledgeBase> listByUserId(Long userId) {
        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeBase::getScope, "PUBLIC")
                .or(w -> w.eq(KnowledgeBase::getScope, "PRIVATE")
                        .eq(KnowledgeBase::getOwnerUserId, userId));
        return kbMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public KnowledgeBase create(KnowledgeBaseCreateRequest request, Long userId, boolean isAdmin) {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setName(request.getName());
        kb.setDescription(request.getDescription());
        kb.setOwnerUserId(userId);
        kb.setScope(isAdmin ? "PUBLIC" : "PRIVATE");
        kbMapper.insert(kb);

        // 自动将创建者添加为成员
        memberService.addMember(kb.getId(), userId);

        return kb;
    }

    @Override
    public void update(Long id, KnowledgeBaseCreateRequest request, Long userId, boolean isAdmin) {
        KnowledgeBase kb = kbMapper.selectById(id);
        if (kb == null) {
            throw new BusinessException("知识库不存在");
        }
        if (!isAdmin && !kb.getOwnerUserId().equals(userId)) {
            throw new BusinessException("无权限修改该知识库");
        }
        kb.setName(request.getName());
        kb.setDescription(request.getDescription());
        kbMapper.updateById(kb);
    }

    @Override
    public void delete(Long id, Long userId, boolean isAdmin) {
        KnowledgeBase kb = kbMapper.selectById(id);
        if (kb == null) {
            throw new BusinessException("知识库不存在");
        }
        if (!isAdmin && !kb.getOwnerUserId().equals(userId)) {
            throw new BusinessException("无权限删除该知识库");
        }
        kbMapper.deleteById(id);
    }

    private boolean canAccess(KnowledgeBase kb, Long userId, boolean isAdmin) {
        if (isAdmin) return true;
        if ("PUBLIC".equals(kb.getScope())) return true;
        return kb.getOwnerUserId().equals(userId);
    }
}
