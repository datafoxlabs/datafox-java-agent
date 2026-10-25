pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
  }
  plugins {
    id("com.gradleup.shadow") version "9.6.1"
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
    id("com.diffplug.spotless") version "8.10.4"
    id("com.github.jk1.dependency-license-report") version "3.1.4"
  }
}

dependencyResolutionManagement {
  repositories {
    mavenCentral()
  }
}

rootProject.name = "datafox-java-agent"

include(
  ":agent",
  ":bootstrap",
  ":custom",
  ":dependencyManagement",
  ":instrumentation",
  ":smoke-tests",
  ":testing:common",
  ":testing:agent-for-testing",
  ":testing:instrumentation-harness",
)
