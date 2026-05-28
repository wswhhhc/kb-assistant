package com.example.kbassistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.kbassistant.dto.response.FailedQuestionVO;
import com.example.kbassistant.entity.FailedQuestion;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface FailedQuestionMapper extends BaseMapper<FailedQuestion> {

    @Select("SELECT fq.id, fq.user_id, fq.knowledge_base_id, kb.name AS kbName, " +
            "fq.session_id, fq.question, fq.failure_type, fq.remark, " +
            "fq.status, fq.resolution, fq.resolved_at, fq.resolved_by, fq.created_at " +
            "FROM failed_question fq " +
            "LEFT JOIN knowledge_base kb ON fq.knowledge_base_id = kb.id " +
            "ORDER BY fq.created_at DESC")
    IPage<FailedQuestionVO> selectVoPage(IPage<FailedQuestionVO> page);

    @Select("SELECT fq.id, fq.user_id, fq.knowledge_base_id, kb.name AS kbName, " +
            "fq.session_id, fq.question, fq.failure_type, fq.remark, " +
            "fq.status, fq.resolution, fq.resolved_at, fq.resolved_by, fq.created_at " +
            "FROM failed_question fq " +
            "LEFT JOIN knowledge_base kb ON fq.knowledge_base_id = kb.id " +
            "WHERE (#{kbId} IS NULL OR fq.knowledge_base_id = #{kbId}) " +
            "AND (#{failureType} IS NULL OR fq.failure_type = #{failureType}) " +
            "AND (#{status} IS NULL OR fq.status = #{status}) " +
            "ORDER BY fq.created_at DESC")
    IPage<FailedQuestionVO> selectVoPageWithFilter(IPage<FailedQuestionVO> page,
                                                    @Param("kbId") Long kbId,
                                                    @Param("failureType") String failureType,
                                                    @Param("status") String status);

    @Select("SELECT COUNT(*) FROM failed_question")
    long countTotal();

    @Select("SELECT COUNT(*) FROM failed_question WHERE failure_type = #{type}")
    long countByType(@Param("type") String type);

    @Select("SELECT COUNT(*) FROM failed_question WHERE status = 'PENDING'")
    long countPending();
}
