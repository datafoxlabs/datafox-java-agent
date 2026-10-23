# 设计文档

设计文档记录工程方案、依赖关系和实现边界；长期架构决策由 ADR 管理。尚未实施的内容必须明确标注为草案。

## 当前设计

- [Datafox Java Agent：基于 Splunk 的 Gradle 发行版工程设计](datafox-gradle-distribution.md) — 草案；优先参考 Splunk v2.32.0 的模块组织和三阶段 Shadow 打包，OpenTelemetry 官方作为运行时契约依据。

## 文档约定

- 明确状态、依据和未解决的问题。
- 区分已存在的仓库代码与尚未实施的目标结构。
- 架构决策的正式接受/拒绝由 ADR 管理。
- 技术正文使用中文；Gradle Task、SPI 和其他机器识别字段保留原名。
