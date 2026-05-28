# 企业知识库智能助手

基于 RAG（检索增强生成）的企业内部知识库智能问答平台。支持文档上传、解析、向量化入库，以及基于知识库的智能问答与引用溯源。

## 功能特性

- **用户与权限管理** — 管理员/普通用户角色，JWT 认证，知识库成员访问控制
- **知识库管理** — 创建/编辑/删除知识库，成员权限管理
- **文档管理** — 上传 PDF / DOCX / MD / TXT 文档，自动解析切片与向量化入库
- **智能问答** — 基于知识库检索 + 大模型生成答案，支持**流式输出**和引用溯源
- **引用溯源** — 每条回答附带来源文档和内容片段，可点击展开查看全文
- **反馈闭环** — 答案点赞/点踩、失败问题自动记录、数据统计面板
- **会话历史** — 多轮对话记录保存与历史查看

## 系统架构

```
用户 → Vue 3 前端 (port 3000) → Spring Boot (REST API port 8080) ─→ MySQL / Redis
                                ↘→ FastAPI (AI 服务 port 8000) ─→ Qdrant / SiliconFlow
```

**文档入库链路**：上传 → 解析（PDF/DOCX/TXT/MD）→ 切片（500字+50 overlap）→ Embedding（BGE-M3）→ 写入 Qdrant + MySQL

**问答链路**：提问 → 向量化 → Qdrant 召回（knowledge_base_id 过滤）→ 拼接 Prompt → LLM 生成 → 引用返回（流式 / 非流式）

## 技术栈

| 层 | 技术 | 说明 |
|---|------|------|
| 前端 | Vue 3 + Element Plus + Pinia + vue-router + Vite 6 | 组件库 Element Plus |
| 业务后端 | Spring Boot 3.4 + MyBatis-Plus 3.5 + JWT (jjwt 0.12) | ORM、认证分离 |
| AI 服务 | FastAPI + PyMuPDF + python-docx + qdrant-client + httpx | 独立的 AI 链路服务 |
| 数据库 | MySQL 8.x | 业务数据与结构化元数据 |
| 缓存 | Redis | 登录态、会话管理 |
| 向量库 | Qdrant | chunk 向量存储与相似度检索 |
| 模型平台 | SiliconFlow API | Embedding（BGE-M3 1024维）+ 生成（Qwen2.5-7B） |

## 项目结构

```
knowledge-base-assistant/
├── frontend/                  # Vue 3 前端
│   ├── src/
│   │   ├── api/               # axios 接口封装
│   │   ├── views/             # 页面（登录、仪表盘、知识库、文档、问答、反馈...）
│   │   ├── router/            # 路由 + 守卫
│   │   ├── stores/            # Pinia 状态管理
│   │   ├── components/        # 通用组件（AppLayout, StatusTag, CitationPanel）
│   │   └── utils/             # 请求拦截器、工具函数
│   └── vite.config.js         # Vite 配置（含 API 代理）
├── backend-java/              # Spring Boot 业务服务
│   └── src/main/java/com/example/kbassistant/
│       ├── controller/        # REST 接口层
│       ├── service/           # 业务逻辑层
│       ├── mapper/            # MyBatis-Plus Mapper
│       ├── entity/            # 数据实体
│       ├── dto/               # 请求/响应 DTO
│       ├── config/            # 配置类（安全、跨域、Jackson、MyBatis-Plus）
│       ├── security/          # JWT 过滤器 + Token 提供者
│       ├── client/            # FastAPI HTTP 客户端
│       └── common/            # 通用工具（Result, PageResult, 异常处理）
├── backend-python/            # FastAPI AI 服务
│   └── app/
│       ├── routers/           # 接口路由（文档处理、问答）
│       ├── services/          # 文档解析、切片、向量化、检索、问答生成
│       │   └── parser/        # PDF / DOCX / TXT / MD 解析器
│       ├── schemas/           # Pydantic 模型
│       └── config/            # 配置（环境变量 + .env）
├── sql/                       # 建表脚本（含初始化数据）
├── docker/                    # Docker Compose 编排
├── docs/                      # 设计文档
└── data/files/                # 上传文件存储
```

## 快速开始

### 前置要求

- JDK 21+
- Python 3.10+
- Node.js 18+
- MySQL 8.x
- Redis
- Qdrant（[Docker](https://hub.docker.com/r/qdrant/qdrant)）
- SiliconFlow API Key（[注册](https://siliconflow.cn)）

### 1. 初始化数据库

```bash
mysql -u root -p < sql/init_schema.sql
```

### 2. 配置环境变量

```bash
# Python 端
cp backend-python/.env.example backend-python/.env
# 编辑 .env，填入 SILICONFLOW_API_KEY 和 MySQL 密码
```

### 3. 启动服务

```bash
# 方式一：一键启动（前端 + FastAPI）
start.bat

# 方式二：分别启动
# Spring Boot（IntelliJ 或命令行）
cd backend-java && mvn spring-boot:run -Dspring-boot.run.profiles=dev

# FastAPI AI 服务
cd backend-python && python -m uvicorn app.main:app --reload --host 0.0.0.0 --port 8000

# 前端
cd frontend && npm install && npm run dev
```

### 4. 访问

浏览器打开 `http://localhost:3000`

## 测试账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | admin123 | 管理员 |

## 开发状态

当前完成：第一 ~ 五阶段（项目初始化 + 基础业务 + 文档入库 + RAG 问答 + 反馈闭环）

- [x] 项目骨架搭建
- [x] 数据库建表 + 初始化数据
- [x] 用户登录 / JWT 认证
- [x] 知识库 CRUD + 成员权限管理
- [x] 文档上传与多格式解析（PDF/DOCX/TXT/MD）
- [x] 切片 + Embedding 向量化（BGE-M3 1024维）
- [x] Qdrant 向量存储与检索
- [x] RAG 问答链路（含流式输出）
- [x] 引用溯源（可展开查看原文）
- [x] 多轮会话历史
- [x] 答案点赞/点踩反馈
- [x] 失败问题自动记录
- [x] 数据统计面板
- [ ] Docker Compose 一键部署

## 知识库文档

- `docs/系统设计文档.md` — 架构设计、模块划分、核心流程
- `docs/数据库设计文档.md` — 表结构、字段定义、索引、Qdrant/Redis 设计
- `docs/项目开发计划文档.md` — 开发计划、阶段划分、技术选型
