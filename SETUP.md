# Setup Checklist (From Zero to Runnable CV-RAG)

This checklist is the concrete "what you need to do" part for this project.

## 1. Runtime prerequisites

1. Install JDK 17.
2. Install Maven 3.9+.
3. Verify:
- `java -version`
- `mvn -version`

## 2. Configure DashScope (Qwen + text-embedding-v3)

1. Create API key in DashScope.
2. Export env var:
- `DASHSCOPE_API_KEY=<your_key>`
3. Update `application.yml` or env:
- `rag.model.mock-enabled=false`
- `rag.model.base-url=https://dashscope.aliyuncs.com/compatible-mode/v1`
- `rag.model.chat-model=qwen-plus`
- `rag.model.embedding-model=text-embedding-v3`

## 3. Configure Pinecone vector database

1. Create Pinecone index.
2. Set metric to cosine.
3. Set dimension equal to your embedding output dimension.
- For this starter mock vectors: `1024`
- For real `text-embedding-v3`: use the model output dimension you choose.
4. Get index host from Pinecone console.
5. Export env vars:
- `PINECONE_API_KEY=<your_key>`
- `PINECONE_HOST=<index-host-without-https>`
- `PINECONE_INDEX=<index-name>`
6. Enable provider:
- `rag.vector.provider=pinecone`

## 4. Configure Redis for short-term memory

1. Start Redis (single instance is enough for initial stage).
2. Configure:
- `REDIS_HOST`
- `REDIS_PORT`
- `REDIS_PASSWORD` (if any)
- `REDIS_DB`
3. Enable Redis memory mode:
- `rag.memory.redis-enabled=true`
4. Set session TTL window:
- `rag.memory.ttl-hours=72` (or your desired retention)

## 5. Configure CV embedding (CLIP path)

This project already supports image ingestion API shape, but production-grade image embeddings must be connected to a real CLIP service.

You need to do one of:
1. Build a small Python microservice exposing:
- `POST /embed/text-for-image`
- `POST /embed/image`
2. Or use a managed multimodal embedding endpoint with the same contract.

Then replace fallback logic in:
- [DashScopeEmbeddingGateway.java](./src/main/java/org/example/cvrag/llm/DashScopeEmbeddingGateway.java)

## 6. Recommended infra decisions

1. Namespace strategy in Pinecone:
- `cv-rag-kb` for knowledge chunks
- `cv-rag-memory` for long-term memories
2. Metadata fields to keep:
- `docId`, `modality`, `source`, `chunkIndex`, `userId`, `type`
3. Retrieval tuning:
- start with `topK=6`, `hybridAlpha=0.7`
- tune by offline eval set

## 7. Run sequence

1. Start Redis (if enabled).
2. Ensure env vars are set.
3. Start app:
- `mvn spring-boot:run`
4. Ingest docs/images first.
5. Run chat queries.

## 8. Operational tasks you should plan next

1. Add OCR stage for image ingestion (to enrich `content` field).
2. Add reranker model after hybrid retrieval.
3. Add source citation formatter and confidence threshold.
4. Add evaluation dataset and quality metrics:
- recall@k
- answer faithfulness
- latency p95
5. Add authentication and per-user quota/cost guard.
