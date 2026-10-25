dependencies {
  testImplementation("com.squareup.okhttp3:mockwebserver")
  testImplementation("io.opentelemetry:opentelemetry-api")
}

tasks.named<Test>("test") {
  enabled = enabled && gradle.startParameter.taskNames.any { it.startsWith(":smoke-tests:") }

  val agentJar = project(":agent").tasks.named<Jar>("shadowJar")
  dependsOn(agentJar)
  inputs.file(agentJar.flatMap { it.archiveFile })
  systemProperty("datafox.agent.jar", agentJar.get().archiveFile.get().asFile.absolutePath)
  systemProperty("datafox.smoke.classpath", sourceSets.test.get().runtimeClasspath.asPath)
  tasks.named<JavaCompile>("compileTestJava") {
    options.release.set(17)
  }
  testLogging.showStandardStreams = true
}
