# Datafox Java Agent 声明式配置兼容设计

状态：实施中（`feat/declarative-config`）；运行时结论以本分支 GitHub Actions 为准。
版本基线：OpenTelemetry Java Instrumentation / Splunk v2.32.0、OpenTelemetry Java SDK v1.66.0。

## 解决的问题

原有 Datafox Kafka Exporter 通过 `ConfigurableSpanExporterProvider` 注册，发行版 Resource 通过 `ResourceProvider` 注册。这两个 SPI 属于传统自动配置路径。在指定 `otel.config.file` 后，上游 SDK 先进入声明式配置路径，不会再调用传统 AutoConfiguration 的 SDK Provider 链；因此原有自定义组件不能自动通过 YAML 使用。

## 上游与 Splunk 机制

- [SDK AutoConfiguredOpenTelemetrySdkBuilder v1.66.0](https://github.com/open-telemetry/opentelemetry-java/blob/v1.66.0/sdk-extensions/autoconfigure/src/main/java/io/opentelemetry/sdk/autoconfigure/AutoConfiguredOpenTelemetrySdkBuilder.java)：首先判断声明式文件，命中后直接返回配置结果。
- [Agent OpenTelemetryInstaller v2.32.0](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/v2.32.0/javaagent-tooling/src/main/java/io/opentelemetry/javaagent/tooling/OpenTelemetryInstaller.java)：复用上游 SDK 初始化和 ClassLoader。
- [Splunk Resource Detector](https://github.com/signalfx/splunk-otel-java/blob/v2.32.0/custom/src/main/java/com/splunk/opentelemetry/resource/SplunkDistroVersionResourceDetector.java)：通过 `ComponentProvider` 声明式加载 Resource。
- [Splunk Profiler 配置](https://github.com/signalfx/splunk-otel-java/blob/v2.32.0/profiler/src/main/java/com/splunk/opentelemetry/profiler/ProfilerConfigurationInitializer.java)：为传统 / YAML 提供不同的 Customizer。
- [官方资源 Customizer](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/v2.32.0/javaagent-tooling/src/main/java/io/opentelemetry/javaagent/tooling/resources/ResourceCustomizerProvider.java)：通过 Model Customizer 将必需 Detector 加入模型。

`ComponentProvider` 在 SDK 1.66.0 属于 `spi.internal`，官方明确标为不稳定 API。此处只通过小范围适配使用，并要求随上游升级验证编译、SPI 打包与应用级 Smoke Test。

## Datafox 实现

1. 传统 `KafkaSpanExporterProvider` 保持原有配置键；声明式 `KafkaSpanExporterComponentProvider` 使用 `bootstrap_servers`、`topic` 和可选 `producer`，两者复用 Kafka Exporter 构建方法。
2. `DatafoxDistroResourceComponentProvider` 与传统 ResourceProvider 复用同一份 `telemetry.distro.*` 元数据，避免两种模式版本漂移。
3. `DatafoxDistroResourceCustomizerProvider` 追加 Detector 到 YAML 模型，保留上游已有 Detector，并避免重复注入。直接在声明式 SDK Resource 管道构建，不额外建 SDK。
4. 独立 Agent 日志仍通过 `EarlyInitAgentConfig` 与现有 LoggingCustomizer 初始化，不把启动期设置混进 SDK YAML。
5. 所有新 Agent 类随 `:custom` 经过 Shadow relocation / isolation；由 AutoService 生成对应 `META-INF/services` 项。

## 行为与限制

- 使用 YAML 时，`otel.traces.exporter` 等传统 SDK properties 不自动覆盖 YAML Exporter。需要在 YAML 中明确指定 exporter、资源、处理器。
- 当 `bootstrap_servers` 或 `topic` 为空时，Kafka Provider 应在创建 Producer 前失败。
- 此版本不支持 YAML 文件动态热更新，不引入 OpAMP 或 Profiler。
- 不增加新的 Instrumentation；`jdbc` 声明式禁用需要独立集成用例才算完成行为验证。
- `distribution.javaagent` 与 Resource 检测机制完全由上游负责；产品适配只使用上游 SPI。
- Kafka `producer` 映射内容透传给 Kafka Producer，非法属性由 Kafka Clients 校验；不额外实现 Kafka 配置模型。

## 验证矩阵

| 验证 | 方法 | 当前状态 |
|---|---|---|
| Java 编译 / JUnit / Spotless | `./gradlew build` | 待本分支 CI |
| 最终 Agent YAML OTLP Traces | `DeclarativeConfigAgentSmokeTest`，子 JVM + MockWebServer | 待本分支 CI |
| Datafox Resource | YAML 导出中的 `telemetry.distro.name/version` | 待本分支 CI |
| YAML 与传统属性隔离 | 同时传入冲突的传统 exporter 属性，确认按 YAML 导出 | 待本分支 CI |
| Kafka Exporter | `KafkaAgentSmokeTest`，分别运行两种模式 | 待本分支 CI / Kafka Job |
| ClassLoader / ServiceLoader | JAR 的 `inst/*.classdata` 与服务资源检查 | 待本分支 CI |
| JDK / OS | Linux、Windows × JDK 8、11、17、21、25 | 待本分支 CI |
| JDBC instrumentation 禁用 | 后续针对 JDBC 的应用级集成用例 | 尚未验证 |
| 生产运行 / 动态更新 | 不在此次范围 | 尚未验证 |

## 设计取舍

坚持复用 SDK YAML Loader；不复制 Splunk 的 `SplunkConfiguration` 抽象和 OpAMP 生效配置生成器。若上游将 `ComponentProvider` 稳定化或更改 API，则优先跟随对应版本的官方实现升级。

研究依据来自固定版本代码，未将本设计文档冒充已获批准的 ADR 或已经封存的 RepoFoundry Research。
