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
| Java 编译与测试约定 | 已实现并通过 CI | Java 主代码使用 `release=8`；测试按测试框架所需 JDK 编译 |
| Spotless / Google Java Format / Kotlin DSL 格式 | 已实现并通过 CI | 已接入 `check`，首次格式问题已修复 |
| 依赖许可证报告 | 已实现并通过 CI | `:agent:generateLicenseReport` 与 CI Artifact |
| Build Scan / Develocity | 已接入并通过默认无上传模式的 CI | Splunk 4.6.0 插件；默认禁止上传，设置 Actions 变量 `DATAFOX_BUILD_SCAN_CONSENT=yes` 后才同意向外部服务上传构建数据 |

## 2. Agent 打包与 Shadow

| Splunk 机制 | Datafox 状态 | 备注 |
|---|---|---|
| `upstreamAgent` / `bootstrapLibs` / `javaagentLibs` | 已实现 | 三种 Configuration 分开解析 |
| `relocateJavaagentLibs` | 已实现 | 对新增 Agent 类执行 relocation 与依赖排除 |
| `isolateJavaagentLibs` | 已实现 | `.class` → `.classdata`，放入 `inst/` |
| 最终 `shadowJar` | 已实现 | 合并上游 Agent、Bootstrap 类与隔离结果 |
| ServiceLoader 合并 | 已实现并通过 JAR 检查 | Relocation 与最终 `inst/META-INF/services` 两阶段处理 |
| Kotlin module metadata 与 `module-info.class` | 已实现并通过 CI | 已配置重复处理和排除规则 |
| Manifest 与薄启动委托 | 已实现并通过 JAR 检查 | `DatafoxAgent` 委托给上游 `OpenTelemetryAgent` |
| `mainShadowJar` 无 classifier 主制品 | 已实现 | 与 `-all` 发行 JAR 分离 |
| Maven Publication / POM / Sources / Javadoc | 已实现 | 发布坐标已配置，未发布到远程仓库 |
| GPG 签名 | CI 临时密钥与 Gradle-signed Maven 制品验签通过 | 真实标签仍需正式 GPG 密钥；CI 验证 Release `.asc`、`SHA256SUMS.asc` 和本地 Maven Publication 的签名，测试签名不作为正式制品上传 |
| License report 与上游报告区分 | 已实现并通过 CI | Release 前生成许可证清单，将 Datafox 许可证归档嵌入并区分于上游资源 |

## 3. 测试与评测体系

| Splunk 机制 | Datafox 状态 | 备注 |
|---|---|---|
| JUnit Platform + 统一测试日志 | 已实现并通过 CI | 已在公共 Java 约定中配置 |
| `:testing:common` | 已实现 | 共享 OTel 测试依赖 |
| `:testing:agent-for-testing` | 已实现并通过 CI | 复用三阶段打包，依赖官方 `opentelemetry-agent-for-testing` |
| Instrumentation 测试约定 | 已通过 Gradle CI（2026-10-25） | 非产品测试夹具通过 `-javaagent` 注入测试 JVM，验证配置了独立 Instrumentation 测试 JAR；尚无产品 Instrumentation |
| `testLatestDeps` | 实现已完善，暂无目标模块 | 已加入预发布版本识别和固定例外约束；待专有 Instrumentation 接入后验证真实依赖切换 |
| Muzzle generation/check 约定 | 已实现（按需应用） | 当前没有 Datafox Instrumentation 模块，因此没有 Muzzle 目标 |
| `:instrumentation:listMuzzleInstrumentations` | 已实现 | CI 逐个执行已注册模块的 Muzzle 任务 |
| 最终 Agent 应用级 Smoke Test | 10/10 通过 CI | 子 JVM 加载最终 Agent，解析 OTLP Protobuf 检查 Span 及发行版资源属性 |
| 跨 JDK / OS 矩阵 | 10/10 通过 CI | Linux / Windows × 应用 JDK 8、11、17、21、25；Gradle 仍运行在 JDK 25，应用夹具编译目标 Java 8 |
| 应用服务器矩阵 | 按产品范围决定 | 尚未声明支持 Tomcat、Jetty、WebLogic 等特定容器 |
| JMH 基准测试模块 | 待补齐 | 尚无需要量化的 Datafox 专有 Instrumentation 性能目标 |

## 4. CI / 发布

| Splunk 机制 | Datafox 状态 | 备注 |
|---|---|---|
| 构建 + 单元测试 | 已通过 CI | `./gradlew build` |
| 最新依赖检查 | 流程已接入；当前跳过 | CI 已确认没有 Instrumentation 模块并跳过；完整最新依赖规则待首个 Instrumentation 模块接入后验证 |
| Muzzle 检查 | 已接入，暂无目标模块 | 有专有 Instrumentation 后执行 |
| Smoke Test 独立 Job | 已通过 CI | 与普通 `build` 分离 |
| 发行 JAR 作为 CI Artifact | 已实现 | `datafox-javaagent` |
| 第三方许可证报告 Artifact | 已通过 CI | `datafox-license-report` |
| GitHub Release、校验和、签名文件 | SHA-256 与离线 GPG 签名检查已通过 | 版本标签强制签名并创建 Draft Release；重复触发仅更新草稿，不覆盖公开 Release；真实标签流程未触发 |
| Maven Central / Sonatype 远程发布 | 本地 Maven Publication 已通过 CI；远程未执行 | CI 验证无 classifier 主 JAR、POM、Sources/Javadoc；正式标签的 `publish_maven=true` 需凭据后才能上传 |
| Docker / Operator / Cloud Foundry 发行渠道 | 按产品范围决定 | 不是当前交付要求 |

## 5. 当前判断

Datafox 已从单一 Shadow 构建脚本扩展为包含公共 Gradle 约定、Bootstrap 入口、测试 Agent、Smoke Test、Muzzle 约定、许可证报告与 Maven Publication 的工程骨架。[最新完整 CI（40179130519）](https://github.com/datafoxlabs/datafox-java-agent/actions/runs/40179130519) 中，Gradle build/unit test、测试 Agent 打包、JAR 检查、许可证报告、Release Bundle 校验及 Linux/Windows × 5 个应用 JDK 的全部 10 组 Smoke Test 均通过。

**尚未完全 1:1 对齐。** 当前主要差距：
1. Latest Dependencies / Muzzle 已有工程入口，但尚没有 Datafox 产品 Instrumentation 作为实际兼容目标；
2. Linux/Windows × Java 8/11/17/21/25 的 10 组运行测试已通过；目标应用服务器矩阵仍需结合产品兼容范围；
3. Release Bundle 和许可证整合通过 CI，但实际带标签的 Draft Release 尚未触发；
4. Maven Central 远程发布、生产密钥验签、构建扫描与产品 Instrumentation 的真实兼容性检查仍未执行；本地签名和测试 Agent 注入均已由 CI 校验。

以上差距只涉及工程基础设施。OpAMP、Profiler、No-code、专有应用服务器 Instrumentation 等产品模块不在本次对齐范围。
