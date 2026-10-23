---
schema_version: "1.1"
metadata_schema: "1"
artifact_type: research-round
id: RR-001
parent_id: R-002
title: "Splunk Java Agent 发行版架构研究——RR-001 源码调查"
status: active
created: 2026-10-23
updated: 2026-10-23
author: "ChatGPT (GitHub MCP)"
owner: "Unassigned"
---

# Splunk Java Agent 发行版架构研究——RR-001 源码调查

## Focus and Questions

以 Splunk `v2.32.0` 为基线，回答 RQ-001～RQ-005：Gradle 模块、上游 Agent 复用、启动委托、产品定制、Instrumentation 和发布机制。RQ-006 由 Datafox 的真实需求决定，本轮延后。

## Scope

只阅读固定版本的 Splunk 源码和官方 Distro 基线；不分析未发布的 Splunk `main` 变化、不运行独立验证原型、不实施 Datafox 工程。

## Evidence Added

- [Gradle 模块、打包与启动入口](../notes/gradle-and-packaging.md)
- [build.gradle.kts 深入解读](../notes/build-gradle-deep-dive.md)
- [Shadow 约定插件逐条源码分析](../notes/shadow-conventions-source-walkthrough.md)
- [SDK 配置、Profiler、OpAMP、Instrumentation](../notes/runtime-customizations.md)
- [CI、兼容性与发布流程](../notes/ci-release.md)
- [Splunk v2.32.0 源码](https://github.com/signalfx/splunk-otel-java/tree/v2.32.0)
- [OpenTelemetry 官方 Distro v2.32.0](https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/v2.32.0/examples/distro)

## Synthesis Delta

相对 R-001 官方 Distro 研究，新增证据表明 Splunk 的主发行版沿用上游三阶段 Shadow 打包，但加入产品模块、薄启动委托、统一依赖管理、Muzzle/Smoke Test 和发布管线。本轮还进一步解释了 `splunk.shadow-conventions` 对所有 ShadowJar 的公共配置、两处 ServiceLoader 合并、relocation 例外及依赖排除的不同作用。推荐借鉴其工程边界，而非照搬全部功能和内部依赖。

## Next Inquiry

待 Datafox 首批发行版功能和交付渠道明确后，结合 R-001/R-002 设计最小 Gradle 工程。根据用户要求，研究阶段不开展额外原型验证。

## Round Outcome

本轮已完成固定版本源码取证并写入 Synthesis；Research 保持 active，结论尚未获得 Research Owner 的封存授权。
