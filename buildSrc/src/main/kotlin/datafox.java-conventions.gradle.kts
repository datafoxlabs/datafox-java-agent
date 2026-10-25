import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
  java
}

evaluationDependsOn(":dependencyManagement")

val dependencyManagementConf = configurations.create("dependencyManagement") {
  isCanBeConsumed = false
  isCanBeResolved = false
}

afterEvaluate {
  configurations.configureEach {
    if (isCanBeResolved && !isCanBeConsumed) {
      extendsFrom(dependencyManagementConf)
    }
  }
}

dependencies {
  add(dependencyManagementConf.name, platform(project(":dependencyManagement")))
  add("testImplementation", "org.junit.jupiter:junit-jupiter-api")
  add("testImplementation", "org.junit.jupiter:junit-jupiter-params")
  add("testImplementation", "org.junit.jupiter:junit-jupiter-engine")
  add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
  add("testRuntimeOnly", "org.slf4j:slf4j-api")
}

tasks.withType<Test>().configureEach {
  useJUnitPlatform()
  testLogging {
    exceptionFormat = TestExceptionFormat.FULL
    showExceptions = true
    showCauses = true
    showStackTraces = true
    showStandardStreams = true
  }
  reports {
    junitXml.isOutputPerTestCase = true
  }
}

tasks.withType<JavaCompile>().configureEach {
  options.compilerArgs.add("-Xlint:-options")
}

tasks.named<Jar>("jar") {
  duplicatesStrategy = DuplicatesStrategy.FAIL
}
