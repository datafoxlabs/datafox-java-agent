dependencies {
  implementation("io.opentelemetry:opentelemetry-api")
  testImplementation("com.squareup.okhttp3:mockwebserver")
  testImplementation("io.opentelemetry.proto:opentelemetry-proto")
  testImplementation("org.apache.kafka:kafka-clients")
  testImplementation("org.testcontainers:testcontainers-kafka:2.0.5")
}

tasks.named<JavaCompile>("compileJava") {
  options.release.set(8)
}

tasks.named<JavaCompile>("compileTestJava") {
  options.release.set(17)
}

tasks.named<Test>("test") {
  enabled = enabled && gradle.startParameter.taskNames.any { it.startsWith(":smoke-tests:") }

  val agentJar = project(":agent").tasks.named<Jar>("shadowJar")
  dependsOn(agentJar)
  inputs.file(agentJar.flatMap { it.archiveFile })
  systemProperty("datafox.agent.jar", agentJar.get().archiveFile.get().asFile.absolutePath)
  val smokeTestJavaHome = providers.environmentVariable("SMOKE_TEST_JAVA_HOME").orNull
  if (smokeTestJavaHome != null) {
    systemProperty("datafox.smoke.javaHome", smokeTestJavaHome)
  }
  systemProperty("datafox.smoke.classpath", sourceSets.test.get().runtimeClasspath.asPath)
  testLogging.showStandardStreams = true
}
