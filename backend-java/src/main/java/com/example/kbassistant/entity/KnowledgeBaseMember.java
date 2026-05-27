package com.example.kbassistant.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("knowledge_base_member")
public class KnowledgeBaseMember {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long knowledgeBaseId;
    private Long userId;
    private String memberRole;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
