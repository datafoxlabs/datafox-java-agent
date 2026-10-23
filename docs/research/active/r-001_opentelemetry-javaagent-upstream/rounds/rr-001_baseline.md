---
schema_version: "1.1"
metadata_schema: "1"
artifact_type: research-round
id: RR-001
parent_id: R-001
title: "OpenTelemetry Java Agent upstream architecture — RR-001 Baseline investigation"
status: active
created: 2026-10-23
updated: 2026-10-23
author: "ChatGPT (GitHub MCP)"
owner: "Unassigned"
---

# OpenTelemetry Java Agent upstream architecture — RR-001 Baseline investigation

## Focus and Questions

固定官方发行版本 `v2.32.0`，确定 RQ-001~RQ-004 的 ClassLoader、构建、SPI 和配置事实；保留 RQ-005（运行验证）和 RQ-006（业务需求）为下一轮输入。

## Scope

只使用 OpenTelemetry 官方源码、发布说明和官方产品文档。本轮不分析第三方发行版、不编写 Datafox Java/Gradle 代码，也不接受架构方案。

## Evidence Added

- [Upstream evidence](../notes/upstream-analysis.md)：官方 Javaagent Structure、Distro/Extension Example、Gradle、版本兼容、测试与配置。
- [Versioned Distro Example](https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/v2.32.0/examples/distro)
- [Versioned Extension Example](https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/v2.32.0/examples/extension)
- [Official Agent Extensions](https://opentelemetry.io/docs/zero-code/java/agent/extensions/)

## Synthesis Delta

首次形成证据摘要：建议将普通 Extension 路径作为优先验证项，完整 Distro 打包仅作为满足额外 Agent/Bootstrap 需求时的方案；研发结论尚未获准实施。

## Next Inquiry

1. 在 GitHub Actions 构建固定版本的最小扩展并产出嵌入式 Java Agent。
2. 使用 `-javaagent` 启动一个最小 Java 应用，验证 SPI 注册、Span 产生与 OTLP 输出。
3. 根据 Datafox 明确功能选择是否需要 Declarative 配置适配、自定义 Instrumentation 或 Bootstrap class。

## Round Outcome

本轮已完成文档取证；研究保持 active，后续需运行级别证据和业务范围。
