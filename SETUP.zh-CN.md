# 部署清单（从零到可运行 CV-RAG）

这份清单对应项目落地时你需要执行的具体工作。

## 1. 运行环境准备

1. 安装 JDK 17。
2. 安装 Maven 3.9+。
3. 验证命令：
- `java -version`
- `mvn -version`

## 2. 配置 DashScope（Qwen + text-embedding-v3）

1. 在 DashScope 创建 API Key。
2. 配置环境变量：
- `DASHSCOPE_API_KEY=<your_key>`
3. 更新 `application.yml` 或环境变量：
- `rag.model.mock-enabled=false`
- `rag.model.base-url=https://dashscope.aliyuncs.com/compatible-mode/v1`
- `rag.model.chat-model=qwen-plus`
- `rag.model.embedding-model=text-embedding-v3`

## 3. 配置 Pinecone 向量数据库

1. 创建 Pinecone 索引。
2. 检索度量设置为 cosine。
3. 向量维度设置为你的 embedding 输出维度。
- 当前 starter 的 mock 向量维度：`1024`
- 真实 `text-embedding-v3`：按你实际选择的输出维度
4. 从 Pinecone 控制台获取 index host。
5. 配置环境变量：
- `PINECONE_API_KEY=<your_key>`
- `PINECONE_HOST=<index-host-without-https>`
- `PINECONE_INDEX=<index-name>`
6. 启用 Pinecone Provider：
- `rag.vector.provider=pinecone`

## 4. 配置短期记忆 Redis

1. 启动 Redis（初期单实例即可）。
2. 配置：
- `REDIS_HOST`
- `REDIS_PORT`
- `REDIS_PASSWORD`（如有）
- `REDIS_DB`
3. 开启 Redis 记忆模式：
- `rag.memory.redis-enabled=true`
4. 设置会话 TTL：
- `rag.memory.ttl-hours=72`（或按你业务需要）

## 5. 配置 CV 嵌入（CLIP 链路）

项目目前已具备图片入库 API 形态，但生产级图片向量需要接入真实 CLIP 服务。

你可以二选一：
1. 自建一个 Python 微服务，暴露：
- `POST /embed/text-for-image`
- `POST /embed/image`
2. 使用托管式多模态 embedding 服务，接口契约保持一致。

随后替换回退逻辑文件：
- [DashScopeEmbeddingGateway.java](./src/main/java/org/example/cvrag/llm/DashScopeEmbeddingGateway.java)

## 6. 基础设施建议

1. Pinecone 命名空间建议：
- `cv-rag-kb`：知识库分块
- `cv-rag-memory`：长期记忆
2. 推荐保留的 metadata 字段：
- `docId`, `modality`, `source`, `chunkIndex`, `userId`, `type`
3. 检索参数建议：
- 先用 `topK=6`、`hybridAlpha=0.7`
- 再基于离线评测集调参

## 7. 启动顺序

1. 先启动 Redis（如果启用）。
2. 确认环境变量已配置。
3. 启动应用：
- `mvn spring-boot:run`
4. 先做文档/图片入库。
5. 再进行问答请求。

## 8. 下一步运维与演进

1. 为图片入库增加 OCR（增强 `content` 字段）。
2. 在混合检索后增加 reranker。
3. 增加引用格式化与置信度阈值。
4. 构建评测集与质量指标：
- recall@k
- answer faithfulness
- latency p95
5. 增加认证与用户级配额/成本控制。
