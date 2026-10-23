---
schema_version: "1.2"
metadata_schema: "1"
artifact_type: research
id: R-002
title: "Splunk OpenTelemetry Java Agent 发行版架构研究"
status: active
maturity: evidence_building
research_type: comparative
synthesis: SYNTHESIS.md
manifest: RESEARCH_MANIFEST.json
created: 2026-10-23
updated: 2026-10-23
owner: "Unassigned"
author: "ChatGPT (GitHub MCP)"
current_round: RR-001
synthesis_revision: "0"
approved_by: ""
approved_at: ""
approval_ref: ""
---

# Splunk OpenTelemetry Java Agent 发行版架构研究

本 Research 通过固定版本源码解释 Splunk Java Agent 发行版的工程架构和实现方式，作为 Datafox 未来 Gradle 工程设计的参考；它不自动批准 Datafox 的具体模块布局。

## Research Metadata

| Field | Value |
|---|---|
| Date | 2026-10-23 |
| Last Updated | 2026-10-23 |
| Research Type | Comparative |
| Research Owner | Unassigned |
| Author | ChatGPT (GitHub MCP) |
| Lifecycle | active |
| Maturity | evidence_building |
| Current Round | RR-001 |
| Synthesis Revision | v0 |
| Approval | Pending |

## Purpose and Decision to Enable

研究 Splunk 如何以 OpenTelemetry Java Agent 为上游构建独立 Distribution：Gradle 多模块、依赖版本管理、Agent/Bootstrap 打包、SDK 定制、Instrumentation、Profiler/OpAMP、CI 和发布。与 R-001 官方 Distro 基线比较，识别 Datafox 应复用的工程机制和不应照搬的产品特有复杂度。

## Current Snapshot

- 固定 Splunk `v2.32.0` 标签，确认其 SDK `1.66.0`、Instrumentation `2.32.0`、Gradle `9.8.1` 和 Shadow `9.6.1`。
- 已核对 `:agent`、`:bootstrap`、`:custom`、`:instrumentation:*`、`:opamp`、`:profiler`、`:dependencyManagement` 的实际代码和依赖。
- 已确认 Splunk 沿用上游 Distro 的 Shadow 三阶段打包，`SplunkAgent` 仅薄委托给上游 Agent。
- 详见 [Synthesis](SYNTHESIS.md) 和 [研究资料](notes/README.md)。
- 按项目要求，不创建独立原型，也不在 Research 阶段运行构建、Smoke Test 或性能测试。

## Research Rounds

| Round | Focus | Status | Author | Started | Evidence and outcome |
|---|---|---|---|---|---|
| RR-001 | Splunk v2.32.0 工程与运行机制源码研究 | active | ChatGPT (GitHub MCP) | 2026-10-23 | [基线研究](rounds/rr-001_baseline.md)；[Gradle/打包](notes/gradle-and-packaging.md)；[运行时](notes/runtime-customizations.md)；[发布](notes/ci-release.md) |

## Scope and Non-goals

范围：固定版本的 Gradle Kotlin DSL、三种打包 Configuration、Shadow relocation、启动委托、SDK SPI、Instrumentation、Muzzle、Profiler、OpAMP、发行包、CI、版本管理和 Datafox 适用性。

不研究 Splunk 的未发布分支；不实施 Datafox Gradle 工程；不运行验证原型；不复制 Splunk 专有配置、商用产品功能或内部 API；不批准 ADR。

## Research Questions

| ID | Status | Question | Answer or disposition | Evidence |
|---|---|---|---|---|
| RQ-001 | answered | Splunk 发行版由哪些 Gradle 模块构成，各自承担什么职责？ | Agent/Bootstrap/Custom/Instrumentation/OpAMP/Profiler/依赖平台/测试与发布分层 | [Gradle 与打包](notes/gradle-and-packaging.md) |
| RQ-002 | answered | Splunk 如何复用上游 Agent 并完成最终 JAR 打包？ | upstreamAgent + bootstrapLibs + javaagentLibs，Shadow 三阶段重定位、隔离、组装 | [Gradle 与打包](notes/gradle-and-packaging.md) |
| RQ-003 | answered | Splunk 是否重写了 Agent 启动与 SDK？ | SplunkAgent 只委托 OpenTelemetryAgent；定制通过官方 SPI 和少量内部 Tooling SPI | [运行时定制](notes/runtime-customizations.md) |
| RQ-004 | answered | 产品默认配置、Resource、Profiler、OpAMP、Instrumentation 如何接入？ | custom 中的 AutoConfiguration SPI；Profiler/OpAMP 的 AgentListener；独立 InstrumentationModule | [运行时定制](notes/runtime-customizations.md) |
| RQ-005 | answered | Splunk 如何管理依赖、测试、版本和发布？ | java-platform/BOM、Gradle conventions、Muzzle/Smoke Matrix、Maven/GPG/发布脚本 | [CI 与发布](notes/ci-release.md) |
| RQ-006 | deferred | Datafox 应落地哪些 Splunk 模块？ | 需根据 Datafox 首批真实功能和交付渠道确定；不能仅凭 Splunk 现有模块数量决策 | 后续工程设计与功能定义 |

状态枚举：`open`、`answered`、`deferred`、`invalidated`；字段保留英文以兼容 RepoFoundryAI。

## Method and Sources

优先阅读 [Splunk v2.32.0](https://github.com/signalfx/splunk-otel-java/tree/v2.32.0) 的实际 Gradle 文件、Java 入口和 SPI、GitHub Actions、发布脚本，并对照 [OpenTelemetry 官方 Distro v2.32.0](https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/v2.32.0/examples/distro)。研究笔记保留逐项源码链接，区分观察与对 Datafox 的建议。

## Experiments and Prototypes

本轮不进行实验或原型。用户已明确要求跳过独立验证，所有结论限于源码和构建配置。Datafox 正式实施阶段仍应执行必要的编译、打包和运行验收；本 Research 不声称这些检查已通过。

## Findings

1. Splunk 采用与官方 Distro 同源的 `bootstrapLibs` / `javaagentLibs` / `upstreamAgent` 和 Shadow 三阶段打包。
2. Splunk 的 `Premain-Class` 指向自有 `SplunkAgent`，但该类只委托上游 `OpenTelemetryAgent`，未重新实现 SDK。
3. `:custom` 通过 SDK AutoConfiguration、ResourceProvider 等 SPI 处理 Splunk 默认配置与产品元数据；部分能力使用内部 Tooling SPI。
4. `:opamp`、`:profiler` 和众多 Instrumentation 是具体产品能力，不能当作发行版的强制模块。
5. CI 与发布已包含 Muzzle、最新依赖、应用服务器 Smoke Test、Maven/GPG、GitHub Release 等，超出 Datafox 初始范围。

## Contradictions and Uncertainty

Splunk 既大量复用上游 SPI，也使用 `BeforeAgentListener`、`javaagent-tooling`、SDK 内部辅助类等非稳定边界。它的复杂度来自产品能力和兼容性范围，不证明 Datafox 必须采用同等规模。未运行性能和启动测试，不能断言某种模块组织在性能上更优。

## Decision Drivers and Options

评价标准：上游契约兼容性、ClassLoader 正确性、最小依赖和模块数量、维护与升级成本、功能需要、发行交付要求。

- A：复制 Splunk 的完整多模块布局和发布系统——当前没有业务依据，不推荐。
- B：参考 Splunk 的 Agent 打包、版本约束与模块职责，按 Datafox 需求精简——当前研究建议。
- C：Fork 上游 Agent/SDK 内部实现——缺乏必要性，不推荐。

不采用独立或嵌入式 Extension JAR 作为 Datafox 发行方案；发行版内部继续允许使用官方 SPI。

## Blockers

| ID | Status | Opened | Resolved | Missing capability | Impact | Unblock or resolution |
|---|---|---|---|---|---|---|

## Progress

- [x] (2026-10-23) 固定 Splunk v2.32.0，读取 Gradle 与启动入口。
- [x] (2026-10-23) 研究 SDK 定制、Instrumentation、Profiler、OpAMP 和发布流程。
- [x] (2026-10-23) 完成与官方 Distro 的架构对照及 Datafox 取舍建议。
- [ ] Datafox 功能边界明确后再决定模块与交付渠道。

## Outcome

研究保持 active，结论处于 draft / evidence_building。Research Owner 未分配，不能直接封存或将本建议视为正式架构批准。

## Artifacts and Notes

- [当前研究结论](SYNTHESIS.md)
- [Gradle 与打包](notes/gradle-and-packaging.md)
- [运行时定制](notes/runtime-customizations.md)
- [CI 与发布](notes/ci-release.md)
- [资料索引](notes/README.md)
- [研究轮次](rounds/rr-001_baseline.md)
- Manifest：`docs/research/active/r-002_splunk-java-agent-architecture/RESEARCH_MANIFEST.json`

## Revision Notes

- 2026-10-23 — 创建 R-002，完成 Splunk v2.32.0 发行版架构的固定源码研究；不开展独立验证。
