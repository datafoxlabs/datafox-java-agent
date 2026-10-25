import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  id("java")
  id("datafox.shadow-conventions")
}

base.archivesName.set("datafox-javaagent")

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
  add(upstreamAgent.name, platform(project(":dependencyManagement")))
  bootstrapLibs(project(":bootstrap"))
  javaagentLibs(project(":custom"))
  upstreamAgent("io.opentelemetry.javaagent:opentelemetry-javaagent")
}

tasks.named<Jar>("jar") {
  enabled = false
}

// 1. 重定位发行版新增的 Agent 类，排除上游 Bootstrap 已提供的依赖。
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
    exclude(dependency("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api"))
    exclude(dependency("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api-incubator"))
  }
}

// 2. 将新增的 Agent 类移入 inst/，保留上游 AgentClassLoader 的隔离结构。
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

// 3. 合并上游 Agent、Bootstrap 类和隔离后的发行版类。
tasks.named<ShadowJar>("shadowJar") {
  configurations = listOf(bootstrapLibs, upstreamAgent)
  dependsOn(isolateJavaagentLibs)
  from(isolateJavaagentLibs)

  archiveClassifier.set("")
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
  mergeServiceFiles("inst/META-INF/services")
  filesMatching("inst/META-INF/services/**") {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
  }

  manifest {
    attributes(
      mapOf(
        "Main-Class" to "io.opentelemetry.javaagent.OpenTelemetryAgent",
        "Agent-Class" to "com.datafox.opentelemetry.javaagent.DatafoxAgent",
        "Premain-Class" to "com.datafox.opentelemetry.javaagent.DatafoxAgent",
        "Can-Redefine-Classes" to "true",
        "Can-Retransform-Classes" to "true",
        "Implementation-Vendor" to "Datafox",
        "Implementation-Version" to "datafox-${project.version}-otel-2.32.0",
      )
    )
  }
}

tasks.named("assemble") {
  dependsOn(tasks.named("shadowJar"))
}
