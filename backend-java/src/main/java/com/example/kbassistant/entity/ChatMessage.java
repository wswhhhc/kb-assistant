package com.example.kbassistant.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_message")
public class ChatMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private String role;
    private String content;
    private String modelName;
    private Integer retrievalCount;
    private String citationJson;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
