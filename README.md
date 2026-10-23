# Datafox Java Agent

基于 OpenTelemetry Java Instrumentation 的自定义 Java Agent 发行版。

## 工程方向

- **工程结构与 Gradle 打包优先参考 [Splunk Java Agent v2.32.0](https://github.com/signalfx/splunk-otel-java/tree/v2.32.0)**。
- Agent 生命周期、ClassLoader、Instrumentation、SDK 和 SPI 遵守 [OpenTelemetry Java Instrumentation](https://github.com/open-telemetry/opentelemetry-java-instrumentation) 的上游契约。
- 采用 Splunk 风格的 `upstreamAgent` / `bootstrapLibs` / `javaagentLibs` 分层与 Shadow 三阶段打包，不采用独立或内嵌 Extension JAR 发行路线。
- 优先复用标准 `otel.*` 配置；仅在确有发行版专有配置时使用 `datafox.*`。
- 模块按真实功能创建，不预先复制 Splunk 的 OpAMP、Profiler、No-code 或应用服务器 Instrumentation。

研究和设计资料：

- [R-001：OpenTelemetry 官方 Distro 研究](docs/research/active/r-001_opentelemetry-javaagent-upstream/SYNTHESIS.md)
- [R-002：Splunk 发行版架构研究](docs/research/active/r-002_splunk-java-agent-architecture/SYNTHESIS.md)
- [Gradle 发行版工程设计草案](docs/design-docs/datafox-gradle-distribution.md)
- [架构说明](ARCHITECTURE.md)

## 云端开发方式

使用 GitHub MCP 修改远程仓库，通过 GitHub Actions 执行需要 Shell 环境的操作，不要求本地安装 Java、Gradle 或 RepoFoundryAI。

工程 Harness 使用 [RepoFoundryAI](https://github.com/XiaoWeiKIN/RepoFoundryAI) `0.15.1` 初始化；[Bootstrap 工作流](.github/workflows/bootstrap-repofoundry.yml) 支持手动执行。

## 当前状态

**仍处于架构设计阶段。** 尚未创建 Gradle Wrapper、Java 源码、打包任务或可运行的 Datafox Agent JAR。研究阶段按要求不执行独立原型验证；正式实施时再进行必要的构建和运行检查。
