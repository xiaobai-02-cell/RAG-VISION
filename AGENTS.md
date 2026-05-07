# AGENTS.md

This file defines practical working rules for human and AI contributors in this repository.

# Repository Guidelines

Communication Language: > You MUST use Simplified Chinese (简体中文) for all your internal thinking processes, terminal outputs, explanations, and direct communications with the user.

## 第一性原理

请使用第一性原理思考。你不能总是假设我非常清楚自己想要什么和该怎么得到。请保持审慎，从原始需求和问题出发，如果动机和目标不清晰，停下来和我讨论。

## 代码规范

当你需要编写任何TypeScript代码时，强制使用 `typescript-project-specifications skill`

## 方案规范

当需要你给出修改或重构方案时必须符合以下规范：

- 不允许给出兼容性或补丁性的方案
- 不允许过度设计，保持最短路径实现且不能违反第一条要求
- 不允许自行给出我提供的需求以外的方案，例如一些兜底和降级方案，这可能导致业务逻辑偏移问题
- 必须确保方案的逻辑正确，必须经过全链路的逻辑验证

## Project Overview

- Type: Maven Java application
- Coordinates: `org.example:RAG:1.0-SNAPSHOT`
- Packaging: `jar`
- Test framework: JUnit 3.8.1

## Repository Layout

- `pom.xml`: Maven project definition
- `src/main/java/org/example/App.java`: application entry point
- `src/test/java/org/example/AppTest.java`: baseline unit test

## Local Commands

- Run tests: `mvn test`
- Build artifact: `mvn clean package`
- Full verify (clean + tests): `mvn clean test`

## Contribution Rules

- Keep changes small and scoped to the task.
- Preserve package structure unless refactor is intentional.
- Add or update tests when behavior changes.
- Do not commit IDE/build artifacts (`.idea/`, `*.iml`, `target/`, `out/`).
- Avoid adding dependencies without a clear need.

## Agent Workflow

1. Read relevant files before editing.
2. Make the smallest correct implementation change.
3. Run `mvn test` when feasible before finalizing.
4. Summarize what changed and why in the final note or commit message.
