# Datafox Java Agent

基于 OpenTelemetry Java Agent 的定制发行版。**工程、Gradle、Shadow 和评测体系以 Splunk Java Agent v2.32.0 为 1:1 对齐基线**；运行时接口、Agent 生命周期和 ClassLoader 契约以 [OpenTelemetry Java Instrumentation v2.32.0](https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/v2.32.0) 为准。

## 工程结构

| 模块 | 职责 |
|---|---|
| `:agent` | 组装发行版 JAR、Maven 主制品、Sources/Javadoc 和可选签名 |
| `:bootstrap` | Splunk 风格的薄启动委托，最终调用上游 Agent |
| `:custom` | 发行版 SDK/SPI 定制：Resource、Kafka Span Exporter 与 Agent Logger |
| `:dependencyManagement` | 统一 SDK、Instrumentation、Agent、测试依赖版本 |
| `:instrumentation` | 专有 Instrumentation 的聚合与 Muzzle 发现入口 |
| `:smoke-tests` | 启动子 JVM，通过最终 Agent JAR 验证 OTLP HTTP 导出 |
| `:testing:common` | 共享测试依赖 |
| `:testing:agent-for-testing` | Instrumentation 测试使用的 Agent JAR |
| `buildSrc` | Java、Spotless、Shadow、Muzzle 和 Instrumentation 测试约定 |

工程保留 Splunk 的 `upstreamAgent`、`bootstrapLibs`、`javaagentLibs` 三种 Configuration，并使用 `relocateJavaagentLibs` → `isolateJavaagentLibs` → `shadowJar` 三阶段打包。**不采用独立或内嵌 Extension JAR 作为发行架构**，但允许发行版内部使用官方 SPI。

Splunk 的 OpAMP、Profiler、No-code、专有应用服务器 Instrumentation 等产品功能不在基础设施对齐范围；有 Datafox 需求时再单独引入。

## 声明式 YAML 配置

基于 OpenTelemetry Java Agent 2.32.0 和 SDK 1.66.0 的实验性声明式配置，通过 `-Dotel.config.file=/etc/datafox/otel-config.yaml` 启用。声明式 SDK 初始化与传统 `otel.*` properties 自动配置是不同路径；不能将全部 JVM properties 视为 YAML 的自动覆盖项。Agent 自身文件日志仍使用 `datafox.javaagent.logging.file` 早期配置。

OTLP HTTP 与 Datafox Kafka Exporter 的示例配置：

```yaml
file_format: "1.1"
resource:
  attributes:
    - name: service.name
      value: order-service
tracer_provider:
  processors:
    - batch:
        exporter:
          otlp_http:
            endpoint: http://localhost:4318/v1/traces
```

Kafka 使用 Datafox 的声明式 `ComponentProvider`（并非 OpenTelemetry 内置 Exporter）：

```yaml
file_format: "1.1"
tracer_provider:
  processors:
    - batch:
        exporter:
          kafka:
            bootstrap_servers: localhost:9092
            topic: otel-traces
            producer:
              acks: all
              linger.ms: 5
```

需要同时满足 `bootstrap_servers` 和 `topic`，缺失时报配置错误；`bootstrap_servers` 优先于 `producer.bootstrap.servers`。自定义 Resource 元数据由发行版声明式 Resource Detector 注入。YAML 模式与传统模式的 SDK 扩展接口分别注册，不能自动互换。

具体实现及验证边界见 [声明式配置兼容设计](docs/design-docs/declarative-configuration.md)。

## Agent 自身诊断日志文件

发行版可以把 **Agent 内部诊断日志** 输出到独立文件，不重定向整个 JVM 的 `System.err`，也不改变应用的 Logback / Log4j2 或 OTLP Logs SDK。默认不启用，沿用 OpenTelemetry 的 `simple`（stderr）、`application`、`none`。

```bash
java \
  -javaagent:datafox-javaagent.jar \
  -Ddatafox.javaagent.logging.file=/var/log/datafox/agent.log \
  -jar application.jar
```

还支持环境变量 `DATAFOX_JAVAAGENT_LOGGING_FILE`、Agent 参数 `-javaagent:agent.jar=datafox.javaagent.logging.file=/path/agent.log`，以及 `otel.javaagent.configuration-file` 指定的 Properties 文件。上游 `AgentInitializer` 先解析 Agent 参数，再由发行版 `LoggingCustomizer` 读取配置；取值顺序为 Agent 参数覆盖后的 JVM 系统属性、环境变量、配置文件。

实现上在 `:custom` 中采用 Splunk 风格的 `LoggingCustomizer`，委托上游已重定位的 SLF4J SimpleLogger；`:bootstrap` 保持薄启动委托。具体行为：

- 仅在 `otel.javaagent.logging` 未设置或为 `simple` 时生效；显式选择 `application` / `none` 时保持上游行为，忽略文件路径。
- 若已直接设置上游 `io.opentelemetry.javaagent.slf4j.simpleLogger.logFile` 系统属性，则该上游属性优先，发行版不覆盖它。
- `otel.javaagent.debug=true` 继续控制 Agent DEBUG 日志。
- 文件父目录必须事先存在。SimpleLogger 打开文件失败时会报告错误并回退 stderr；空白路径由发行版报告错误并保持默认输出。
- **SimpleLogger 每次 JVM 启动时截断文件**，不追加；不提供文件轮转。外部轮转需考虑进程持有的文件描述符，`copytruncate` 存在丢日志窗口。
- 上游 Agent 初始化日志后通过内部 Logger 打印的内容写入文件。Bootstrap 早期、JVM 本身以及上游直接使用 `System.err` 的输出不保证进入这个文件。

这是 Agent 自身诊断日志功能，与 Logback / Log4j2 应用日志注入和 `otel.logs.exporter` 无关。

## Kafka Trace Exporter

Datafox Java Agent 通过官方 `ConfigurableSpanExporterProvider` 提供 `kafka` 选项，直接复用 [DataFox Java Contrib Kafka Exporter](https://github.com/datafoxlabs/datafox-java-contrib/tree/main/kafka-exporter)。例如：

```properties
otel.traces.exporter=kafka
otel.metrics.exporter=none
otel.logs.exporter=none
datafox.exporter.kafka.bootstrap-servers=localhost:9092
datafox.exporter.kafka.topic=otel-traces
```

可选 `datafox.exporter.kafka.producer`：按 SDK map 配置语法传递 Kafka Producer 配置，例如 `acks=all,linger.ms=5`；明确声明的 `datafox.exporter.kafka.bootstrap-servers` 优先于 Map 中的 `bootstrap.servers`。Kafka Record 使用无 Key、OTLP Protobuf 二进制 Value，推荐保持默认 BatchSpanProcessor；`kafka` 启用时如果缺失 broker/topic，Agent SDK 初始化将报错。Java 8+ 运行时使用 Kafka Clients 3.9.2。

Contrib 尚未发布 Maven 制品，所以本仓库使用固定提交的 Git submodule + Gradle composite build，仅对 Kafka Exporter 声明依赖替换。构建前必须拉取子模块：

```shell
git clone --recurse-submodules https://github.com/datafoxlabs/datafox-java-agent.git
# 已经克隆的仓库：
git submodule update --init --recursive
```

修改 Contrib 版本时，应显式更新子模块 Git SHA 并运行全部 Agent 测试。Contrib 升级不会由 Gradle 自动追踪 `main`。

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

GitHub Actions 包含 Gradle build / unit test、测试 Agent 打包、JAR 内容检查、Muzzle 发现与执行、最新依赖模式、最终 Agent Smoke Test 和第三方许可证报告。Muzzle 与 latest-dependency 测试在新增对应 Instrumentation 模块后才会有实际目标。Smoke Test 已配置 Linux/Windows × Java 8/11/17/21/25 的应用 JVM 矩阵，Gradle 由 Java 25 运行；[最新 CI 的全部 10 组组合](https://github.com/datafoxlabs/datafox-java-agent/actions/runs/40179130519) 均已通过。

## Gradle 与 Instrumentation 测试

普通 CI 已覆盖发行包、测试 Agent、JAR 结构、Maven 本地发布和一次性 GPG 测试签名。另有 `:testing:instrumentation-harness` 用于验证测试 JVM 真实注入了 `-javaagent`；它不进入产品发行包。

构建诊断对齐 Splunk 的 Develocity 4.6.0，但**默认不上传 Build Scan**。只有仓库管理员显式设置 GitHub Actions 变量 `DATAFOX_BUILD_SCAN_CONSENT=yes` 后才会允许向第三方服务上传构建信息。

## Release

发行包通过 [prepare-release.sh](scripts/prepare-release.sh) 汇总 Agent JAR、Sources/Javadoc、Maven POM 和许可证归档，并生成、核对 SHA-256 校验和。该流程已经过常规 CI 验证。[Release 工作流](.github/workflows/release.yml) 可以手动打包但不公开发布；只有推送与 `version.gradle.kts` 一致的非 Snapshot 版本标签，才会创建 **Draft GitHub Release**。标签发布需要配置 GPG 私钥、口令及密钥 ID；Maven Central 仅在版本标签上手动指定 `publish_maven=true` 且具备 Sonatype 凭据时才执行远程发布。正式生产签名和远程发布均尚未实际执行。

## 文档

- [架构说明](ARCHITECTURE.md)
- [Splunk 对齐清单](docs/design-docs/splunk-parity-checklist.md)
- [评测与 Release 流程](docs/design-docs/release-and-evaluation.md)
- [Datafox Gradle 工程设计](docs/design-docs/datafox-gradle-distribution.md)
- [R-001：OpenTelemetry 官方 Distro 研究](docs/research/active/r-001_opentelemetry-javaagent-upstream/SYNTHESIS.md)
- [R-002：Splunk Gradle 与 Shadow 研究](docs/research/active/r-002_splunk-java-agent-architecture/SYNTHESIS.md)

以上是工程实现与目标范围说明。构建、打包和应用级 Smoke Test 的通过状态以最新 [GitHub Actions 记录](https://github.com/datafoxlabs/datafox-java-agent/actions/workflows/agent-build.yml) 为准。
