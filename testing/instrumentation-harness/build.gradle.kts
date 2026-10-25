plugins {
  id("datafox.instrumentation-conventions")
}

tasks.withType<Test>().configureEach {
  jvmArgs("-Dotel.traces.exporter=none")
  jvmArgs("-Dotel.metrics.exporter=none")
  jvmArgs("-Dotel.logs.exporter=none")
}
