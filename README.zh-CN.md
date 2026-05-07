# CV-RAG（Java + Spring Boot）

本项目是一个**计算机视觉 RAG 系统**的起步实现,目的是利用起来实验室堆积的资料做一个智能问答系统：

- Java + Spring Boot
- LangChain4j（当前先保留依赖，后续可进一步接入）
- 大模型：`Qwen-plus`（兼容 DashScope API）
- 文本向量模型：`text-embedding-v3`
- 向量数据库：`Pinecone`（提供 `in-memory` 回退）
- 短期记忆：`Redis`（提供 `in-memory` 回退）
- 文档解析：`Apache Tika`
- 检索：**稠密向量 + BM25 类稀疏融合**（混合打分）

## 当前架构

1. 入库链路
- `/api/ingest/file`：使用 Tika 解析文件，分块、向量化并写入向量库
- `/api/ingest/text`：直接写入文本
- `/api/ingest/image`：写入图片（caption + 图片向量）
- 基于 `docId` 的替换和删除（`DELETE /api/ingest/doc/{docId}`）

2. 问答链路
- 加载短期记忆（会话级）
- 召回长期记忆提示（用户级）
- 查询改写
- 从知识库进行混合检索（文本 + 图片模态）
- 组装 Prompt（短记忆 + 长记忆 + 检索片段 + 用户问题）
- 生成最终回答

3. 记忆链路
- 短期记忆：滑动窗口 + 摘要压缩
- 长期记忆：主动写入（`/api/memory/remember` 或用户消息以 `记住`/`remember` 开头）

## 包结构

- `org.example.cvrag.api`：REST 控制器 + DTO
- `org.example.cvrag.ingest`：Tika 解析、分块、入库
- `org.example.cvrag.retrieval`：混合检索服务
- `org.example.cvrag.memory`：短期/长期记忆模块
- `org.example.cvrag.llm`：Mock 与 DashScope 网关
- `org.example.cvrag.vector`：向量存储抽象（`in-memory`、`pinecone`）
- `org.example.cvrag.service`：RAG 编排与 Prompt 组装

## 快速开始（本地 Mock 模式）

默认 `application.yml` 使用：
- `rag.model.mock-enabled=true`
- `rag.vector.provider=in-memory`
- `rag.memory.redis-enabled=false`

因此无需外部服务即可先跑通端到端流程。

## Web 控制台

启动服务后，直接访问：

- `http://localhost:8080/`

页面包含三个工作区：
- 文档服务：上传文档/图片/文本并标注元数据（文档类型、所属项目、标签）
- 问答服务：可视化查看问答结果、改写问句、命中 chunkId
- 长期记忆：用户主动录入长期偏好信息

## API 示例

1. 文本入库

```bash
curl -X POST http://localhost:8080/api/ingest/text \
  -H "Content-Type: application/json" \
  -d "{\"docId\":\"doc-java-rag\",\"sourceName\":\"manual\",\"content\":\"RAG combines retrieval and generation.\"}"
```

2. 文件入库

```bash
curl -X POST http://localhost:8080/api/ingest/file \
  -F "file=@./sample.pdf" \
  -F "docId=doc-sample-pdf"
```

3. 图片入库

```bash
curl -X POST http://localhost:8080/api/ingest/image \
  -F "file=@./diagram.png" \
  -F "docId=img-arch-1" \
  -F "caption=系统架构图，包含RAG检索和Redis记忆"
```

4. 对话问答

```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d "{\"userId\":\"u001\",\"sessionId\":\"s001\",\"question\":\"这个系统里短期记忆怎么做？\",\"topK\":6}"
```

5. 写入长期记忆

```bash
curl -X POST http://localhost:8080/api/memory/remember \
  -H "Content-Type: application/json" \
  -d "{\"userId\":\"u001\",\"content\":\"我更偏好Pinecone而不是自建Milvus\"}"
```

## 外部服务配置

生产化配置清单见 [SETUP.zh-CN.md](./SETUP.zh-CN.md)：
- DashScope API Key 与模型参数
- Pinecone 索引与命名空间
- Redis 部署与持久化
- CV 嵌入服务（CLIP）接入建议
