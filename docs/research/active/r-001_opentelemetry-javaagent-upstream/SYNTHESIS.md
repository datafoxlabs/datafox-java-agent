---
schema_version: "1.2"
metadata_schema: "1"
artifact_type: research-synthesis
id: R-001-SYNTHESIS
parent_id: R-001
title: "OpenTelemetry Java Agent 官方上游架构研究结论"
status: draft
revision: "0"
created: 2026-10-23
updated: 2026-10-23
author: "ChatGPT (GitHub MCP)"
owner: "Unassigned"
payload_sha256:
---

# OpenTelemetry Java Agent 官方上游架构研究结论

## Executive Conclusion

针对 OpenTelemetry Java Agent `v2.32.0`，官方提供“独立/内嵌 Extension”和“完整 Distro 重打包”两种不同范围的工程路线。现有证据支持**先以官方 Agent 加最小 Extension 验证发行物**，而不是预先复制全部 Distro Example 模块。该方案只适用于 SDK/SPI 与常规扩展能实现 Datafox 真实需求的情况。

**成熟度：草稿／证据建设中。** 尚未在 Datafox 仓库实际构建、启动或验证 `-javaagent`。如业务要求新增 Bootstrap 级工具类、Agent 内置库、ClassLoader 可见的 Instrumentation helper，或需要声明式配置等价行为，必须重新评估构建方案。

## Supported Findings

| 发现 | 可信程度 | 证据 |
|---|---|---|
| Agent 的 System / Bootstrap / Agent / Extension 分层及 `inst/*.classdata` 是上游封装契约 | high（官方源码文档） | [upstream-analysis.md](notes/upstream-analysis.md) |
| Extension 可外部加载或放入 Agent 的 `extensions/`，保留原 Manifest 即可采用官方内嵌方案 | high（官方示例及文档） | [upstream-analysis.md](notes/upstream-analysis.md) |
| 完整 Distro 通过重定位、合并 SPI 与类隔离将新的 agent libraries 装入发行物 | high（固定版本 Gradle 源码） | [upstream-analysis.md](notes/upstream-analysis.md) |
| Alpha 扩展接口不受 stable 兼容性承诺约束 | high（上游 VERSIONING.md） | [upstream-analysis.md](notes/upstream-analysis.md) |
| 标准 `AutoConfigurationCustomizerProvider` 不用于声明式 YAML 路径 | high（官方 Extension 文档） | [upstream-analysis.md](notes/upstream-analysis.md) |

## Rejected Hypotheses

- “自定义发行版必须复制所有 Distro Example Gradle 模块”：不成立。官方文档支持只在上游 Agent 中嵌入 Extension JAR。
- “Java 8 兼容字节码表示 Gradle 本身在 Java 8 下执行”：不成立。Gradle Wrapper/构建 JVM 与编译目标版本是两个独立条件。
- “所有配置 SPI 对声明式配置与传统 properties 行为相同”：不成立。官方对 `AutoConfigurationCustomizerProvider` 明确标记该限制。

## Remaining Unknowns

- RQ-005：未运行 Agent 启动、SPI 加载、OTLP 导出或 ClassLoader 边界测试。
- RQ-006：Datafox 的功能、受支持 JVM/框架、标准配置覆盖范围、默认协议和发布要求尚未正式确定。
- 尚未与 Splunk 或 Microsoft 的实际仓库结构进行对照；本 Research 有意只研究上游。

## Options Comparison

| 方案 | 适用条件 | 主要代价 |
|---|---|---|
| A. 上游 Agent 最小重包装 | 只需要品牌与发行流程 | 不能提供业务自定义逻辑 |
| B. 上游 Agent + `extensions/` | 通过上游 SPI 实现新增功能，要求单文件分发 | 依赖扩展 API；必须验证嵌入与版本兼容 |
| C. 完整官方 Distro 打包流程 | 需要 Bootstrap 类、AgentClassLoader 新库或复杂 Instrumentation | relocation、资源合并、Muzzle、升级维护面增加 |

## Recommendation and Preconditions

优先验证 B，且**只有明确 Extension 功能需求后才新增 custom 模块**；若功能均由上游配置实现，则优先选择 A。C 必须以已确认的 ClassLoader 或 Instrumentation 限制为前提。

下一阶段用固定 `v2.32.0`/SDK 1.66.0 的 GitHub Actions 原型验证：Gradle 构建可复现、`Premain-Class` 保留、SPI 自动发现、HTTP 请求产生 Span、OTLP 接收结果、显式 `otel.*` 配置不被默认配置覆盖。不得在缺少运行证据时宣布正式架构验收。

## Handoff to ADR and ExecPlan

该 Research 暂不交给 ADR 或工程实现。研发时以 [上游源码证据](notes/upstream-analysis.md) 为入口，后续必须补齐 Datafox 功能需求和实际 smoke-test 证据。Research Owner 未分配，不能封存 Synthesis 或认为架构已批准。

## Revision Notes

- 2026-10-23 — 首轮官方源码调查形成 draft，保留运行验证与功能范围两个未决问题。
