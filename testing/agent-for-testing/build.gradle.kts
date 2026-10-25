import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  id("datafox.shadow-conventions")
}

val bootstrapLibs = configurations.create("bootstrapLibs") {
  isCanBeResolved = true
  isCanBeConsumed = false
}
val javaagentLibs = configurations.create("javaagentLibs") {
  isCanBeResolved = true
  isCanBeConsumed = false
}
val upstreamAgent = configurations.create("upstreamAgent") {
  isCanBeResolved = true
  isCanBeConsumed = false
}

dependencies {
  add("bootstrapLibs", platform(project(":dependencyManagement")))
  add("javaagentLibs", platform(project(":dependencyManagement")))
  add("upstreamAgent", platform(project(":dependencyManagement")))

  bootstrapLibs(project(":bootstrap"))
  javaagentLibs(project(":custom"))
  upstreamAgent("io.opentelemetry.javaagent:opentelemetry-agent-for-testing")
}

tasks.named<Jar>("jar") {
  enabled = false
}

val relocateJavaagentLibs = tasks.register<ShadowJar>("relocateJavaagentLibs") {
  configurations = listOf(javaagentLibs)
  archiveFileName.set("javaagentLibs-relocated.jar")
  duplicatesStrategy = DuplicatesStrategy.FAIL

  dependencies {
    exclude(dependency("org.slf4j:slf4j-api"))
    exclude(dependency("io.opentelemetry:opentelemetry-api"))
    exclude(dependency("io.opentelemetry:opentelemetry-common"))
    exclude(dependency("io.opentelemetry:opentelemetry-context"))
    exclude(dependency("io.opentelemetry.semconv:opentelemetry-semconv"))
    exclude(dependency("io.opentelemetry.semconv:opentelemetry-semconv-incubating"))
    exclude(dependency("io.opentelemetry:opentelemetry-api-incubator"))
  }
}

val isolateJavaagentLibs = tasks.register<Copy>("isolateJavaagentLibs") {
  dependsOn(relocateJavaagentLibs)
  from(relocateJavaagentLibs.map { zipTree(it.archiveFile.get().asFile) }) {
    into("inst")
    rename("^(.*)\\.class$", "$1.classdata")
    exclude("META-INF/INDEX.LIST")
    exclude("META-INF/*.DSA")
    exclude("META-INF/*.SF")
    exclude("META-INF/MANIFEST.MF")
  }
  into(layout.buildDirectory.dir("isolated/javaagentLibs"))
}

tasks.named<ShadowJar>("shadowJar") {
  configurations = listOf(bootstrapLibs, upstreamAgent)
  dependsOn(isolateJavaagentLibs)
  from(isolateJavaagentLibs)

  archiveClassifier.set("all")
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
  mergeServiceFiles("inst/META-INF/services")
  filesMatching("inst/META-INF/services/**") {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
  }
  filesMatching("inst/META-INF/*.kotlin_module") {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
  }

  manifest {
    attributes(
      mapOf(
        "Main-Class" to "io.opentelemetry.javaagent.OpenTelemetryAgent",
        "Agent-Class" to "io.opentelemetry.javaagent.OpenTelemetryAgent",
        "Premain-Class" to "io.opentelemetry.javaagent.OpenTelemetryAgent",
        "Can-Redefine-Classes" to true,
        "Can-Retransform-Classes" to true,
        "Implementation-Vendor" to "Datafox",
        "Implementation-Version" to "datafox-${project.version}-otel-2.32.0",
      )
    )
  }
}

tasks.named("assemble") {
  dependsOn(tasks.named("shadowJar"))
}
