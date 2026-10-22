# Datafox Java Agent

Custom OpenTelemetry Java Agent distribution.

## Engineering principles

- Follow the upstream [OpenTelemetry Java Instrumentation](https://github.com/open-telemetry/opentelemetry-java-instrumentation) architecture and coding conventions.
- Prefer supported upstream extension points instead of forking agent internals.
- Keep distribution-specific logic minimal and compatible with upstream upgrades.
- Reuse standard `otel.*` configuration; reserve `datafox.*` for distribution-specific settings when needed.

## Repository bootstrap

The repository is prepared for [RepoFoundry AI](https://github.com/XiaoWeiKIN/RepoFoundryAI). Install its CLI, then preview, apply and validate the official repository Harness:

```bash
curl -fsSL https://raw.githubusercontent.com/XiaoWeiKIN/RepoFoundryAI/main/install.py | python3 -
repofoundry --repo . bootstrap --all-adapters
repofoundry --repo . bootstrap --all-adapters --apply
repofoundry --repo . validate
```

Review any reported conflicts before applying. The RepoFoundry Harness is not yet installed in this repository.

## Build

BOOTSTRAP_TODO: Select an upstream OpenTelemetry Java Agent release and establish the corresponding Gradle build and distribution packaging. No build commands are available yet.
