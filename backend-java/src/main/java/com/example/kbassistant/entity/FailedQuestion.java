package com.example.kbassistant.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("failed_question")
public class FailedQuestion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long knowledgeBaseId;
    private Long sessionId;
    private String question;
    private String failureType;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
