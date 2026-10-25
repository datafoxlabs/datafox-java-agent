plugins {
  `java-library`
}

dependencies {
  compileOnly(platform(project(":dependencyManagement")))
  compileOnly("io.opentelemetry:opentelemetry-sdk")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
}

tasks.withType<JavaCompile>().configureEach {
  options.release.set(8)
}
