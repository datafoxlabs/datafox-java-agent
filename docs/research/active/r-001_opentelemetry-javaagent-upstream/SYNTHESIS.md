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

针对 OpenTelemetry Java Agent `v2.32.0`，官方提供“独立/内嵌 Extension”和“完整 Distro 重打包”两种不同范围的工程路线。项目已明确排除独立或内嵌 Extension 作为发行版打包路线。本轮已从固定版本源码确认官方完整 Distro 的三类依赖 Configuration、Shadow 三阶段打包、Bootstrap 可见性、Instrumentation/Muzzle 和 Smoke Test 机制。项目已明确跳过 Research 阶段的独立运行原型，直接以源码结论作为后续工程设计输入。当前尚不能确定 Datafox 所需的最小模块集合。

**成熟度：草稿／证据建设中。** 尚未在 Datafox 仓库实际构建、启动或验证 `-javaagent`。是否需要新增 Bootstrap 类、Agent 内置库、Instrumentation Helper 或声明式配置适配，仍须由 Datafox 的实际功能决定。

## Supported Findings

| 发现 | 可信程度 | 证据 |
|---|---|---|
| Agent 的 System / Bootstrap / Agent / Extension 分层及 `inst/*.classdata` 是上游封装契约 | high（官方源码文档） | [upstream-analysis.md](notes/upstream-analysis.md) |
| Extension 可外部加载或放入 Agent 的 `extensions/`，保留原 Manifest 即可采用官方内嵌方案 | high（官方示例及文档） | [upstream-analysis.md](notes/upstream-analysis.md) |
| 完整 Distro 通过 `relocateJavaagentLibs`、`isolateJavaagentLibs`、`shadowJar` 三阶段将新增 Agent 库隔离并装入发行物 | high（固定版本 Gradle 源码） | [distro-build-and-test.md](notes/distro-build-and-test.md) |
| Instrumentation 的 Muzzle 包含构建期生成和运行期兼容性匹配；发行包 Smoke Test 验证实际 OTLP 数据 | high（官方源码及测试） | [distro-build-and-test.md](notes/distro-build-and-test.md) |
| Alpha 扩展接口不受 stable 兼容性承诺约束 | high（上游 VERSIONING.md） | [upstream-analysis.md](notes/upstream-analysis.md) |
| 标准 `AutoConfigurationCustomizerProvider` 不用于声明式 YAML 路径 | high（官方 Extension 文档） | [upstream-analysis.md](notes/upstream-analysis.md) |

## Rejected Hypotheses

- “自定义发行版必须复制所有 Distro Example Gradle 模块”：不成立。官方文档支持只在上游 Agent 中嵌入 Extension JAR。
- “Java 8 兼容字节码表示 Gradle 本身在 Java 8 下执行”：不成立。Gradle Wrapper/构建 JVM 与编译目标版本是两个独立条件。
- “所有配置 SPI 对声明式配置与传统 properties 行为相同”：不成立。官方对 `AutoConfigurationCustomizerProvider` 明确标记该限制。

## Remaining Unknowns

- RQ-005（deferred）：按项目要求不开展独立 Distro 原型；Agent 启动、SPI 加载、OTLP 导出及 ClassLoader 边界仍未实测，移至后续正式实施阶段。
- RQ-006：Datafox 的功能、受支持 JVM/框架、标准配置覆盖范围、默认协议和发布要求尚未正式确定。
- 尚未与 Splunk 或 Microsoft 的实际仓库结构进行对照；本 Research 有意只研究上游。

## Options Comparison

| 方案 | 适用条件 | 主要代价 |
|---|---|---|
| A. 上游 Agent 最小重包装 | 只需要品牌与发行流程 | 不能提供业务自定义逻辑 |
| B. 上游 Agent + `extensions/` | 通过上游 SPI 实现新增功能，要求单文件分发 | 依赖扩展 API；必须验证嵌入与版本兼容 |
| C. 完整官方 Distro 打包流程 | 需要 Bootstrap 类、AgentClassLoader 新库或复杂 Instrumentation | relocation、资源合并、Muzzle、升级维护面增加 |

## Recommendation and Preconditions

后续仅研究 C（官方 Distro 构建路径）的最小充分实现；A 作为上游对照，B 已被项目要求排除。构建需区分 `bootstrapLibs`、`javaagentLibs`、`upstreamAgent`，保留上游 `premain` 入口，并对新增 Agent 库执行官方要求的 relocation、`inst/*.classdata` 隔离和 SPI 合并。按实际功能决定模块，保留官方 SPI 的使用，不照搬示例 Servlet 响应头修改。

本阶段不建立独立验证原型。后续正式实现时再执行必要的 Gradle 构建与运行验收，包括 `Premain-Class`、ClassLoader、SPI 和 OTLP 行为；当前结论仅依据上游固定版本源码，不声称 Datafox 已通过这些检查。

## Handoff to ADR and ExecPlan

该 Research 可以作为后续 Gradle 工程设计的源码依据，但不等于已批准 ADR。研发时以 [上游源码证据](notes/upstream-analysis.md) 和 [Distro 构建分析](notes/distro-build-and-test.md) 为入口；正式实施时再完成必要的构建与运行验收。Research Owner 未分配，不能封存 Synthesis 或认为架构已批准。

## Revision Notes

- 2026-10-23 — 首轮官方源码调查形成 draft，保留运行验证与功能范围两个未决问题。
- 2026-10-23 — 深入核对 Gradle 约定、Shadow 三阶段打包、Muzzle 和 Smoke Test；不改变 Research 的未批准状态。
- 2026-10-23 — 根据用户要求取消独立原型验证，将 RQ-005 延后到正式实现阶段。
