package com.example.kbassistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.kbassistant.common.BusinessException;
import com.example.kbassistant.entity.KnowledgeBaseMember;
import com.example.kbassistant.mapper.KnowledgeBaseMemberMapper;
import com.example.kbassistant.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final KnowledgeBaseMemberMapper memberMapper;

    @Override
    public List<KnowledgeBaseMember> listByKnowledgeBaseId(Long knowledgeBaseId) {
        LambdaQueryWrapper<KnowledgeBaseMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeBaseMember::getKnowledgeBaseId, knowledgeBaseId);
        return memberMapper.selectList(wrapper);
    }

    @Override
    public void addMember(Long knowledgeBaseId, Long userId) {
        LambdaQueryWrapper<KnowledgeBaseMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeBaseMember::getKnowledgeBaseId, knowledgeBaseId)
                .eq(KnowledgeBaseMember::getUserId, userId);
        if (memberMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("该用户已是成员");
        }
        KnowledgeBaseMember member = new KnowledgeBaseMember();
        member.setKnowledgeBaseId(knowledgeBaseId);
        member.setUserId(userId);
        memberMapper.insert(member);
    }

    @Override
    public void removeMember(Long knowledgeBaseId, Long userId) {
        LambdaQueryWrapper<KnowledgeBaseMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeBaseMember::getKnowledgeBaseId, knowledgeBaseId)
                .eq(KnowledgeBaseMember::getUserId, userId);
        memberMapper.delete(wrapper);
    }

    @Override
    public boolean isMember(Long knowledgeBaseId, Long userId) {
        LambdaQueryWrapper<KnowledgeBaseMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeBaseMember::getKnowledgeBaseId, knowledgeBaseId)
                .eq(KnowledgeBaseMember::getUserId, userId);
        return memberMapper.selectCount(wrapper) > 0;
    }

    @Override
    public List<Long> getUserKnowledgeBaseIds(Long userId) {
        LambdaQueryWrapper<KnowledgeBaseMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeBaseMember::getUserId, userId);
        return memberMapper.selectList(wrapper)
                .stream()
                .map(KnowledgeBaseMember::getKnowledgeBaseId)
                .collect(Collectors.toList());
    }
}
