plugins {
  id("java-test-fixtures")
}

dependencies {
  compileOnly(project(":bootstrap"))
  compileOnly("io.opentelemetry:opentelemetry-sdk")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling")
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-internal-logging-simple")
  compileOnly("com.google.auto.service:auto-service")
  annotationProcessor("com.google.auto.service:auto-service")
  implementation("com.datafox:datafox-kafka-exporter:0.1.0-alpha-SNAPSHOT")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
  testImplementation("org.mockito:mockito-core:5.24.0")
  testFixturesApi(project(":testing:common"))
  testImplementation("io.opentelemetry:opentelemetry-sdk")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
}

val versionProperties = layout.buildDirectory.file("generated/resources/datafox.properties")
val generateVersionProperties = tasks.register("generateVersionProperties") {
  inputs.property("version", project.version.toString())
  outputs.file(versionProperties)

  doLast {
    val output = versionProperties.get().asFile
    output.parentFile.mkdirs()
    output.writeText("telemetry.distro.version=" + project.version.toString() + "\n")
  }
}

tasks.named<ProcessResources>("processResources") {
  from(generateVersionProperties)
}

tasks.named<JavaCompile>("compileJava") {
  options.release.set(8)
}
