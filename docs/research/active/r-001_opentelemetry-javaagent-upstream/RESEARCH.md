---
schema_version: "1.2"
metadata_schema: "1"
artifact_type: research
id: R-001
title: "OpenTelemetry Java Agent 官方上游架构研究"
status: active
maturity: evidence_building
research_type: architecture
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

# OpenTelemetry Java Agent 官方上游架构研究

本 Research 为 Datafox Java Agent 的工程架构选择建立上游证据。当前为调查阶段；不能把建议直接视为已接受的 ADR。

## Research Metadata

| 字段 | 内容 |
|---|---|
| 创建日期 | 2026-10-23 |
| 更新日期 | 2026-10-23 |
| 研究类型 | 架构研究 |
| 研究负责人 | 未分配 |
| 编写者 | ChatGPT (GitHub MCP) |
| 生命周期 | 进行中 |
| 成熟度 | 证据建设中 |
| 当前轮次 | RR-001 |
| 结论修订版 | v0 |
| 批准状态 | 待批准 |

## Purpose and Decision to Enable

研究 OpenTelemetry Java Instrumentation `v2.32.0` 的启动路径、ClassLoader、扩展 SPI、Gradle Distro 打包与测试约定，为未来决定 Datafox 是嵌入常规 Extension，还是采用完整 Distro 重打包管线提供可复查依据。

## Current Snapshot

- 已核查固定标签 `v2.32.0` 官方源码和官方文档；对应 SDK `1.66.0`，与正在开发的 `main` 分开。
- 已确认标准 Extension、嵌入式 Extension 和完整 Distro Example 的设计差异。
- 当前结论见 [SYNTHESIS.md](SYNTHESIS.md)，具体引用见 [upstream-analysis.md](notes/upstream-analysis.md)。
- 下一项实验：用 GitHub Actions 固定上游 Agent 和 Gradle 版本，测试最小嵌入式 Extension 的启动、SPI 发现和 OTLP 导出。
- 未决：Datafox 特有功能是否需要 Bootstrap / AgentClassLoader 新增类，以及是否要兼容声明式 YAML 自定义配置。

## Research Rounds

| Round | Focus | Status | 编写者 | Started | Evidence and outcome |
|---|---|---|---|---|---|
| RR-001 | 上游源码与两种打包方式 | 进行中 | ChatGPT (GitHub MCP) | 2026-10-23 | [Baseline](rounds/rr-001_baseline.md), [Evidence](notes/upstream-analysis.md) |

## Scope and Non-goals

范围：官方发行版概念、Agent 启动、ClassLoader、安全扩展点、Gradle 打包依赖、配置覆盖优先级、测试方式和上游升级风险。

本轮不分析 Splunk/Microsoft，不实现 Java 工程，不选择 Datafox 长期模块布局，不作性能、兼容性或安全承诺，不批准 ADR。

## Research Questions

| 编号 | 状态 | 问题 | 结论或处理意见 | 证据 |
|---|---|---|---|---|
| RQ-001 | 已回答 | Agent 的 ClassLoader 与封装边界是什么？ | System/Bootstrap/Agent/Extension 分层；`inst/*.classdata` 与独立 Extension 加载器 | [Evidence](notes/upstream-analysis.md) |
| RQ-002 | 已回答 | 标准 Extension 与完整 Distro 打包的范围差异？ | 标准 Extension 可独立或嵌入；完整 Distro 增加 relocations 与 Agent/Bootstrap 打包 | [Evidence](notes/upstream-analysis.md) |
| RQ-003 | 已回答 | 2.32.0 的 Gradle / SDK / SPI 版本与构建约定？ | SDK 1.66.0、Gradle 9.8.0、Java `release=8`；Alpha SPI 无兼容承诺 | [Evidence](notes/upstream-analysis.md) |
| RQ-004 | 已回答 | 标准配置优先级及声明式配置的差异？ | 属性 supplier 优先级最低；`AutoConfigurationCustomizerProvider` 不适用于声明式 YAML | [Evidence](notes/upstream-analysis.md) |
| RQ-005 | 待研究 | 一个最小嵌入式 Extension 是否通过实际 Agent 启动与遥测验证？ | 尚未构建及运行原型 | 下一轮 GitHub Actions 原型 |
| RQ-006 | 待研究 | Datafox 已确定功能是否需要 Bootstrap/Agent 类或声明式配置？ | 项目尚无足够功能需求及验收契约 | 需进入功能设计前确认 |

状态枚举：`open`（待研究）、`answered`（已回答）、`deferred`（延后）、`invalidated`（问题前提失效）。

## Method and Sources

以发行标签 `v2.32.0` 的 Java Instrumentation 源码、README、Gradle Kotlin DSL、`VERSIONING.md` 和 OpenTelemetry 官方产品文档为主。记录固定路径和功能契约，区分官方实现事实与对 Datafox 的设计建议；不将 `main` SNAPSHOT 当作已发布行为。

证据地图：[notes/upstream-analysis.md](notes/upstream-analysis.md)。

## Experiments and Prototypes

当前轮仅源码取证，未执行编译、`-javaagent`、Muzzle 或 OTLP 收集验证。下一轮原型应验证发行物的 JAR Manifest、扩展 SPI 发现、实际遥测输出、用户配置优先级以及 JVM 类加载无冲突；不以构建成功代替运行验证。

## Findings

1. 上游 Agent 负责初始化、SDK、Instrumentation 和隔离；Datafox 无需 fork 核心启动器。
2. 仅通过 Extension SPI 增加自定义能力时，可以使用 `extensions/` 内嵌 JAR 形成单文件发行包。
3. 完整 Distro 重打包适用于额外 Agent/Bootstrap 类库的隔离需求，包含 relocation 与 `.classdata` 处理。
4. Alpha 扩展点与声明式配置分支需要版本专属测试。详见 [上游证据](notes/upstream-analysis.md)。

## Contradictions and Uncertainty

官方 Distro Example 是全面能力展示，不意味着每个发行版都必须拥有其全部子模块。官方 SDK AutoConfiguration SPI 对声明式 YAML 并不等价。对当前仓库未执行 Agent 原型，打包兼容结论限于上游源码与文档，不构成 Datafox 实测。

## Decision Drivers and Options

以 API 合规、JVM ClassLoader 边界、最小新增代码、升级成本、运行验证和所需特有能力作为比较标准：

- A：仅发行上游 Agent，增加元数据与发行脚本。
- B：在上游 Agent 的 `extensions/` 嵌入独立 Extension。
- C：按官方 Distro Example 构建并隔离自定义 Bootstrap、Agent 库与 Instrumentation。

初步优先测试 B 的可行性；若没有真正的扩展需求，A 的实现成本更低。只有 B 不满足已确认需求才升级到 C。本轮不批准选择。

## Blockers

| 编号 | 状态 | 创建日期 | 解决日期 | 缺失能力 | 影响 | 解决方式 |
|---|---|---|---|---|---|---|

## Progress

- [x] (2026-10-23) 以官方 v2.32.0 为基准完成第一轮源码/文档取证。
- [ ] 在后续 Round 验证嵌入式 Extension 的真实执行路径。
- [ ] 确认 Datafox 首批特有能力和兼容范围，再进入架构决策。

## Outcome

研究保持进行中，研究结论仍为草稿。没有 Research Owner 的明确授权，不进行 `conclude-research`、ADR 批准或项目架构落地。

## Artifacts and Notes

- 证据清单： `docs/research/active/r-001_opentelemetry-javaagent-upstream/RESEARCH_MANIFEST.json`
- 研究结论： [SYNTHESIS.md](SYNTHESIS.md)
- 官方源码证据： [notes/upstream-analysis.md](notes/upstream-analysis.md)
- 当前轮次： [rounds/rr-001_baseline.md](rounds/rr-001_baseline.md)

## Revision Notes

- 2026-10-23 — 用固定上游源码与官方文档完成第一次证据分析，记录开放问题。
