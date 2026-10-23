# OpenTelemetry Java Agent 上游架构证据（v2.32.0）

研究编号：R-001；轮次：RR-001；核查日期：2026-10-23。本文是源码研究记录，不代表 Datafox 架构已获批准，也不代表构建验证通过。

## 版本基线与范围

本轮以 OpenTelemetry Java Instrumentation `v2.32.0` 为基线，对应 SDK `1.66.0`。官方 `main` 已进入 `3.0.0-SNAPSHOT` 开发阶段，因此所有源码依据均固定在 `v2.32.0` 标签。

- [官方版本发布](https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/tag/v2.32.0)
- [版本定义](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/v2.32.0/examples/distro/buildSrc/src/main/kotlin/Versions.kt)
- [官方发行版概念](https://opentelemetry.io/docs/concepts/distributions/)

## 运行时 ClassLoader 边界

根据[官方 Java Agent 结构说明](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/v2.32.0/docs/contributing/javaagent-structure.md)，运行时分为四个部分：

1. **System ClassLoader**：加载 `io.opentelemetry.javaagent.OpenTelemetryAgent`，将必要类加入 Bootstrap 并委托初始化。
2. **Bootstrap ClassLoader**：包含 `javaagent-bootstrap`、部分 API、桥接类及重定位后的 API。内部类不构成稳定扩展契约。
3. **AgentClassLoader**：包含 `javaagent-tooling`、SDK、Exporter、Byte Buddy 和 Instrumentation。其类和资源存放于 `inst/`，类文件使用 `.classdata` 扩展名。
4. **Extension ClassLoader**：隔离加载外部扩展 JAR 或 Agent 内 `extensions/` 中的扩展。加载时可以动态处理未重定位的 OpenTelemetry API 引用。

工程约束：不得将普通依赖直接全部解压到 Agent JAR 根目录；不得随意重定位已经打包的上游类；不得重复实现 Agent 初始化器。

## 两种官方定制打包路径

### 方案 A：标准 Agent Extension

[官方扩展文档](https://opentelemetry.io/docs/zero-code/java/agent/extensions/)和[扩展 Gradle 示例](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/v2.32.0/examples/extension/build.gradle.kts)使用 Shadow 构建独立扩展 JAR。Agent 提供的 API/SPI 依赖采用 `compileOnly`；扩展运行时真正需要的第三方依赖使用 `implementation` 并打包。SPI 通过 `META-INF/services/` 注册，可使用 AutoService 生成。

扩展可先通过 `otel.javaagent.extensions` 外部加载进行测试，随后嵌入上游 Agent JAR 的 `extensions/`，形成单文件发行物。重新打包时需要保留原有 `META-INF/MANIFEST.MF`。

纯 SPI 定制不需要替换 `premain`、建立 `bootstrap` 模块或重建上游 `inst/` 内容。

### 方案 B：完整 Distro Example

[官方 Distro Example](https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/v2.32.0/examples/distro)展示 `agent`、`bootstrap`、`custom`、`instrumentation`、`instrumentation:servlet-3`、`smoke-tests` 和 `testing:agent-for-testing` 等模块。

[Agent 构建脚本](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/v2.32.0/examples/distro/agent/build.gradle.kts)使用 `bootstrapLibs`、`javaagentLibs` 和 `upstreamAgent` 配置。[Shadow 约定插件](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/v2.32.0/examples/distro/buildSrc/src/main/kotlin/otel.javaagent-shadow-conventions.gradle.kts)依次执行新增 Agent 依赖的重定位与 SPI 合并、`inst/*.classdata` 隔离转换，以及和上游 Agent、Bootstrap 组件的最终组装，并设置 Agent Manifest 属性。

该流程适用于必须在 Bootstrap 或 AgentClassLoader 中增加类库的场景。官方示例展示扩展能力，并不要求所有发行版复制其全部模块。

## Gradle、依赖与测试

在 `v2.32.0` 中，[settings.gradle.kts](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/v2.32.0/examples/distro/settings.gradle.kts)使用 Shadow `9.6.1` 和 Muzzle 插件 `2.32.0-alpha`；[Gradle Wrapper](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/v2.32.0/examples/distro/gradle/wrapper/gradle-wrapper.properties)指定 Gradle `9.8.0`。[Java 构建约定](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/v2.32.0/examples/distro/buildSrc/src/main/kotlin/otel.java-conventions.gradle.kts)采用 SDK BOM `1.66.0`、对应 Instrumentation BOM、JUnit、Spotless/Google Java Format，以及 `options.release = 8`。编译目标 Java 8 不等于 Gradle 9.8 可以在 Java 8 JVM 上运行。

[Smoke Test 配置](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/v2.32.0/examples/distro/smoke-tests/build.gradle.kts)将构建生成的 Agent `shadowJar` 路径传入测试。Datafox 后续应验证 Manifest、`-javaagent` 启动、实际遥测、默认配置和失败路径，不能只检查 JAR 是否生成。

## 配置与 SPI 限制

[官方 Agent 配置文档](https://opentelemetry.io/docs/zero-code/java/agent/configuration/)说明常规配置优先级由高到低为：JVM System Properties、环境变量、配置文件、`AutoConfigurationCustomizer#addPropertiesSupplier()` 提供的默认值。发行版默认配置不得覆盖用户显式设置的 `otel.*` 值。

[官方扩展文档](https://opentelemetry.io/docs/zero-code/java/agent/extensions/)指出 `AutoConfigurationCustomizerProvider` 不直接应用于声明式 YAML 配置，声明式配置存在独立扩展机制。两条路径不能在未经实现和测试时宣称行为一致。

[上游版本策略](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/v2.32.0/VERSIONING.md)不对 `-alpha` 制品提供稳定 API 兼容保证。使用 `InstrumentationModule` 或 `InstrumenterCustomizerProvider` 等接口时，应按固定版本检查升级影响；没有业务必要时不依赖 `javaagent-tooling` 内部实现。

## 对 Datafox 的方案比较

| 比较项 | 嵌入标准 Extension | 完整 Distro 打包 |
|---|---|---|
| SDK 默认值、Resource、Sampler 等常规 SPI | 支持 | 支持 |
| 单文件 `-javaagent` JAR | 支持 | 支持 |
| 新增 Agent/Bootstrap 类库 | 不是通用实现路径 | 官方示例已展示 |
| 自定义 Byte Buddy Instrumentation | Extension SPI 可支持，需验证依赖与边界 | 示例包含 Muzzle 和 Agent 集成 |
| 自行处理 relocation 与 `.classdata` | 不需要 | 新增 Agent 类库时需要 |
| 打包复杂度与升级维护面 | 较小 | 较大 |

**研究建议，不代表架构批准：** 若 Datafox 的实际需求可通过标准 SPI 实现，先验证嵌入式 Extension；若无需任何专有逻辑，直接复用上游 Agent 的成本更低。只有存在确切的 Bootstrap 或 AgentClassLoader 新增能力需求时，才引入完整 Distro 构建流程。

## 未验证事项

- 尚未在 Datafox 仓库构建或运行任何 Agent。
- 尚未验证上游制品校验和、Manifest 保留、ServiceLoader 发现和 OTLP 实际输出。
- 尚未确定是否需要声明式 YAML、自定义 Instrumentation、Bootstrap helper 或仅 SDK 配置。
- 尚未确定 Datafox 支持的 JVM、配置范围和升级周期。
- 尚未执行性能、可靠性或 ClassLoader 兼容性测量。
