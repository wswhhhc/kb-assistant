# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

企业知识库智能助手是一个面向企业内部场景的 RAG 知识库问答平台。支持文档上传 → 解析 → 切片 → 向量化 → 智能问答的完整链路，提供引用溯源、会话历史、反馈闭环等功能。

项目定位：个人全栈项目，可用于实习面试展示。

## 技术栈

| 层 | 技术 | 说明 |
|---|------|------|
| 前端 | Vue 3 + Element Plus + Pinia + vue-router | UI 组件库用 Element Plus |
| 业务后端 | Spring Boot + MyBatis-Plus + JWT | ORM 用 MyBatis-Plus，认证用 JWT |
| AI 服务 | FastAPI + PyMuPDF + python-docx + qdrant-client | 独立的 AI 链路服务 |
| 数据库 | MySQL 8.x | 存储业务数据和结构化元数据 |
| 缓存 | Redis | 登录态、热点数据、处理状态 |
| 向量库 | Qdrant | 存储 chunk 向量，支持 knowledge_base_id 过滤检索 |
| 模型平台 | SiliconFlow API | 提供 Embedding（BGE-M3）和生成（DeepSeek） |

## 架构概览

```
用户 → Vue 3 前端 → Spring Boot (REST API) ─→ MySQL / Redis
                  ↘→ FastAPI (AI 服务) ─→ Qdrant / SiliconFlow API
```

关键链路：
- **文档入库链路**：上传 → 解析 → 切片 → Embedding → 写入 Qdrant + MySQL
- **问答链路**：提问 → 向量化 → Qdrant 召回 → 拼接 Prompt → DeepSeek 生成 → 引用返回

服务间文件传输采用 **共享卷 + 文件路径** 方式（Spring Boot 存文件，传路径给 FastAPI 读取）。

## 数据库核心表

- `sys_user` — 用户（含 email、逻辑删除预留）
- `knowledge_base` — 知识库（含 is_deleted 逻辑删除）
- `knowledge_base_member` — 成员关系（唯一索引 kb_id + user_id）
- `document` — 文档元数据（含 is_deleted 逻辑删除、parse_status 状态流转）
- `document_chunk` — 切片元数据（vector_id 关联 Qdrant point）
- `chat_session` / `chat_message` — 会话与消息
- `answer_feedback` — 答案反馈（唯一索引 message_id + user_id）
- `failed_question` — 失败问题记录
- `hot_question_stat` — V2 预留

文档状态流转：`UPLOADED → PARSING → CHUNKING → EMBEDDING → READY / FAILED`

## 项目目录结构（规划）

```
/项目根目录
├── frontend/            # Vue 3 前端项目
├── backend-java/        # Spring Boot 业务服务
│   ├── controller/      # REST 接口层
│   ├── service/         # 业务逻辑层
│   ├── mapper/          # MyBatis-Plus Mapper
│   ├── entity/          # 数据实体
│   ├── dto/             # 请求/响应 DTO
│   ├── config/          # 配置类（安全、跨域、异常处理）
│   └── util/            # 工具类（JWT、密码加密）
├── backend-python/      # FastAPI AI 服务
│   ├── routers/         # 接口路由
│   ├── services/        # 文档解析、切片、向量化、问答
│   └── config/          # 模型配置
├── sql/                 # 建表脚本
├── docker/              # Docker Compose 编排
└── docs/                # 设计文档
```

## 开发阶段（共 6 阶段，预计 3-4 周）

1. **项目初始化**（1-2 天）— 骨架搭建、Docker Compose 基础配置
2. **基础业务模块**（3-5 天）— 建表、登录、知识库 CRUD、成员权限
3. **文档入库链路**（3-5 天）— 上传、解析、切片、向量化
4. **RAG 问答链路**（3-5 天）— 检索、答案生成、引用溯源、会话
5. **闭环优化**（2-3 天）— 反馈、失败问题、统计
6. **测试部署**（3-4 天）— 联调、Docker 部署、面试材料

## V1 设计边界

- PDF 解析只保证纯文本型文档，表格/多栏/扫描件不做精确还原
- 切片用固定长度（500 字 + 50 overlap）兜底，标题感知切面作为后续优化
- 文档处理是同步的（2 分钟超时），不引入消息队列
- 搜索不做 rerank（标记为 V2），回答质量在"能出东西"层面
- 知识库名称不做数据库强唯一，业务层校验
- Embedding 维度暂定 1024，开发时确认 SiliconFlow 实际输出

## 文档索引

- `需求文档.docx` — 功能需求、业务规则、用户角色、验收标准
- `系统设计文档.md` — 架构设计、模块划分、核心流程（Mermaid 图）、接口列表
- `数据库设计文档.md` — 表结构、字段定义、索引、Qdrant/Redis 设计
- `项目开发计划文档.md` — 开发计划、阶段划分、技术选型表、风险控制
- `sql/init_schema.sql` — MySQL 建表脚本（含索引、约束、管理员初始数据）
- `docs/项目目录结构说明.md` — 前后端完整目录树、包名规范、各模块职责说明
