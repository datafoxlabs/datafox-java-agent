# 设计文档

设计文档记录工程方案、依赖关系和实现边界；长期架构决策由 ADR 管理。尚未实施的内容必须明确标注为草案。

## 当前设计

- [Datafox Java Agent：基于 Splunk 的 Gradle 发行版工程设计](datafox-gradle-distribution.md) — 实施中；工程基础设施按 Splunk v2.32.0 对齐，OpenTelemetry 官方作为运行时契约依据。
- [Datafox 评测与发行流程](release-and-evaluation.md) — 从 Gradle 构建到跨 JDK/OS Smoke Test、Release Bundle、校验和与草稿发行。
- [Splunk 工程基础设施对齐清单](splunk-parity-checklist.md) — 记录 Gradle、Shadow、测试评测、CI 与发布体系的逐项差距。

- [声明式配置兼容设计](declarative-configuration.md) — SDK / Agent 配置边界、Datafox Kafka/Resource 适配和验证矩阵。

## 文档约定

- 明确状态、依据和未解决的问题。
- 区分已存在的仓库代码与尚未实施的目标结构。
- 架构决策的正式接受/拒绝由 ADR 管理。
- 技术正文使用中文；Gradle Task、SPI 和其他机器识别字段保留原名。
