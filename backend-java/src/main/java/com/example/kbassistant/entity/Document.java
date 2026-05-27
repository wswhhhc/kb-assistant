package com.example.kbassistant.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("document")
public class Document {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long knowledgeBaseId;
    private String fileName;
    private String fileType;
    private String filePath;
    private Long fileSize;
    private String parseStatus;
    private Integer pageCount;
    private Integer chunkCount;
    private String errorMessage;
    private Long createdBy;
    @TableLogic
    private Integer isDeleted;
    private LocalDateTime deletedAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
