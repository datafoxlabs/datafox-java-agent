plugins {
  `java-platform`
}

javaPlatform {
  allowDependencies()
}

dependencies {
  api(enforcedPlatform("io.opentelemetry:opentelemetry-bom-alpha:1.66.0-alpha"))
  api(enforcedPlatform("io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom-alpha:2.32.0-alpha"))

  constraints {
    api("io.opentelemetry.javaagent:opentelemetry-javaagent:2.32.0")
    api("io.opentelemetry.javaagent:opentelemetry-javaagent-bootstrap:2.32.0")
    api("io.opentelemetry.javaagent:opentelemetry-agent-for-testing:2.32.0-alpha")
    api("org.junit.jupiter:junit-jupiter-api:5.12.2")
    api("org.junit.jupiter:junit-jupiter-engine:5.12.2")
    api("org.junit.platform:junit-platform-launcher:1.12.2")
    api("com.squareup.okhttp3:mockwebserver:4.12.0")
    api("io.opentelemetry:opentelemetry-sdk-testing:1.66.0")
  }
}
