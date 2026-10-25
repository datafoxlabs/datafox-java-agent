plugins {
  `kotlin-dsl`
  id("com.diffplug.spotless") version "8.10.4"
}

repositories {
  gradlePluginPortal()
  mavenCentral()
}

dependencies {
  implementation(gradleApi())
  implementation("com.diffplug.spotless:spotless-plugin-gradle:8.10.4")
  implementation("io.opentelemetry.instrumentation:gradle-plugins:2.32.0-alpha")
  implementation("com.gradleup.shadow:shadow-gradle-plugin:9.6.1")
}
