plugins {
  `java-platform`
}

javaPlatform {
  allowDependencies()
}

dependencies {
  api(enforcedPlatform("io.opentelemetry:opentelemetry-bom-alpha:1.66.0-alpha"))
  api(enforcedPlatform("org.junit:junit-bom:6.1.3"))
  api(enforcedPlatform("io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom-alpha:2.32.0-alpha"))

  constraints {
    api("io.opentelemetry.javaagent:opentelemetry-javaagent:2.32.0")
    api("io.opentelemetry.javaagent:opentelemetry-javaagent-bootstrap:2.32.0")
    api("io.opentelemetry.javaagent:opentelemetry-javaagent-bootstrap:2.32.0")
    api("io.opentelemetry.javaagent:opentelemetry-agent-for-testing:2.32.0-alpha")
    api("org.apache.kafka:kafka-clients:3.9.2")
    api("org.slf4j:slf4j-api:2.0.20")
    api("org.slf4j:slf4j-simple:2.0.20")
    api("com.squareup.okhttp3:mockwebserver:4.12.0")
    api("io.opentelemetry.proto:opentelemetry-proto:1.11.1-alpha")
    api("io.opentelemetry:opentelemetry-sdk-testing:1.66.0")
  }
}
