# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

企业知识库智能助手是一个面向企业内部场景的 RAG 知识库问答平台。支持文档上传 → 解析 → 切片 → 向量化 → 智能问答的完整链路，提供引用溯源、会话历史、反馈闭环等功能。

项目定位：个人全栈项目，可用于实习面试展示。

## 技术栈

| 层 | 技术 |
|---|------|
| 前端 | Vue 3.5 + Element Plus 2.9 + Pinia 2.3 + Vite 6 |
| 业务后端 | Spring Boot 3.4.1 + MyBatis-Plus 3.5.7 + JWT (jjwt 0.12), Java 21 |
| AI 服务 | FastAPI 0.115 + PyMuPDF + python-docx + qdrant-client + httpx |
| 数据库 | MySQL 8.x |
| 缓存 | Redis 7 |
| 向量库 | Qdrant |
| AI 平台 | SiliconFlow API — BGE-M3 (1024维) + DeepSeek-V4-Flash |

## 启动命令

```bash
# 一键启动（前端 + FastAPI，不含 Spring Boot）
start.bat

# 停止
stop.bat

# 三服务分别启动
cd frontend && npm run dev                                     # 前端 :3000
cd backend-python && python -m uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
cd backend-java && mvn spring-boot:run -Dspring-boot.run.profiles=dev   # :8080

# 依赖安装
cd frontend && npm install
cd backend-python && pip install -r requirements.txt
```

所有服务启动后访问 `http://localhost:3000`，测试账号 admin / admin123。

## 架构与关键链路

```
:3000 Vue 3 (dev)          :8080 Spring Boot              :8000 FastAPI
  ├─ /api/* ──proxy──→  controller ──→ service ──→ MySQL/Redis
  │                                  └─→ AiServiceClient ──→ /internal/*
  └─ /internal/* ─proxy──→  (直通 FastAPI, 仅开发时)
```

**文档入库链路**：上传 → 解析（PyMuPDF/python-docx）→ 切片（500+50）→ BGE-M3 Embedding → Qdrant + MySQL
**问答链路**：提问 → 查询改写 → 混合检索（向量+关键词+RRF）→ Rerank → Prompt 拼接 → DeepSeek 生成 → 引用标注
**服务间文件传输**：共享卷 + 文件路径（Spring Boot 写文件，传路径给 FastAPI 读取）

## 项目关键路径

### 后端 Java (backend-java/src/main/java/com/example/kbassistant/)

| 层 | 关键文件 | 说明 |
|----|---------|------|
| 统一响应 | `common/Result.java`, `PageResult.java` | `Result.code/message/data` 格式，前端根据 `code !== 200` 判断成功 |
| 全局异常 | `common/GlobalExceptionHandler.java` | `BusinessException` 返回 HTTP 200 + code=500，前端 axios 拦截器走 success 分支检查 code |
| 安全 | `security/JwtAuthenticationFilter.java` | `OncePerRequestFilter`，从 `Authorization: Bearer <token>` 提取 |
| 安全 | `security/JwtTokenProvider.java` | jjwt 0.12，密钥 Base64 解码后 `hmacShaKeyFor()` |
| 安全 | `security/JwtUserDetails.java` | 含 `isAdmin()` 方法，替代 `"ADMIN".equals(...)` 散落判断 |
| 常量 | `constants/UserRoles.java`, `constants/MessageRoles.java` | 角色/消息角色字符串常量 |
| 枚举 | `enums/DocumentStatus.java`, `enums/FailureType.java` | 文档状态/失败类型枚举 |
| 客户端 | `client/AiServiceClient.java` | `HttpURLConnection` 调 FastAPI，已抽取 `openPostConnection`/`postJsonForMap`/`postJsonForStream` 三个私有方法 |
| 应用服务 | `service/ChatAskService.java` | 聊天公用逻辑：`prepareContext`、`saveAiMessage`、`recordFailedQuestion` |
| 异步处理 | `service/DocumentProcessingService.java` | `@Async("documentTaskExecutor")` 异步文档处理（ThreadPoolTaskExecutor 2/4） |
| 配置 | `config/SecurityConfig.java` | 放行 `/api/auth/**`, `/internal/**`，其余需认证。CSRF disable，CORS 单独配置 |
| 配置 | `config/CorsConfig.java` | `addAllowedOriginPattern("*")` + `allowCredentials(true)` |
| 配置 | `config/RedisConfig.java` | `RedisTemplate<String, Object>`，Jackson 序列化，Long→String 防 JS 精度丢失 |
| 配置 | `config/MyMetaObjectHandler.java` | `createdAt`/`updatedAt` 自动填充 |
| 配置 | `config/AsyncConfig.java` | `@EnableAsync` + ThreadPoolTaskExecutor 配置 |
| 实体 | `entity/*.java` | `@Data` + `@TableName` + `@TableId`，软删字段 `is_deleted` + `@TableLogic`，时间字段 `@TableField(fill = FieldFill.INSERT_UPDATE)` |

### Python (backend-python/app/)

| 文件 | 职责 |
|------|------|
| `services/document_processor.py` | 文档处理编排：解析→切片→向量化→写 Qdrant→写 MySQL。注意：用 `pymysql` 直连无连接池 |
| `services/retriever.py` | 混合检索：Qdrant 向量检索 + MySQL FULLTEXT 关键词检索 + RRF 融合 |
| `services/reranker.py` | 调 SiliconFlow Rerank API 二次排序，失败时回退原始排序 |
| `services/qa_generator.py` | Prompt 工程：查询改写、Prompt 拼接、流式/非流式生成 |
| `services/qa_service.py` | 问答编排：`prepare_answer` 统一同步/流式准备流程，`ask` 调用生成 |
| `services/chunker.py` | 固定长度切片 500 字 + 50 overlap，字符级 |
| `services/embedder.py` | BGE-M3 Embedding，底层复用 SiliconFlowClient |
| `services/siliconflow_client.py` | SiliconFlow API 统一客户端：embed/embed_batch/chat/chat_stream/rerank |
| `services/citation.py` | 引用组装 |
| `services/parser/base.py` | 解析器抽象基类 + `ParseResult` |
| `services/parser/pdf_parser.py` | PyMuPDF 提取纯文本 |
| `services/parser/docx_parser.py` | python-docx 提取文本 |
| `routers/chat.py` | 同步/流式问答路由，均通过 `QAService` 实例调用，无独立 `QAGenerator` |

### 前端 (frontend/src/)

| 路径 | 说明 |
|------|------|
| `utils/request.js` | axios 实例，baseURL `/api`，自动注入 token，401 跳转 `/login` |
| `utils/markdown.js` | `renderMarkdown`/`formatTime`/`buildSessionTitle` 工具函数 |
| `utils/streamRequest.js` | SSE fetch 封装，自动 token 注入 + 401 处理 |
| `composables/useChatStream.js` | 流式问答状态管理 + `handleSend` |
| `composables/useChatSessions.js` | 会话 CRUD + 知识库选择 + 反馈弹窗状态 |
| `composables/useCitationNavigation.js` | 引用弹窗 + `window.__goToCitation` 生命周期管理 |
| `components/chat/ChatSessionSidebar.vue` | 会话侧边栏（列表 + KB 选择） |
| `components/chat/ChatMessageList.vue` | 消息列表 + 自动滚动 + 引用导航事件 |
| `components/chat/ChatMessageBubble.vue` | 单条消息气泡（思考过程 + 反馈 + 引用） |
| `components/chat/CitationDialog.vue` | 引用详情弹窗 |
| `components/chat/FeedbackDialog.vue` | 反馈原因弹窗 |
| `stores/auth.js` | 登录/登出/用户信息，`getUserInfo()` 在 `AppLayout.vue` 的 `onMounted` 中调用 |
| `router/index.js` | 路由守卫检测 token，**无角色守卫**（菜单用 `v-if="authStore.isAdmin"` 但直接访问 URL 不会拦截） |
| `views/chat/ChatPage.vue` | 主聊天页（~184行），编排上述 composable 和组件 |

### 配置与部署

| 文件 | 说明 |
|------|------|
| `backend-java/src/main/resources/application-dev.yml` | 本地开发（localhost 直连 MySQL/Redis/FastAPI） |
| `backend-java/src/main/resources/application-prod.yml` | Docker 部署（容器内服务名连接） |
| `backend-python/.env` | SiliconFlow API Key、模型选择、Qdrant/MySQL 连接 |
| `frontend/vite.config.js` | 代理 `/api` → `:8080`，`/internal` → `:8000` |
| `docker/docker-compose.yml` | 6 服务编排，共享卷 `shared-files` 传文件 |

### 数据库核心表

- `sys_user` — 用户（password_hash BCrypt）
- `knowledge_base` — 知识库（scope: PUBLIC/PRIVATE，is_deleted 逻辑删除）
- `knowledge_base_member` — 成员关系（唯一索引 kb_id + user_id，role: ADMIN/MEMBER）
- `document` — 文档（parse_status: UPLOADED→PARSING→CHUNKING→EMBEDDING→READY/FAILED）
- `document_chunk` — 切片（vector_id 关联 Qdrant point，content 有 FULLTEXT 索引）
- `chat_session` / `chat_message` — 会话消息（citation_json TEXT 存引用）
- `answer_feedback` — 反馈（唯一索引 message_id + user_id）
- `failed_question` — 失败问题

## 开发模式与代码约定

### 新增页面完整链路

```
views/XxxPage.vue → router/index.js 添加路由 → api/xxx.js 添加接口
→ Java: controller/XxxController → service/XxxService → mapper/XxxMapper → entity/Xxx
```

- 响应体用 `Result<T>` 包装，分页用 `PageResult<T>`（records/total/pageNum/pageSize）
- 前端用 `ElMessage` / `ElMessageBox` 反馈操作结果
- DTO 用 `@NotBlank` / `@NotNull` 做参数校验，请求用 `@Valid`
- MyBatis-Plus Mapper 继承 `BaseMapper<T>` 获得基础 CRUD

### 新增前端组件规范

- 页面级功能拆分优先：composable 管理状态，component 负责渲染
- 通用 composable 放在 `composables/`，页面级组件放在 `components/xxx/`
- 纯工具函数放在 `utils/`，不依赖 Vue API

### 代码常量规范

- 角色字符串、消息角色用 `constants/` 常量类
- 文档状态、失败类型用 `enums/` 枚举，通过 `.name()` 转字符串
- 管理员判断用 `userDetails.isAdmin()`，不用 `"ADMIN".equals(...)`

### 文档状态机

文档 parse_status 流转通过 `document_processor.py` 控制：
```
UPLOADED → PARSING → CHUNKING → EMBEDDING → READY
                                            → FAILED（任一阶段抛异常）
```
上传后由 `DocumentProcessingService.processAsync()` 异步触发（`@Async` + ThreadPoolTaskExecutor）。

### RAG 问答数据流

1. 前端 `POST /api/chat/ask` 或 `/chat/ask-stream`
2. Spring Boot `ChatController` → `ChatAskService.prepareContext`（会话创建 + 历史 + 用户消息保存）
3. `AiServiceClient` 调 FastAPI `/internal/chat/ask` 或 `/ask-stream`
4. FastAPI `qa_service.py`：`prepare_answer`（查询改写 → 向量化 → 检索 → Rerank → Prompt）→ 生成
5. 答案返回给 Spring Boot，`ChatAskService.saveAiMessage` 保存 `chat_message`（含 `citation_json`）

### V1 设计边界

- PDF 解析只保证纯文本型文档，不处理表格/多栏/扫描件
- 切片 500 字 + 50 overlap 固定长度
- 文档处理异步（`@Async` 线程池 2/4），不引入消息队列
- Embedding 维度 1024（BGE-M3）
- 密码明文传输（仅 HTTPS 保护）
- 前端流式问答用 fetch + 手动 SSE 解析（不依赖 axios）

## 常见问题

- **Spring Boot 起不来**：检查 MySQL/Redis 是否运行，`application-dev.yml` 中数据库密码是否正确
- **问答返回空**：检查 `.env` 中 `SILICONFLOW_API_KEY` 是否有效，Qdrant 是否运行在 `localhost:6333`
- **文档处理失败**：看 FastAPI 日志（`POST /internal/document/process` 的响应），通常是文件路径问题或 SiliconFlow API 超时
- **前端代理不生效**：`vite.config.js` 确认 proxy 配置，需重启 `npm run dev`
- **CORS 报错**：`CorsConfig.java` 配置了宽松 CORS，Docker 部署时 Nginx 做反向代理不存在此问题
- **api/chat 的 SQL limit 拼接**：`messageMapper.findRecentBySessionId` 中使用了 `.last("LIMIT " + limit)`，SQL 拼接风格，参数来自内部固定值
