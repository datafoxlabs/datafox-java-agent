import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  id("datafox.java-conventions")
  id("datafox.shadow-conventions")
}

val testInstrumentation = configurations.create("testInstrumentation")

dependencies {
  add("testInstrumentation", platform(project(":dependencyManagement")))
  compileOnly("io.opentelemetry:opentelemetry-sdk")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
  compileOnly("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api")
  compileOnly("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api-incubator")
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api")
  compileOnly("io.opentelemetry.semconv:opentelemetry-semconv")
  annotationProcessor("com.google.auto.service:auto-service")
  compileOnly("com.google.auto.service:auto-service")
  compileOnly(project(":bootstrap"))
  testImplementation(project(":testing:common"))
  testCompileOnly(project(":bootstrap"))
}

tasks.named<ShadowJar>("shadowJar") {
  configurations = listOf(project.configurations.runtimeClasspath.get(), testInstrumentation)
  archiveFileName.set("agent-testing.jar")
}

evaluationDependsOn(":testing:agent-for-testing")

tasks.withType<Test>().configureEach {
  val testAgent = project(":testing:agent-for-testing").tasks.named<ShadowJar>("shadowJar")
  val instrumentationJar = tasks.named<ShadowJar>("shadowJar")

  dependsOn(testAgent, instrumentationJar)
  inputs.file(instrumentationJar.flatMap { it.archiveFile })
  jvmArgs("-Dotel.javaagent.debug=true")
  jvmArgs("-Dotel.javaagent.experimental.initializer.jar=${instrumentationJar.get().archiveFile.get().asFile.absolutePath}")
  jvmArgs("-Dotel.javaagent.testing.additional-library-ignores.enabled=false")
  jvmArgs("-Dotel.javaagent.testing.fail-on-context-leak=true")
}

val testLatestDeps = gradle.startParameter.projectProperties["testLatestDeps"] == "true"
extra["testLatestDeps"] = testLatestDeps

@CacheableRule
abstract class TestLatestDepsRule : ComponentMetadataRule {
  override fun execute(context: ComponentMetadataContext) {
    val version = context.details.id.version
    if (listOf("-alpha", "-beta", "-rc", ".rc", "-m", ".m", ".alpha", ".beta", ".cr")
        .any { version.contains(it, ignoreCase = true) }
      || version.endsWith("-nf-execution")
      || Regex("^.*-[0-9a-f]{7,}$").matches(version)) {
      context.details.status = "milestone"
    }
  }
}

val library = configurations.create("library") {
  isCanBeResolved = false
  isCanBeConsumed = false
}
val testLibrary = configurations.create("testLibrary") {
  isCanBeResolved = false
  isCanBeConsumed = false
}
val latestDepTestLibrary = configurations.create("latestDepTestLibrary") {
  isCanBeResolved = false
  isCanBeConsumed = false
}

val testImplementationConfiguration = configurations.getByName("testImplementation")

listOf(library, testLibrary).forEach { configuration ->
  configuration.dependencies.whenObjectAdded {
    val dependency = copy()
    if (testLatestDeps && dependency is ExternalModuleDependency) {
      dependency.version {
        require("latest.release")
      }
    }
    testImplementationConfiguration.dependencies.add(dependency)
  }
}

if (testLatestDeps) {
  dependencies.components.all<TestLatestDepsRule>()
  latestDepTestLibrary.dependencies.whenObjectAdded {
    val dependency = copy()
    testImplementationConfiguration.dependencies.add(dependency)
  }
}

configurations.named("compileOnly") {
  extendsFrom(library)
}

tasks.register("generateInstrumentationVersionFile") {
  val name = "com.datafox.${project.name}"
  val version = project.version.toString()
  inputs.property("instrumentation.name", name)
  inputs.property("instrumentation.version", version)
  val outputDir = layout.buildDirectory.dir("generated/instrumentationVersion/META-INF/io/opentelemetry/instrumentation")
  outputs.dir(outputDir)
  doLast {
    outputDir.get().file("$name.properties").asFile.apply {
      parentFile.mkdirs()
      writeText("version=$version")
    }
  }
}

sourceSets {
  main {
    output.dir("build/generated/instrumentationVersion", "builtBy" to "generateInstrumentationVersionFile")
  }
}
