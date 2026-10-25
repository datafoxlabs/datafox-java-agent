plugins {
  id("java-test-fixtures")
}

dependencies {
  compileOnly(project(":bootstrap"))
  compileOnly("io.opentelemetry:opentelemetry-sdk")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
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
