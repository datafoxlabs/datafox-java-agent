# Datafox Java Agent

基于 OpenTelemetry Java Agent 的定制发行版。**工程、Gradle、Shadow 和评测体系以 Splunk Java Agent v2.32.0 为 1:1 对齐基线**；运行时接口、Agent 生命周期和 ClassLoader 契约以 [OpenTelemetry Java Instrumentation v2.32.0](https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/v2.32.0) 为准。

## 工程结构

| 模块 | 职责 |
|---|---|
| `:agent` | 组装发行版 JAR、Maven 主制品、Sources/Javadoc 和可选签名 |
| `:bootstrap` | Splunk 风格的薄启动委托，最终调用上游 Agent |
| `:custom` | 发行版 SDK/SPI 定制；当前提供 Datafox Resource 元数据 |
| `:dependencyManagement` | 统一 SDK、Instrumentation、Agent、测试依赖版本 |
| `:instrumentation` | 专有 Instrumentation 的聚合与 Muzzle 发现入口 |
| `:smoke-tests` | 启动子 JVM，通过最终 Agent JAR 验证 OTLP HTTP 导出 |
| `:testing:common` | 共享测试依赖 |
| `:testing:agent-for-testing` | Instrumentation 测试使用的 Agent JAR |
| `buildSrc` | Java、Spotless、Shadow、Muzzle 和 Instrumentation 测试约定 |

工程保留 Splunk 的 `upstreamAgent`、`bootstrapLibs`、`javaagentLibs` 三种 Configuration，并使用 `relocateJavaagentLibs` → `isolateJavaagentLibs` → `shadowJar` 三阶段打包。**不采用独立或内嵌 Extension JAR 作为发行架构**，但允许发行版内部使用官方 SPI。

Splunk 的 OpAMP、Profiler、No-code、专有应用服务器 Instrumentation 等产品功能不在基础设施对齐范围；有 Datafox 需求时再单独引入。

## 构建

CI 使用 JDK 25 运行 Gradle；发行版 Java 主代码目标为 Java 8。Gradle Wrapper 固定 9.8.1，Shadow 固定 9.6.1。

```bash
./gradlew build
./gradlew :agent:shadowJar
./gradlew :smoke-tests:test
./gradlew :agent:generateLicenseReport
```

发行物包括直接分发的 `-all.jar` 与 Maven Publication 使用的无 classifier 主 JAR。开发版本当前为 `0.1.0-SNAPSHOT`，不代表稳定发布承诺。

## CI 与评测

GitHub Actions 包含 Gradle build / unit test、测试 Agent 打包、JAR 内容检查、Muzzle 发现与执行、最新依赖模式、最终 Agent Smoke Test 和第三方许可证报告。Muzzle 与 latest-dependency 测试在新增对应 Instrumentation 模块后才会有实际目标。Smoke Test 已配置 Linux/Windows × Java 8/11/17/21/25 的应用 JVM 矩阵，Gradle 由 Java 25 运行；[最新 CI 的全部 10 组组合](https://github.com/datafoxlabs/datafox-java-agent/actions/runs/40166248476) 均已通过。

## Release

发行包通过 [prepare-release.sh](scripts/prepare-release.sh) 汇总 Agent JAR、Sources/Javadoc、Maven POM 和许可证归档，并生成、核对 SHA-256 校验和。该流程已经过常规 CI 验证。[Release 工作流](.github/workflows/release.yml) 可以手动打包但不公开发布；只有推送与 `version.gradle.kts` 一致的非 Snapshot 版本标签，才会创建 **Draft GitHub Release**。Maven Central 远程发布与签名密钥尚未启用。

## 文档

- [架构说明](ARCHITECTURE.md)
- [Splunk 对齐清单](docs/design-docs/splunk-parity-checklist.md)
- [评测与 Release 流程](docs/design-docs/release-and-evaluation.md)
- [Datafox Gradle 工程设计](docs/design-docs/datafox-gradle-distribution.md)
- [R-001：OpenTelemetry 官方 Distro 研究](docs/research/active/r-001_opentelemetry-javaagent-upstream/SYNTHESIS.md)
- [R-002：Splunk Gradle 与 Shadow 研究](docs/research/active/r-002_splunk-java-agent-architecture/SYNTHESIS.md)

以上是工程实现与目标范围说明。构建、打包和应用级 Smoke Test 的通过状态以最新 [GitHub Actions 记录](https://github.com/datafoxlabs/datafox-java-agent/actions/workflows/agent-build.yml) 为准。
