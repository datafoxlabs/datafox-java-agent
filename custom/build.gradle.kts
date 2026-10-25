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
