# Splunk 工程基础设施对齐清单

基线：[Splunk OpenTelemetry Java Agent v2.32.0](https://github.com/signalfx/splunk-otel-java/tree/v2.32.0)。目标是工程、Gradle、Shadow、评测与发行流程对齐；不复制 Splunk 的产品功能。

状态含义：
- **已实现**：代码已提交；不等于最新 CI 已通过。
- **待 CI 确认**：实现已提交，最新云端构建尚未确认。
- **待补齐**：尚未实现或尚未形成可运行的完整流程。
- **按产品范围排除**：属于 Splunk 专有产品功能，不属于工程基础设施对齐。

## 1. Gradle 工程与依赖管理

| Splunk 机制 | Datafox 状态 | 备注 |
|---|---|---|
| Kotlin DSL + Gradle Wrapper | 已实现 | Wrapper 固定 Gradle 9.8.1，并包含 SHA-256 校验 |
| 独立发行版版本文件 | 已实现 | `version.gradle.kts`，当前 `0.1.0-SNAPSHOT` |
| `:dependencyManagement` Java Platform | 已实现 | OTel BOM、Instrumentation BOM、Agent 与测试依赖约束 |
| 根项目统一子项目约定 | 已实现 | `datafox.java-conventions`、`datafox.spotless-conventions` |
| Java 编译与测试约定 | 待 CI 确认 | Java 主代码使用 `release=8`；测试按测试框架所需 JDK 编译 |
| Spotless / Google Java Format / Kotlin DSL 格式 | 待 CI 确认 | 已接入 `check` |
| 依赖许可证报告 | 待 CI 确认 | `:agent:generateLicenseReport` 与 CI Artifact |
| Build Scan / Develocity | 待补齐 | 不影响 Agent 打包；如需完全对齐构建可观测性，再接入 |

## 2. Agent 打包与 Shadow

| Splunk 机制 | Datafox 状态 | 备注 |
|---|---|---|
| `upstreamAgent` / `bootstrapLibs` / `javaagentLibs` | 已实现 | 三种 Configuration 分开解析 |
| `relocateJavaagentLibs` | 已实现 | 对新增 Agent 类执行 relocation 与依赖排除 |
| `isolateJavaagentLibs` | 已实现 | `.class` → `.classdata`，放入 `inst/` |
| 最终 `shadowJar` | 已实现 | 合并上游 Agent、Bootstrap 类与隔离结果 |
| ServiceLoader 合并 | 待 CI 确认 | Relocation 与最终 `inst/META-INF/services` 两阶段处理 |
| Kotlin module metadata 与 `module-info.class` | 待 CI 确认 | 已配置重复处理和排除规则 |
| Manifest 与薄启动委托 | 待 CI 确认 | `DatafoxAgent` 委托给上游 `OpenTelemetryAgent` |
| `mainShadowJar` 无 classifier 主制品 | 已实现 | 与 `-all` 发行 JAR 分离 |
| Maven Publication / POM / Sources / Javadoc | 已实现 | 发布坐标已配置，未发布到远程仓库 |
| GPG 签名 | 已实现（可选） | 仅在两个 GPG 环境变量同时存在时启用 |
| License report 与上游报告区分 | 待补齐 | 还需确认最终 JAR 中许可证资源的命名和保留规则与 Splunk 一致 |

## 3. 测试与评测体系

| Splunk 机制 | Datafox 状态 | 备注 |
|---|---|---|
| JUnit Platform + 统一测试日志 | 待 CI 确认 | 已在公共 Java 约定中配置 |
| `:testing:common` | 已实现 | 共享 OTel 测试依赖 |
| `:testing:agent-for-testing` | 待 CI 确认 | 复用三阶段打包，依赖官方 `opentelemetry-agent-for-testing` |
| Instrumentation 测试约定 | 待 CI 确认 | 已创建 `datafox.instrumentation-testing-conventions` |
| `testLatestDeps` | 待补齐 | CI 入口已加；动态版本规则、latestDepTest source set 和限制策略尚未完整对齐 |
| Muzzle generation/check 约定 | 已实现（按需应用） | 当前没有 Datafox Instrumentation 模块，因此没有 Muzzle 目标 |
| `:instrumentation:listMuzzleInstrumentations` | 已实现 | CI 逐个执行已注册模块的 Muzzle 任务 |
| 最终 Agent 应用级 Smoke Test | 待 CI 确认 | 启动子 JVM，加载最终 JAR，发送 Span 到 MockWebServer OTLP HTTP 接收端 |
| 跨 JDK / OS 矩阵 | 待补齐 | 当前 CI 以 JDK 25 的 Linux Runner 为主 |
| 应用服务器矩阵 | 按产品范围决定 | 尚未声明支持 Tomcat、Jetty、WebLogic 等特定容器 |
| JMH 基准测试模块 | 待补齐 | 尚无需要量化的 Datafox 专有 Instrumentation 性能目标 |

## 4. CI / 发布

| Splunk 机制 | Datafox 状态 | 备注 |
|---|---|---|
| 构建 + 单元测试 | 待 CI 确认 | `./gradlew build` |
| 最新依赖检查 | 待 CI 确认 | `-PtestLatestDeps=true` 入口已加，机制仍需补完整 |
| Muzzle 检查 | 已接入，暂无目标模块 | 有专有 Instrumentation 后执行 |
| Smoke Test 独立 Job | 已接入，待 CI 确认 | 与普通 `build` 分离 |
| 发行 JAR 作为 CI Artifact | 已实现 | `datafox-javaagent` |
| 第三方许可证报告 Artifact | 已接入，待 CI 确认 | `datafox-license-report` |
| GitHub Release、校验和、签名文件 | 待补齐 | 尚未配置正式发布工作流 |
| Maven Central / Sonatype 远程发布 | 待补齐 | Publication 模型已建立，未配置发布凭据/流程 |
| Docker / Operator / Cloud Foundry 发行渠道 | 按产品范围决定 | 不是当前交付要求 |

## 5. 当前判断

Datafox 已从单一 Shadow 构建脚本扩展为包含公共 Gradle 约定、Bootstrap 入口、测试 Agent、Smoke Test、Muzzle 约定、许可证报告与 Maven Publication 的工程骨架。

**还不能称为完整 1:1 对齐。** 当前主要差距：
1. 最新依赖测试机制尚未完整复制 Splunk；
2. Smoke Test 和测试 Agent 需要最新 CI 结果确认；
3. 跨 JDK/OS 及目标应用服务器矩阵尚未配置；
4. 发布工作流、校验和和最终 JAR 许可证资源管理还未完成。

以上差距只涉及工程基础设施。OpAMP、Profiler、No-code、专有应用服务器 Instrumentation 等产品模块不在本次对齐范围。
