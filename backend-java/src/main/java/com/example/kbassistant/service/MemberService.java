package com.example.kbassistant.service;

import com.example.kbassistant.entity.KnowledgeBaseMember;

import java.util.List;

public interface MemberService {
    List<KnowledgeBaseMember> listByKnowledgeBaseId(Long knowledgeBaseId);
    void addMember(Long knowledgeBaseId, Long userId);
    void removeMember(Long knowledgeBaseId, Long userId);
    boolean isMember(Long knowledgeBaseId, Long userId);
    List<Long> getUserKnowledgeBaseIds(Long userId);
}
