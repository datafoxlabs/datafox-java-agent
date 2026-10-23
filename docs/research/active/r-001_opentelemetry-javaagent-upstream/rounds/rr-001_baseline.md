---
schema_version: "1.1"
metadata_schema: "1"
artifact_type: research-round
id: RR-001
parent_id: R-001
title: "OpenTelemetry Java Agent 官方上游架构研究——RR-001 基线调查"
status: active
created: 2026-10-23
updated: 2026-10-23
author: "ChatGPT (GitHub MCP)"
owner: "Unassigned"
---

# OpenTelemetry Java Agent 官方上游架构研究——RR-001 基线调查

## Focus and Questions

固定官方发行版本 `v2.32.0`，确定 RQ-001~RQ-004 的 ClassLoader、构建、SPI 和配置事实；将 RQ-005（Distro 运行验证）延后至实施阶段，RQ-006（业务需求）仍待明确。项目已明确排除 Extension 打包方案。

## Scope

只使用 OpenTelemetry 官方源码、发布说明和官方产品文档。本轮不分析第三方发行版、不编写 Datafox Java/Gradle 代码，也不接受架构方案。

## Evidence Added

- [上游证据](../notes/upstream-analysis.md)：官方 Javaagent Structure、Distro/Extension Example、Gradle、版本兼容、测试与配置。
- [Distro 构建与测试分析](../notes/distro-build-and-test.md)：官方三种 Configuration、Shadow 三阶段打包、Bootstrap、Instrumentation/Muzzle、测试 Agent 和 Smoke Test。
- [固定版本 Distro 示例](https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/v2.32.0/examples/distro)
- [固定版本 Extension 示例](https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/v2.32.0/examples/extension)
- [官方 Agent 扩展文档](https://opentelemetry.io/docs/zero-code/java/agent/extensions/)

## Synthesis Delta

在项目排除 Extension JAR 打包路径后，进一步从官方固定版本源码确认 Distro 的完整打包依赖关系、ServiceLoader 合并、ClassLoader 隔离与测试机制。尚未实际执行构建或运行；用户已明确不需要独立原型验证，因此 RQ-005 转为 deferred。

## Next Inquiry

1. 根据 Datafox 的首批功能明确是否需要 Bootstrap、AgentClassLoader 类、专有 Instrumentation 或声明式配置适配。
2. 基于已研究的官方 Distro 源码确定最小 Gradle 工程设计；不再单独开展研究原型。
3. 将构建和运行测试留到正式实施阶段。

## Round Outcome

本轮已完成 Distro Gradle 与测试体系的源码取证；按用户要求不执行独立运行原型。研究保持 active，后续需明确功能范围。
