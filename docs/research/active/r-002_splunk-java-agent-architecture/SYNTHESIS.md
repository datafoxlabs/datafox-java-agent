---
schema_version: "1.2"
metadata_schema: "1"
artifact_type: research-synthesis
id: R-002-SYNTHESIS
parent_id: R-002
title: "Splunk OpenTelemetry Java Agent 发行版架构研究结论"
status: draft
revision: "0"
created: 2026-10-23
updated: 2026-10-23
author: "ChatGPT (GitHub MCP)"
owner: "Unassigned"
payload_sha256:
---

# Splunk OpenTelemetry Java Agent 发行版架构研究结论

## Executive Conclusion

**Splunk 的主体实现是“上游 OpenTelemetry Java Agent + 官方 Distro 风格的三阶段重打包 + 产品特有模块”。** 它不 Fork SDK，也不依赖把主发行版功能作为外部 Extension JAR 加载。Splunk 在官方 Distro 基础上增加了薄启动入口、集中依赖管理、产品默认配置、专有 Instrumentation、OpAMP、Profiler 和成熟的测试发布工程。

对 Datafox 最值得借鉴的是 **Gradle 的类加载依赖边界、Shadow 组装、统一版本和按业务能力分模块**。**建议先读 [从 Gradle 工程到最终 Agent JAR](notes/gradle-and-packaging.md)**，沿着一个发行包的构建过程理解各阶段。需要核对完整 Gradle 配置、插件约定、依赖平台和 ServiceLoader 的实现细节，先读 [Shadow 约定插件逐条精读](notes/shadow-conventions-source-walkthrough.md)，再看 [build.gradle.kts 全工程源码精读](notes/build-gradle-deep-dive.md)。Synthesis 用于保存结论，不代替这些源码说明。不建议直接复制 Splunk 的所有模块、内部 Tooling SPI、远程管理、Profiling 或商业发布渠道。该结论来自 Splunk `v2.32.0` 与 OpenTelemetry `v2.32.0` 的源码；**尚未构建或运行 Datafox Agent**。

## Supported Findings

| 发现 | 证据强度 | 来源 |
|---|---|---|
| Splunk 使用发布的上游 Agent 和三种 Configuration，Shadow 分为 relocation、`inst/*.classdata` 隔离、最终组装 | 高：固定 Gradle 源码 | [Gradle 与打包](notes/gradle-and-packaging.md)、[Shadow 规则逐条精读](notes/shadow-conventions-source-walkthrough.md)、[构建脚本深入解读](notes/build-gradle-deep-dive.md) |
| `SplunkAgent.premain` 最终委托 `OpenTelemetryAgent.agentmain`，没有重新实现 SDK 初始化 | 高：Java 入口源码 | [Gradle 与打包](notes/gradle-and-packaging.md) |
| `:custom` 使用 SDK SPI 处理默认配置、Realm/Token、SpanLimits 和发行版资源属性 | 高：Java 源码 | [运行时定制](notes/runtime-customizations.md) |
| OpAMP 与 Profiler 独立模块，借助 AgentListener 生命周期按需启动 | 高：Java 源码 | [运行时定制](notes/runtime-customizations.md) |
| `:dependencyManagement` 集中 BOM/constraints；CI 分离构建、Muzzle、最新依赖和 Smoke Test；发布 Maven、签名及 JAR | 高：Gradle/Workflow/Shell | [CI 与发布](notes/ci-release.md) |

## Rejected Hypotheses

- “Splunk Fork 了 OpenTelemetry Agent 的核心初始化流程”：不成立。主发行包引入官方 Maven Agent，SplunkAgent 仅薄委托。
- “Splunk 的所有 Instrumentation 都必须在 `:agent` 里显式列出”：不成立。Agent 脚本通过约定插件标记动态加入相关子项目。
- “Splunk 所有定制均基于稳定公开 API”：不成立。部分实现依赖上游内部 Tooling、SDK 内部辅助类和 Alpha 依赖。
- “完整 Splunk 工程模块是所有发行版的标准要求”：没有依据。OpAMP、Profiler、No-code、应用服务器矩阵等源自 Splunk 自身功能范围。

## Remaining Unknowns

- RQ-006：Datafox 首批功能、最低 JVM、支持的框架、配置模式和发行渠道尚未确定，因此不能决定 `:bootstrap`、`:custom`、`:instrumentation`、`:opamp`、`:profiler` 的最终保留范围。
- 未进行构建、运行、性能或 ClassLoader 兼容性实测；按项目要求本阶段不创建独立验证原型。
- Splunk v2.32.0 的产品取舍不能直接推导为 Datafox 的性能、稳定性或兼容性保证。

## Options Comparison

| 方案 | 优点 | 主要代价 | 当前判断 |
|---|---|---|---|
| A. 复制完整 Splunk 工程 | 功能模块与发布流程现成 | 无业务依据的依赖、内部 API、测试矩阵和维护成本 | 不建议 |
| B. 复用上游 Distro 机制，参考 Splunk 工程实践 | 兼容上游、可按需加入功能、易于升级 | 需要自行确定最小模块与发行契约 | 优先研究 |
| C. Fork 上游 Agent/SDK | 可以深度修改内部行为 | 上游升级和 ClassLoader 风险高 | 当前不采用 |

## Recommendation and Preconditions

**项目工程优先级已明确：以 Splunk `v2.32.0` 的工程结构与 Gradle 打包为第一参考，以 OpenTelemetry 官方 Distro `v2.32.0` 为 ClassLoader、Agent 生命周期和 SPI 的技术契约依据。** 具体设计如下：

1. 保留 `upstreamAgent`、`bootstrapLibs`、`javaagentLibs` 的 ClassLoader 构建边界和官方 Shadow 三阶段流程。
2. 优先保留上游 `OpenTelemetryAgent` 入口；除非存在启动前逻辑需求，不引入 Splunk 式自定义 `premain`。
3. 按 Datafox 已确认的能力建立模块；不要预先复制 `opamp`、`profiler`、No-code、专有应用服务器 Instrumentation。
4. 统一 SDK/Instrumentation 版本；发行版版本与上游版本分开记录。只有确实需要时才建立复杂 `java-platform` 和 `buildSrc` 约定插件。
5. 发行版内部优先使用官方 SDK SPI、Instrumentation API，严格限制内部 Tooling/Alpha 依赖。
6. Datafox 已明确排除外部和内嵌 Extension JAR 发行架构；此限制不排除内部 SPI 的使用。

## Handoff to ADR and ExecPlan

用户已明确优先参考 Splunk 的工程结构与 Gradle 打包；对应的 [Datafox Gradle 工程设计草案](../../../design-docs/datafox-gradle-distribution.md) 记录规划。R-002 作为后续 Datafox Gradle 架构设计的事实依据，但**不构成 ADR 批准或代码实施授权**。后续工程设计应同时引用 [R-001 官方 Distro 研究](../../r-001_opentelemetry-javaagent-upstream/SYNTHESIS.md) 与本研究；决定模块时应逐项说明业务需求、依赖边界、可替代的上游能力和新增维护成本。

Research Owner 尚未分配；本 Synthesis 保持草稿，不执行 `conclude-research`。正式实施阶段再执行必要构建和运行验收，不在研究阶段另做原型。

## Revision Notes

- 2026-10-23 — 固定 Splunk v2.32.0 完成 Gradle、运行时、Instrumentation、测试与发布源码研究，形成 Datafox 取舍建议。
- 2026-10-23 — 根据项目要求明确 Splunk First 的工程结构与 Gradle 打包优先级，关联设计草案；不改变 Research 未批准状态。
- 2026-10-23 — 将 build.gradle.kts 简版摘要改写为完整源码精读，覆盖构建职责、依赖平台、Shadow 各阶段、Manifest、发布与 Datafox 取舍；不改变研究状态。
- 2026-10-23 — 将 Gradle 构建分析改为可循序阅读的源码导读，并通过资料索引提供“先理解，再深入”的阅读路径。
- 2026-10-23 — 增补 Shadow 约定插件专题，结合真实 `:agent` 三阶段 Task 解释每条重定位、ServiceLoader 合并与依赖排除规则。
