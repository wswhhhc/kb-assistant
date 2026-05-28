package com.example.kbassistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.kbassistant.dto.response.ChatSessionListItemVO;
import com.example.kbassistant.entity.ChatSession;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ChatSessionMapper extends BaseMapper<ChatSession> {

    @Select("""
            SELECT
                cs.id,
                cs.user_id,
                cs.knowledge_base_id,
                cs.title,
                kb.name AS kbName,
                COUNT(cm.id) AS msgCount,
                cs.created_at,
                cs.updated_at
            FROM chat_session cs
            LEFT JOIN knowledge_base kb ON cs.knowledge_base_id = kb.id
            LEFT JOIN chat_message cm ON cs.id = cm.session_id
            WHERE cs.user_id = #{userId}
              AND (#{knowledgeBaseId} IS NULL OR cs.knowledge_base_id = #{knowledgeBaseId})
            GROUP BY cs.id, cs.user_id, cs.knowledge_base_id, cs.title, kb.name, cs.created_at, cs.updated_at
            ORDER BY cs.updated_at DESC
            """)
    IPage<ChatSessionListItemVO> selectSessionListPage(IPage<ChatSessionListItemVO> page,
                                                       @Param("userId") Long userId,
                                                       @Param("knowledgeBaseId") Long knowledgeBaseId);
}
