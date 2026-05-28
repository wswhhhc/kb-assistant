# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

企业知识库智能助手是一个面向企业内部场景的 RAG 知识库问答平台。支持文档上传 → 解析 → 切片 → 向量化 → 智能问答的完整链路，提供引用溯源、会话历史、反馈闭环等功能。

项目定位：个人全栈项目，可用于实习面试展示。

## 技术栈

| 层 | 技术 | 说明 |
|---|------|------|
| 前端 | Vue 3 + Element Plus + Pinia + vue-router + Vite | 组件库 Element Plus |
| 业务后端 | Spring Boot 3.4 + MyBatis-Plus + JWT (jjwt 0.12) | ORM 用 MyBatis-Plus，认证用 JWT |
| AI 服务 | FastAPI + PyMuPDF + python-docx + qdrant-client + httpx | 独立的 AI 链路服务 |
| 数据库 | MySQL 8.x | 业务数据与结构化元数据 |
| 缓存 | Redis | 登录态、热点数据 |
| 向量库 | Qdrant | chunk 向量存储与 knowledge_base_id 过滤检索 |
| 模型平台 | SiliconFlow API | Embedding（BGE-M3 1024维）+ 生成（Qwen2.5-7B / DeepSeek-V4） |

## 架构概览

```
用户 → Vue 3 前端 (dev:3000) → Spring Boot (REST API 8080) ─→ MySQL / Redis
                                 ↘→ FastAPI (AI 服务 8000) ─→ Qdrant / SiliconFlow
```

关键链路：
- **文档入库链路**：上传 → 解析 → 切片 → Embedding → 写入 Qdrant + MySQL
- **问答链路**：提问 → 向量化 → Qdrant 召回 → 拼接 Prompt → LLM 生成 → 引用返回
- 服务间文件传输：**共享卷 + 文件路径**（Spring Boot 存文件，传路径给 FastAPI 读取）

## 启动命令

```bash
# 一键启动（前端 + FastAPI，不含 Spring Boot）
start.bat

# 停止
stop.bat

# 前端单独启动
cd frontend && npm run dev

# FastAPI 单独启动
cd backend-python && python -m uvicorn app.main:app --reload --host 0.0.0.0 --port 8000

# Spring Boot（通过 IntelliJ 或 Maven）
cd backend-java && mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 前端依赖安装
cd frontend && npm install

# Python 依赖安装
cd backend-python && pip install -r requirements.txt
```

所有服务启动后访问 `http://localhost:3000`，测试账号 admin / admin123。

## 数据库核心表

- `sys_user` — 用户（含 password_hash BCrypt 加密）
- `knowledge_base` — 知识库（is_deleted 逻辑删除）
- `knowledge_base_member` — 成员关系（唯一索引 kb_id + user_id）
- `document` — 文档（is_deleted + parse_status 状态流转）
- `document_chunk` — 切片（vector_id 关联 Qdrant point）
- `chat_session` / `chat_message` — 会话消息（citation_json 存引用）
- `answer_feedback` — 答案反馈（唯一索引 message_id + user_id）
- `failed_question` — 失败问题

文档状态：`UPLOADED → PARSING → CHUNKING → EMBEDDING → READY / FAILED`

## API 端点

### Spring Boot (port 8080, 前缀 `/api`)

| 模块 | 端点 | 说明 |
|------|------|------|
| Auth | `POST /auth/login`, `GET /auth/me` | JWT 登录、当前用户 |
| Users | `GET /users`, `POST /users` | 用户管理（管理员） |
| KBs | `GET/POST/PUT/DELETE /knowledge-bases` | 知识库 CRUD |
| Members | `GET/POST/DELETE /knowledge-bases/{id}/members` | 成员管理 |
| Documents | `GET/POST/DELETE /knowledge-bases/{id}/documents` | 文档管理 |
| Process | `POST /documents/{id}/process` | 触发文档处理 |
| Chat | `POST /chat/ask`, `POST /chat/sessions` | 问答与会话 |
| Feedback | `POST /feedback`, `GET /feedback` | 点赞/点踩 |
| Dashboard | `GET /dashboard/statistics` | 统计 |

### FastAPI (port 8000, 内部调用)

| 端点 | 说明 |
|------|------|
| `POST /internal/document/process` | 文档处理（解析→切片→向量化→入库） |
| `POST /internal/chat/ask` | 非流式问答 |
| `POST /internal/chat/ask-stream` | 流式问答（SSE 格式） |
| `GET /health` | 健康检查 |

## 前端架构

```
src/
├── api/          # axios 接口封装
├── views/        # 页面组件（login, dashboard, knowledgeBase, document, chat, feedback...）
├── router/       # vue-router 路由 + 守卫
├── stores/       # Pinia 状态管理（auth, chat）
├── components/   # 通用组件（AppLayout, StatusTag, CitationPanel）
└── utils/        # request.js（axios 拦截器）, auth.js
```

Vite 开发服务器在 3000 端口，代理 `/api` → Spring Boot 8080，`/internal` → FastAPI 8000。

## 项目目录

```
├── frontend/              # Vue 3 前端
├── backend-java/          # Spring Boot 业务服务
│   ├── controller/        # REST 接口
│   ├── service/impl/      # 业务逻辑
│   ├── mapper/            # MyBatis-Plus Mapper
│   ├── entity/            # 数据实体
│   ├── dto/               # 请求/响应 DTO
│   ├── config/            # 配置类
│   ├── security/          # JWT 过滤器
│   └── client/            # FastAPI HTTP 客户端
├── backend-python/        # FastAPI AI 服务
│   ├── routers/           # 接口路由
│   ├── services/          # 解析、切片、向量化、问答
│   │   └── parser/        # PDF/DOCX/TXT/MD 解析器
│   ├── schemas/           # Pydantic 模型
│   └── config/            # 模型配置（.env）
├── sql/                   # 建表脚本
├── docker/                # Docker Compose 编排
├── data/files/            # 上传文件存储
└── docs/                  # 设计文档
```

## V1 设计边界

- PDF 解析只保证纯文本型文档，不处理表格/多栏/扫描件
- 切片用 500 字 + 50 overlap 固定长度
- 文档处理同步（2 分钟超时），不引入消息队列
- 搜索不做 rerank
- Embedding 维度 1024（BGE-M3）

## 常见开发模式

- **新增页面**：`views/` 下创建 → `router/index.js` 添加路由 → `api/` 下添加接口 → 后端 `controller/service/mapper/entity`
- **实体类**：使用 `@Data` + `@TableName` + `@TableId` 注解，逻辑删除字段 `is_deleted` + `@TableLogic`
- **分页**：后端返回 `PageResult<T>`（records/total/pageNum/pageSize），前端用 `el-pagination`
- **配置文件**：`application-dev.yml` 本地开发，`application-prod.yml` Docker 部署，Python 端用 `.env`
