# Datafox Java Agent

Custom OpenTelemetry Java Agent distribution.

## Engineering principles

- Follow the upstream [OpenTelemetry Java Instrumentation](https://github.com/open-telemetry/opentelemetry-java-instrumentation) architecture and coding conventions.
- Prefer supported upstream extension points instead of forking agent internals.
- Keep distribution-specific logic minimal and compatible with upstream upgrades.
- Reuse standard `otel.*` configuration; reserve `datafox.*` for distribution-specific settings when needed.

## Repository Harness

The repository Harness was initialized using [RepoFoundry AI](https://github.com/XiaoWeiKIN/RepoFoundryAI) 0.15.1. Its project instructions, adapter entrypoints, and validation configuration are committed in this repository.

Development uses GitHub MCP for repository changes and GitHub Actions for commands that require an execution environment. The [Bootstrap workflow](.github/workflows/bootstrap-repofoundry.yml) installs the RepoFoundryAI CLI on an ephemeral GitHub-hosted runner, previews the requested changes, applies them, validates the Harness, and commits generated files. It can also be invoked manually with `workflow_dispatch`. A local toolchain is not required.

## Build

BOOTSTRAP_TODO: Select an upstream OpenTelemetry Java Agent release and establish the corresponding Gradle build and distribution packaging. No build commands are available yet.
