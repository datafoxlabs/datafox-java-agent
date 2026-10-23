# OpenTelemetry Java Agent upstream evidence (v2.32.0)

Research: R-001; round: RR-001; observed 2026-10-23. This is a source-backed architecture investigation, not an accepted Datafox architecture or a build validation.

## Baseline and scope

The released Java instrumentation baseline is `v2.32.0` (2026-10-03), targeting SDK `1.66.0`. The `main` example has already advanced to `3.0.0-SNAPSHOT`; reading it as if it were the 2.32.0 distribution would mix contracts. Read all linked source files at the pinned `v2.32.0` tag.

- [Official releases](https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/tag/v2.32.0)
- [Pinned example versions](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/v2.32.0/examples/distro/buildSrc/src/main/kotlin/Versions.kt)
- [Official distribution concept](https://opentelemetry.io/docs/concepts/distributions/)

## Runtime class loader boundaries

The official [Java agent structure](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/v2.32.0/docs/contributing/javaagent-structure.md) separates:

1. System class loader: `io.opentelemetry.javaagent.OpenTelemetryAgent`, which transfers bootstrap components and delegates initialization.
2. Bootstrap class loader: `javaagent-bootstrap`, selected APIs, bridging classes and the relocated API used by instrumentations. Internal classes are not a stable extension contract.
3. Agent class loader: `javaagent-tooling`, SDK, exporters, Byte Buddy, and instrumentation. Agent classes and resources are packed in `inst/`, with `.classdata` instead of ordinary `.class`.
4. Extension class loaders: independently load external extension JARs or embedded JARs from `extensions/`. Unshaded OTel API references may be rewritten at load time.

Consequences: do not unzip ordinary implementation dependencies indiscriminately into the root of the agent JAR, do not relocate already packaged upstream agent classes, and do not add a duplicate Agent initializer.

## Two official customization packaging paths

### Path A — Standard Agent extension

The [official extension docs](https://opentelemetry.io/docs/zero-code/java/agent/extensions/) and [extension Gradle example](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/v2.32.0/examples/extension/build.gradle.kts) describe an isolated extension JAR built with Shadow. Interfaces provided by the agent use `compileOnly`, runtime-only third-party libraries use `implementation` and are bundled into the extension JAR. SPI registrations use `META-INF/services/` (often created using AutoService). The extension can be loaded via `otel.javaagent.extensions` for testing, then embedded in the upstream agent JAR under `extensions/` for single-file deployment. Preserve the upstream `META-INF/MANIFEST.MF` on repackaging.

This path does **not** require replacing `premain`, creating a `bootstrap` module, or extracting/rebuilding `inst/` for purely SPI-based additions. A one-JAR distribution can use this path without copying the full Distro Example.

### Path B — Full Distro Example

The [Distro Example](https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/v2.32.0/examples/distro) demonstrates vendor customizations beyond extension-only packaging. Its Gradle settings include `agent`, `bootstrap`, `custom`, `instrumentation`, `instrumentation:servlet-3`, `smoke-tests`, `testing:agent-for-testing`.

The [agent build](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/v2.32.0/examples/distro/agent/build.gradle.kts) declares `bootstrapLibs`, `javaagentLibs` and `upstreamAgent`. The [shadow convention](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/v2.32.0/examples/distro/buildSrc/src/main/kotlin/otel.javaagent-shadow-conventions.gradle.kts) processes additional agent libraries in stages: Shadow relocation and service merging, conversion of agent classes to `inst/*.classdata`, and assembly with the upstream Agent plus bootstrap components. It sets the expected Java Agent Manifest attributes.

This pipeline matters when adding new classes that must live in Bootstrap or the Agent class loader. The example is a demonstration of available mechanisms, not proof that all those modules are mandatory for Datafox.

## Gradle, dependencies, tests

At `v2.32.0`, the [Distro settings](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/v2.32.0/examples/distro/settings.gradle.kts) pin `com.gradleup.shadow:9.6.1`, the Muzzle plugins at `2.32.0-alpha`, and the upstream example's [Gradle Wrapper](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/v2.32.0/examples/distro/gradle/wrapper/gradle-wrapper.properties) points to Gradle 9.8.0. The [Java conventions](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/v2.32.0/examples/distro/buildSrc/src/main/kotlin/otel.java-conventions.gradle.kts) use the SDK BOM `1.66.0`, instrumentation BOMs matching `2.32.0`, JUnit, Spotless/Google Java Format, and `options.release = 8`. Build JDK and bytecode target are separate constraints; do not interpret `release=8` as “build Gradle 9.8 using Java 8”.

The [Distro smoke-test Gradle task](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/v2.32.0/examples/distro/smoke-tests/build.gradle.kts) injects the built Agent `shadowJar` location into test JVM arguments. Datafox should validate JAR Manifest, attachment with `-javaagent`, actual telemetry, resource/default configuration and failure paths, not merely `jar` success.

## Configuration and SPI caveat

The [agent configuration documentation](https://opentelemetry.io/docs/zero-code/java/agent/configuration/) defines default configuration precedence, from highest to lowest: JVM system properties, environment variables, configuration file, values supplied by `AutoConfigurationCustomizer#addPropertiesSupplier()`. A distribution's default supplier should therefore avoid overwriting an explicit user's `otel.*` setting.

The [extension documentation](https://opentelemetry.io/docs/zero-code/java/agent/extensions/) states that `AutoConfigurationCustomizerProvider` does not apply to declarative configuration; declarative YAML has distinct extension APIs. No design can claim identical custom configuration behavior on both paths without implementation and tests.

The [versioning policy](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/v2.32.0/VERSIONING.md) explicitly excludes `-alpha` artifacts from stable API guarantees. Both `InstrumentationModule` integration and `InstrumenterCustomizerProvider` may require version-specific maintenance. Do not depend on `javaagent-tooling` internals without a proven need.

## Comparison for Datafox

| Criterion | Embed a standard extension | Full Distro packaging |
|---|---|---|
| SDK defaults / Resource / Sampler SPI | Supported in standard configuration mode | Supported |
| Single `-javaagent` JAR | Supported | Supported |
| Custom Agent/Bootstrap classes | Not a general-purpose route | Demonstrated |
| Extra Byte Buddy `InstrumentationModule` | Extension SPI supports modules; validate scope and dependency boundary | Demonstrated with Muzzle and agent integration |
| Own relocation and `inst/*.classdata` pipeline | Not needed for embedded extension | Necessary for extra AgentClassLoader libraries |
| Repackaging and upgrade surface | Small | Larger |

**Evidence-based recommendation, not an architecture approval:** begin with standard upstream Agent packaging plus a narrowly scoped extension only when an actual Datafox SPI use case exists. Escalate to the full Distro build model when a supported requirement needs Bootstrap or AgentClassLoader additions that the standard extension packaging cannot supply. Do not create every example module preemptively.

## Unverified items / falsifiers

- No Datafox functionality, upstream JAR or Gradle project has yet been built or tested in this Research round.
- Exact artifact source/checksum, manifest preservation, extension ServiceLoader discovery, and telemetry output must be demonstrated by a pinned-version GitHub Actions prototype.
- Determine whether Datafox requires declarative YAML support, dedicated instrumentation, Bootstrap-visible helpers or only SDK customization. These requirements change the packaging decision.
- The current Datafox repository does not yet declare a compatible minimum JVM, supported configurations, or upgrade cadence.

No performance, reliability or ClassLoader compatibility measurement has been performed.
