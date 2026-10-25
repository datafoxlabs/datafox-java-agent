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
  jvmArgumentProviders.add(DatafoxJavaagentProvider(testAgent.flatMap { it.archiveFile }))
  jvmArgs("-Dotel.javaagent.debug=true")
  jvmArgs("-Dotel.javaagent.experimental.initializer.jar=${instrumentationJar.get().archiveFile.get().asFile.absolutePath}")
  jvmArgs("-Dotel.javaagent.testing.additional-library-ignores.enabled=false")
  jvmArgs("-Dotel.javaagent.testing.fail-on-context-leak=true")
  jvmArgs("-Dotel.javaagent.testing.transform-safe-logging.enabled=true")
  jvmArgs("-Dotel.javaagent.add-thread-details=false")

  val buildDirectory = layout.buildDirectory.get().asFile
  val mainClasses = buildDirectory.resolve("classes/java/main")
  val mainResources = buildDirectory.resolve("resources/main")
  classpath = classpath.filter { it != mainClasses && it != mainResources }
}

val testLatestDeps = gradle.startParameter.projectProperties["testLatestDeps"] == "true"
extra["testLatestDeps"] = testLatestDeps

@CacheableRule
abstract class TestLatestDepsRule : ComponentMetadataRule {
  override fun execute(context: ComponentMetadataContext) {
    val version = context.details.id.version
    if (
      version.contains("-alpha", true) ||
      version.contains("-beta", true) ||
      version.contains("-rc", true) ||
      version.contains(".rc", true) ||
      version.contains("-m", true) ||
      version.contains(".m", true) ||
      version.contains(".alpha", true) ||
      version.contains(".beta", true) ||
      version.contains(".cr", true) ||
      version.endsWith("-nf-execution") ||
      GIT_SHA_PATTERN.matches(version) ||
      DATETIME_PATTERN.matches(version)
    ) {
      context.details.status = "milestone"
    }
  }

  companion object {
    private val GIT_SHA_PATTERN = Regex("^.*-[0-9a-f]{7,}$")
    private val DATETIME_PATTERN = Regex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}-\\d{2}-\\d{2}.*$")
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
    val declaredVersion = dependency.version
    if (declaredVersion != null && dependency is ExternalModuleDependency) {
      dependency.version {
        strictly(declaredVersion)
      }
    }
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

// Run Instrumentation tests against the packaged Agent, not just the application classpath.
class DatafoxJavaagentProvider(
  @InputFile
  @PathSensitive(PathSensitivity.RELATIVE)
  val agentJar: Provider<RegularFile>,
) : CommandLineArgumentProvider {
  override fun asArguments(): Iterable<String> =
    listOf("-javaagent:${agentJar.get().asFile.absolutePath}")
}
