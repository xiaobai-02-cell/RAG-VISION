# CV-RAG (Java, Spring Boot)

This project is a starter implementation of a **computer vision RAG system** aligned with the stack from `Al-项目.pdf`:

- Java + Spring Boot
- LangChain4j (dependency reserved for follow-up integration)
- LLM: `Qwen-plus` (DashScope compatible API)
- Text embedding: `text-embedding-v3`
- Vector DB: `Pinecone` (with `in-memory` fallback)
- Short-term memory: `Redis` (with `in-memory` fallback)
- Document parsing: `Apache Tika`
- Retrieval: **dense vector + BM25-like sparse fusion** (hybrid scoring)

## Current Architecture

1. Ingestion pipeline
- `/api/ingest/file`: parse file with Tika, chunk text, embed, upsert vectors
- `/api/ingest/text`: ingest direct text
- `/api/ingest/image`: ingest image with caption + image embedding
- `docId`-based replacement and deletion (`DELETE /api/ingest/doc/{docId}`)

2. Query pipeline
- Load short-term memory (session scoped)
- Retrieve long-term memory hints (user scoped)
- Query rewriting
- Hybrid retrieval from KB (text + image modality)
- Prompt assembly (short memory + long memory + retrieval chunks + user question)
- Final answer generation

3. Memory pipeline
- Short memory: sliding window + summary compression
- Long memory: active write (`/api/memory/remember` or user message starting with `记住`/`remember`)

## Package Layout

- `org.example.cvrag.api`: REST controllers + DTO
- `org.example.cvrag.ingest`: Tika parse + chunk + ingestion
- `org.example.cvrag.retrieval`: hybrid retrieval service
- `org.example.cvrag.memory`: short/long memory modules
- `org.example.cvrag.llm`: mock and DashScope gateways
- `org.example.cvrag.vector`: vector store abstraction (`in-memory`, `pinecone`)
- `org.example.cvrag.service`: RAG orchestration + prompt assembly

## Quick Start (Local Mock Mode)

By default, `application.yml` uses:
- `rag.model.mock-enabled=true`
- `rag.vector.provider=in-memory`
- `rag.memory.redis-enabled=false`

So you can run without external services and test end-to-end flow.

## Web Console

After startup, open:

- `http://localhost:8080/`

The page includes:
- Document service (upload + metadata labels)
- QA service (chat + rewritten query + cited chunks)
- Long-memory input (user-driven memory entries)

## API Examples

1. Ingest text

```bash
curl -X POST http://localhost:8080/api/ingest/text \
  -H "Content-Type: application/json" \
  -d "{\"docId\":\"doc-java-rag\",\"sourceName\":\"manual\",\"content\":\"RAG combines retrieval and generation.\"}"
```

2. Ingest file

```bash
curl -X POST http://localhost:8080/api/ingest/file \
  -F "file=@./sample.pdf" \
  -F "docId=doc-sample-pdf"
```

3. Ingest image

```bash
curl -X POST http://localhost:8080/api/ingest/image \
  -F "file=@./diagram.png" \
  -F "docId=img-arch-1" \
  -F "caption=系统架构图，包含RAG检索和Redis记忆"
```

4. Chat

```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d "{\"userId\":\"u001\",\"sessionId\":\"s001\",\"question\":\"这个系统里短期记忆怎么做？\",\"topK\":6}"
```

5. Remember long-term user fact

```bash
curl -X POST http://localhost:8080/api/memory/remember \
  -H "Content-Type: application/json" \
  -d "{\"userId\":\"u001\",\"content\":\"我更偏好Pinecone而不是自建Milvus\"}"
```

## External Setup

See [SETUP.md](./SETUP.md) for production-like setup checklist:
- DashScope API key and model settings
- Pinecone index and namespaces
- Redis deployment and persistence
- CV embedding service (CLIP) integration recommendations
