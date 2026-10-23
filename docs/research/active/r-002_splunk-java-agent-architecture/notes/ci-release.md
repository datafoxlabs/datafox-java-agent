# Splunk Java Agent 工程流程、测试与发布

Research：R-002；源码基线：`signalfx/splunk-otel-java@v2.32.0`。本篇只描述仓库中可核查的 CI/发布定义，**不执行任何构建或测试**。

## 1. 构建与版本策略

[version.gradle.kts](https://github.com/signalfx/splunk-otel-java/blob/v2.32.0/version.gradle.kts) 定义发行版版本；[dependencyManagement/build.gradle.kts](https://github.com/signalfx/splunk-otel-java/blob/v2.32.0/dependencyManagement/build.gradle.kts) 固定 OTel SDK、Instrumentation、Contrib 及第三方依赖。发行版版本与上游版本分别管理，虽然 `v2.32.0` 两者数值相同。

[VERSIONING.md](https://github.com/signalfx/splunk-otel-java/blob/v2.32.0/VERSIONING.md) 声明 Splunk 遵循 SemVer、稳定配置变更需要考虑兼容性，并计划尽快跟进上游月度版本；这些是 Splunk 自身维护承诺，不应直接移植为 Datafox 的对外 SLA。

## 2. CI 分层

[ci.yaml](https://github.com/signalfx/splunk-otel-java/blob/v2.32.0/.github/workflows/ci.yaml) 和 [pr.yaml](https://github.com/signalfx/splunk-otel-java/blob/v2.32.0/.github/workflows/pr.yaml) 主要包含：

- `./gradlew build`：常规构建与测试；
- `./gradlew check -x spotlessCheck -PtestLatestDeps=true`：针对目标库较新版本的兼容性检查；
- `instrumentation:listMuzzleInstrumentations`：动态构造 Instrumentation Muzzle Matrix，分别运行各模块的 `muzzle`；
- `:smoke-tests:test -PsmokeTestSuite=...`：按 Tomcat、Jetty、WebLogic、WebSphere、Profiler 等场景拆分 Smoke Test；
- 许可证、格式、脚本、链接检查等附加任务。

[smoke-tests/build.gradle.kts](https://github.com/signalfx/splunk-otel-java/blob/v2.32.0/smoke-tests/build.gradle.kts) 显式要求 `:smoke-tests:` 任务才执行测试，避免常规构建重复运行耗时的应用服务器矩阵。Smoke Test 使用最终 Agent `shadowJar`，验证的对象是发行物而非仅编译后的 Java 类。

这些流程是 Splunk 产品范围带来的测试投入。Datafox 在没有专有 Instrumentation 时不需要 Muzzle Matrix；在没有应用服务器支持承诺时也不需要大量测试镜像。

## 3. 发行包与渠道

[agent/build.gradle.kts](https://github.com/signalfx/splunk-otel-java/blob/v2.32.0/agent/build.gradle.kts) 为 Maven 主制品创建 `mainShadowJar`（无 classifier），并将 `shadowJar` 作为 `all` classifier 构建产物。源码/Javadoc JAR、POM、GPG 签名、第三方许可证报告由专门的 Gradle 配置管理。

[release.sh](https://github.com/signalfx/splunk-otel-java/blob/v2.32.0/scripts/release.sh) 生成 Maven 发布物、Metadata、额外 CSA Bundle、Cloud Foundry Buildpack、SHA-256 校验和和签名，再创建 GitHub Draft Release。脚本注释说明其依赖 GitLab CI 变量；不能误写为整个发布完全由 GitHub Actions 执行。

[Dockerfile-ghcr](https://github.com/signalfx/splunk-otel-java/blob/v2.32.0/Dockerfile-ghcr) 将发行版 JAR 放入 BusyBox 镜像，并建立 `/javaagent.jar` 链接以适配 Operator 注入。

## 4. Datafox 适用范围

可参考：固定上游版本、单文件 JAR 产物、发行版与上游版本分别标识、CI 构建分层、最终 JAR Smoke Test 的设计、必要的许可证与来源说明。

暂不引入：专有应用服务器矩阵、最新依赖测试矩阵、Maven Central/GPG/Cloud Foundry/CSA 发布管线。是否增加取决于明确的支持范围和交付渠道。

本轮是 Research，按项目要求不创建独立验证原型；这些验证在 Datafox 正式实现阶段按风险选择。
