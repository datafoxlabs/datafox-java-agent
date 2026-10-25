pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
  }
  plugins {
    id("com.gradleup.shadow") version "9.6.1"
    id("com.diffplug.spotless") version "8.10.4"
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
)
