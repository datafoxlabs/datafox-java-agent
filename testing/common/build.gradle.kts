plugins {
  id("java-library")
}

dependencies {
  api("io.opentelemetry.javaagent:opentelemetry-testing-common")
  api("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api")
  api("io.opentelemetry:opentelemetry-sdk-testing")
}
