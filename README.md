# 企业知识库智能助手

基于 RAG（检索增强生成）的企业内部知识库智能问答平台。支持文档上传、解析、向量化入库，以及基于知识库的智能问答与引用溯源。

## 功能特性

- **用户与权限管理** — 支持管理员和普通用户角色，JWT 认证
- **知识库管理** — 创建/编辑/删除知识库，成员访问控制
- **文档管理** — 上传 PDF / DOCX / MD / TXT 文档，自动解析与入库
- **智能问答** — 基于知识库检索 + DeepSeek 大模型生成答案，支持引用溯源
- **会话历史** — 多轮对话记录与历史查看
- **反馈闭环** — 答案点赞/点踩、失败问题自动记录、基础统计面板

## 系统架构

```
用户 → Vue 3 前端 → Spring Boot (REST API) ─→ MySQL / Redis
                 ↘→ FastAPI (AI 服务) ─→ Qdrant / SiliconFlow API
```

**文档入库链路**：上传 → 解析 → 切片 → Embedding → 写入 Qdrant + MySQL

**问答链路**：提问 → 向量化 → Qdrant 召回 → 拼接 Prompt → DeepSeek 生成 → 引用返回

## 技术栈

| 层 | 技术 | 说明 |
|---|------|------|
| 前端 | Vue 3 + Element Plus + Pinia + vue-router | Element Plus 组件库 |
| 业务后端 | Spring Boot 3.4 + MyBatis-Plus + JWT | ORM 用 MyBatis-Plus，认证用 JWT |
| AI 服务 | FastAPI + PyMuPDF + python-docx + qdrant-client | 独立的 AI 处理链路 |
| 数据库 | MySQL 8.x | 业务数据与结构化元数据 |
| 缓存 | Redis | 登录态、热点数据 |
| 向量库 | Qdrant | chunk 向量存储与相似度检索 |
| 模型平台 | SiliconFlow API | Embedding（BGE-M3）+ 生成（DeepSeek） |

## 项目结构

```
knowledge-base-assistant/
├── frontend/                  # Vue 3 前端项目
│   ├── src/
│   │   ├── api/               # 接口封装（axios）
│   │   ├── views/             # 页面（登录、仪表盘、知识库、问答...）
│   │   ├── router/            # 路由 + 守卫
│   │   ├── stores/            # Pinia 状态管理
│   │   └── components/        # 通用组件
│   └── ...
├── backend-java/              # Spring Boot 业务服务
│   ├── src/main/java/.../
│   │   ├── controller/        # REST 接口层
│   │   ├── service/           # 业务逻辑层
│   │   ├── mapper/            # MyBatis-Plus Mapper
│   │   ├── entity/            # 数据实体
│   │   ├── security/          # JWT 安全相关
│   │   └── client/            # FastAPI HTTP 客户端
│   └── ...
├── backend-python/            # FastAPI AI 服务
│   ├── app/
│   │   ├── routers/           # 接口路由
│   │   ├── services/          # 解析、切片、向量化、问答
│   │   └── config/            # 模型配置
│   └── ...
├── sql/                       # 建表脚本
├── docker/                    # Docker Compose 编排
├── docs/                      # 设计文档
└── data/files/                # 上传文件存储（共享卷）
```

## 快速开始

### 前置要求

- JDK 21+
- Python 3.10+
- Node.js 18+
- MySQL 8.x
- Redis
- Qdrant
- SiliconFlow API Key（[注册](https://siliconflow.cn)）

### 1. 初始化数据库

```bash
mysql -u root -p < sql/init_schema.sql
```

### 2. 启动后端服务

```bash
# Spring Boot 业务服务
cd backend-java
mvn spring-boot:run

# FastAPI AI 服务（另开终端）
cd backend-python
pip install -r requirements.txt
python -m app.main
```

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

### 4. 访问

浏览器打开 `http://localhost:5173`

## 测试账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | admin123 | 管理员 |

## 开发状态

当前完成：第一阶段（项目初始化）+ 第二阶段（基础业务模块）

- [x] 项目骨架搭建
- [x] 数据库建表 + 初始化数据
- [x] 用户登录 / JWT 认证
- [x] 知识库 CRUD
- [x] 成员权限管理
- [x] 文档上传与管理
- [x] AI 服务基础通信
- [ ] 文档解析与切片
- [ ] Embedding 向量化
- [ ] RAG 问答链路
- [ ] 反馈与统计面板
- [ ] Docker Compose 一键部署
