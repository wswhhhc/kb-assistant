package com.example.kbassistant.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("document_chunk")
public class DocumentChunk {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long documentId;
    private Long knowledgeBaseId;
    private Integer chunkIndex;
    private String sectionTitle;
    private String content;
    private Integer pageNo;
    private Integer charCount;
    private Integer tokenCount;
    private String vectorId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
