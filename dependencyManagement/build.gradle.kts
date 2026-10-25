plugins {
  `java-platform`
}

javaPlatform {
  allowDependencies()
}

dependencies {
  api(platform("io.opentelemetry:opentelemetry-bom:1.66.0"))
  api(platform("io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom:2.32.0"))

  constraints {
    api("io.opentelemetry.javaagent:opentelemetry-javaagent:2.32.0")
  }
}
