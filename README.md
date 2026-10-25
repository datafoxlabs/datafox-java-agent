# Datafox Java Agent

基于 OpenTelemetry Java Agent 的定制发行版。工程组织和 Gradle 打包优先参考 [Splunk Java Agent v2.32.0](https://github.com/signalfx/splunk-otel-java/tree/v2.32.0)，运行时接口及 ClassLoader 契约以 [OpenTelemetry Java Instrumentation v2.32.0](https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/v2.32.0) 为准。

## 工程结构

| 模块 | 职责 |
|---|---|
| `:agent` | 组装最终 Java Agent JAR，包括 relocation、`inst/*.classdata` 隔离和 SPI 合并 |
| `:custom` | 发行版专有能力；当前只通过 `ResourceProvider` 标识 `telemetry.distro.name=datafox` |
| `:dependencyManagement` | 统一 OpenTelemetry SDK、Instrumentation、Agent 版本 |
| `buildSrc` | 集中管理 Shadow 和 ClassLoader 相关的重定位规则 |

工程定义保留 `bootstrapLibs`、`javaagentLibs`、`upstreamAgent` 三种依赖配置。当前**没有单独的 Bootstrap 类库**，所以没有创建空的 `:bootstrap` 模块；确有相关功能时再添加。`instrumentation`、Profiler、OpAMP 也按需增加，不预置演示功能。

## 构建

构建 JVM 使用 Java 21，Gradle Wrapper 为 9.8.1，Shadow 为 9.6.1，代码采用 Java 8 兼容字节码（`options.release=8`）。

```bash
./gradlew :agent:shadowJar
```

预期发行包位于：

```text
agent/build/libs/datafox-javaagent-0.1.0-SNAPSHOT.jar
```

其中 `0.1.0-SNAPSHOT` 是当前开发占位版本，不代表稳定发行承诺。构建过程仍使用上游 Agent 的 `OpenTelemetryAgent` 作为 `Premain-Class`，不复制 Splunk 的品牌启动器。

Gradle 采用 Splunk 的三阶段打包：

```text
javaagentLibs → relocateJavaagentLibs → isolateJavaagentLibs ┐
bootstrapLibs ────────────────────────────────────────────────┼→ shadowJar
upstreamAgent ─────────────────────────────────────────────────┘
```

自定义类直接按 AgentClassLoader 的 `inst/` 布局进入最终发行包，**不使用独立或内嵌 Extension JAR 作为发行架构**。发行版仍通过官方 SPI 扩展 SDK 行为。

## 配置与兼容

优先使用上游标准 `otel.*` 配置；新增 Datafox 专有参数时使用 `datafox.*`。当前没有自定义配置参数、远程管理或 Profiler。

## 云端开发

使用 GitHub MCP 修改代码，GitHub Actions 运行 Gradle；不依赖本地开发环境。

详细资料：

- [ARCHITECTURE.md](ARCHITECTURE.md)
- [Datafox Gradle 设计](docs/design-docs/datafox-gradle-distribution.md)
- [R-001：官方 Distro 研究](docs/research/active/r-001_opentelemetry-javaagent-upstream/SYNTHESIS.md)
- [R-002：Splunk Gradle 和 Shadow 研究](docs/research/active/r-002_splunk-java-agent-architecture/SYNTHESIS.md)

以上是工程实施说明，不代表已完成应用级 Smoke Test；构建和 JAR 检查结果以 GitHub Actions 的实际执行记录为准。
