-- ============================================================
-- 企业知识库智能助手 —— 数据库初始化脚本 V1
-- 数据库：MySQL 8.x
-- 字符集：utf8mb4（支持中文和 emoji）
-- ============================================================

CREATE DATABASE IF NOT EXISTS `knowledge_base_assistant`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE `knowledge_base_assistant`;

-- ============================================================
-- 1. 用户表
-- ============================================================
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键 ID',
    `username`      VARCHAR(50)   NOT NULL                 COMMENT '用户名',
    `password_hash` VARCHAR(255)  NOT NULL                 COMMENT '密码哈希（BCrypt）',
    `real_name`     VARCHAR(50)   DEFAULT NULL             COMMENT '真实姓名',
    `email`         VARCHAR(100)  DEFAULT NULL             COMMENT '邮箱地址',
    `role`          VARCHAR(20)   NOT NULL DEFAULT 'USER'  COMMENT '角色：ADMIN / USER',
    `status`        VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE / DISABLED',
    `created_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_user_username` (`username`),
    KEY `idx_sys_user_role` (`role`),
    KEY `idx_sys_user_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================================
-- 2. 知识库表
-- ============================================================
DROP TABLE IF EXISTS `knowledge_base`;
CREATE TABLE `knowledge_base` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键 ID',
    `name`          VARCHAR(100)  NOT NULL                 COMMENT '知识库名称',
    `description`   VARCHAR(500)  DEFAULT NULL             COMMENT '知识库描述',
    `owner_user_id` BIGINT        NOT NULL                 COMMENT '创建人 ID',
    `scope`         VARCHAR(10)   NOT NULL DEFAULT 'PRIVATE' COMMENT '可见范围：PUBLIC-公开(全员可见), PRIVATE-私有(仅管理员和创建者可见)',
    `status`        VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE / DISABLED',
    `is_deleted`    TINYINT(1)    NOT NULL DEFAULT 0       COMMENT '逻辑删除标记：0 未删除 1 已删除',
    `deleted_at`    DATETIME      DEFAULT NULL             COMMENT '删除时间',
    `created_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_kb_owner_user_id` (`owner_user_id`),
    KEY `idx_kb_status` (`status`),
    KEY `idx_kb_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库表';

-- ============================================================
-- 3. 知识库成员表
-- ============================================================
DROP TABLE IF EXISTS `knowledge_base_member`;
CREATE TABLE `knowledge_base_member` (
    `id`                BIGINT      NOT NULL AUTO_INCREMENT  COMMENT '主键 ID',
    `knowledge_base_id` BIGINT      NOT NULL                 COMMENT '知识库 ID',
    `user_id`           BIGINT      NOT NULL                 COMMENT '用户 ID',
    `member_role`       VARCHAR(20) NOT NULL DEFAULT 'MEMBER' COMMENT '成员角色：MEMBER',
    `created_at`        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_kb_member_kb_user` (`knowledge_base_id`, `user_id`),
    KEY `idx_kb_member_user_id` (`user_id`),
    KEY `idx_kb_member_kb_id` (`knowledge_base_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库成员表';

-- ============================================================
-- 4. 文档表
-- ============================================================
DROP TABLE IF EXISTS `document`;
CREATE TABLE `document` (
    `id`                BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键 ID',
    `knowledge_base_id` BIGINT        NOT NULL                 COMMENT '所属知识库 ID',
    `file_name`         VARCHAR(255)  NOT NULL                 COMMENT '文件名称',
    `file_type`         VARCHAR(20)   NOT NULL                 COMMENT '文件类型：PDF / DOCX / MD / TXT',
    `file_path`         VARCHAR(500)  NOT NULL                 COMMENT '文件存储路径',
    `file_size`         BIGINT        DEFAULT 0                COMMENT '文件大小（字节）',
    `parse_status`      VARCHAR(20)   NOT NULL DEFAULT 'UPLOADED' COMMENT '处理状态',
    `page_count`        INT           DEFAULT NULL             COMMENT '页数，非分页文档为 NULL',
    `chunk_count`       INT           DEFAULT 0                COMMENT '切片数量',
    `error_message`     VARCHAR(1000) DEFAULT NULL             COMMENT '处理失败原因',
    `created_by`        BIGINT        NOT NULL                 COMMENT '上传人 ID',
    `is_deleted`        TINYINT(1)    NOT NULL DEFAULT 0       COMMENT '逻辑删除标记',
    `deleted_at`        DATETIME      DEFAULT NULL             COMMENT '删除时间',
    `created_at`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_doc_kb_id` (`knowledge_base_id`),
    KEY `idx_doc_status` (`parse_status`),
    KEY `idx_doc_created_by` (`created_by`),
    KEY `idx_doc_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档表';

-- ============================================================
-- 5. 文档切片表
-- ============================================================
DROP TABLE IF EXISTS `document_chunk`;
CREATE TABLE `document_chunk` (
    `id`                BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键 ID',
    `document_id`       BIGINT        NOT NULL                 COMMENT '文档 ID',
    `knowledge_base_id` BIGINT        NOT NULL                 COMMENT '知识库 ID',
    `chunk_index`       INT           NOT NULL                 COMMENT '切片序号',
    `section_title`     VARCHAR(255)  DEFAULT NULL             COMMENT '所属章节标题',
    `content`           LONGTEXT      NOT NULL                 COMMENT '切片内容',
    `page_no`           INT           DEFAULT NULL             COMMENT '所属页码',
    `char_count`        INT           DEFAULT 0                COMMENT '字符数',
    `token_count`       INT           DEFAULT 0                COMMENT 'Token 数估算值',
    `vector_id`         VARCHAR(100)  DEFAULT NULL             COMMENT 'Qdrant point ID',
    `created_at`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_chunk_doc_index` (`document_id`, `chunk_index`),
    KEY `idx_chunk_doc_id` (`document_id`),
    KEY `idx_chunk_kb_id` (`knowledge_base_id`),
    FULLTEXT KEY `ft_chunk_content` (`content`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档切片表';

-- ============================================================
-- 6. 会话表
-- ============================================================
DROP TABLE IF EXISTS `chat_session`;
CREATE TABLE `chat_session` (
    `id`                BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键 ID',
    `user_id`           BIGINT        NOT NULL                 COMMENT '用户 ID',
    `knowledge_base_id` BIGINT        NOT NULL                 COMMENT '所属知识库 ID',
    `title`             VARCHAR(255)  DEFAULT NULL             COMMENT '会话标题',
    `created_at`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_session_user_id` (`user_id`),
    KEY `idx_session_kb_id` (`knowledge_base_id`),
    KEY `idx_session_updated_at` (`updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话表';

-- ============================================================
-- 7. 消息表
-- ============================================================
DROP TABLE IF EXISTS `chat_message`;
CREATE TABLE `chat_message` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键 ID',
    `session_id`      BIGINT        NOT NULL                 COMMENT '会话 ID',
    `role`            VARCHAR(20)   NOT NULL                 COMMENT '消息角色：USER / ASSISTANT',
    `content`         LONGTEXT      NOT NULL                 COMMENT '消息内容',
    `model_name`      VARCHAR(100)  DEFAULT NULL             COMMENT '回答使用的模型名称',
    `retrieval_count` INT           DEFAULT 0                COMMENT '召回片段数量',
    `citation_json`   JSON          DEFAULT NULL             COMMENT '引用来源 JSON',
    `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_msg_session_id` (`session_id`),
    KEY `idx_msg_role` (`role`),
    KEY `idx_msg_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表';

-- ============================================================
-- 8. 答案反馈表
-- ============================================================
DROP TABLE IF EXISTS `answer_feedback`;
CREATE TABLE `answer_feedback` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键 ID',
    `message_id`    BIGINT        NOT NULL                 COMMENT '回答消息 ID',
    `user_id`       BIGINT        NOT NULL                 COMMENT '反馈用户 ID',
    `feedback_type` VARCHAR(20)   NOT NULL                 COMMENT '反馈类型：LIKE / DISLIKE',
    `reason`        VARCHAR(500)  DEFAULT NULL             COMMENT '点踩原因或备注',
    `created_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_feedback_message_user` (`message_id`, `user_id`),
    KEY `idx_feedback_user_id` (`user_id`),
    KEY `idx_feedback_type` (`feedback_type`),
    KEY `idx_feedback_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='答案反馈表';

-- ============================================================
-- 9. 失败问题表
-- ============================================================
DROP TABLE IF EXISTS `failed_question`;
CREATE TABLE `failed_question` (
    `id`                BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键 ID',
    `user_id`           BIGINT        DEFAULT NULL             COMMENT '提问用户 ID',
    `knowledge_base_id` BIGINT        NOT NULL                 COMMENT '知识库 ID',
    `session_id`        BIGINT        DEFAULT NULL             COMMENT '会话 ID',
    `question`          VARCHAR(1000) NOT NULL                 COMMENT '失败问题内容',
    `failure_type`      VARCHAR(30)   NOT NULL                 COMMENT '失败类型：NO_HIT / LOW_QUALITY / INSUFFICIENT_CITATION / MODEL_ERROR',
    `remark`            VARCHAR(1000) DEFAULT NULL             COMMENT '失败说明',
    `status`            VARCHAR(20)   NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING-待处理, REVIEWED-已查看, RESOLVED-已解决, DISMISSED-已忽略',
    `resolution`        VARCHAR(500)  DEFAULT NULL             COMMENT '处理说明',
    `resolved_at`       DATETIME      DEFAULT NULL             COMMENT '处理时间',
    `resolved_by`       BIGINT        DEFAULT NULL             COMMENT '处理人 ID',
    `created_at`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_failed_kb_id` (`knowledge_base_id`),
    KEY `idx_failed_user_id` (`user_id`),
    KEY `idx_failed_type` (`failure_type`),
    KEY `idx_failed_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='失败问题表';

-- ============================================================
-- 10. 热门问题统计表（V2 预留）
-- ============================================================
DROP TABLE IF EXISTS `hot_question_stat`;
CREATE TABLE `hot_question_stat` (
    `id`                BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键 ID',
    `knowledge_base_id` BIGINT        NOT NULL                 COMMENT '知识库 ID',
    `question`          VARCHAR(1000) NOT NULL                 COMMENT '问题内容',
    `ask_count`         INT           NOT NULL DEFAULT 0       COMMENT '提问次数',
    `last_asked_at`     DATETIME      DEFAULT NULL             COMMENT '最近提问时间',
    `updated_at`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_hot_kb_id` (`knowledge_base_id`),
    KEY `idx_hot_ask_count` (`ask_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='热门问题统计表（V2）';

-- ============================================================
-- 初始化数据：创建默认管理员账号
-- ============================================================
INSERT INTO `sys_user` (`username`, `password_hash`, `real_name`, `role`, `status`)
VALUES ('admin', '$2a$10$.V/ETsxVXaaNkiwxPneHUOA0zVaQCkk5jE7UuRXDqUyE.suFn8l56', '系统管理员', 'ADMIN', 'ACTIVE');
-- 默认密码：admin123（BCrypt 哈希），请上线后立即修改
