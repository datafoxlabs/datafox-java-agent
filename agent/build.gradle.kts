import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  id("maven-publish")
  id("signing")
  id("com.github.jk1.dependency-license-report")
  id("datafox.shadow-conventions")
}

base.archivesName.set("datafox-javaagent")

java {
  withJavadocJar()
  withSourcesJar()
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

val licenseReportDependencies = configurations.create("licenseReportDependencies") {
  extendsFrom(bootstrapLibs)
  extendsFrom(javaagentLibs)
}

dependencies {
  add("upstreamAgent", platform(project(":dependencyManagement")))
  bootstrapLibs(project(":bootstrap"))
  javaagentLibs(project(":custom"))
  upstreamAgent("io.opentelemetry.javaagent:opentelemetry-javaagent")
}

tasks.named<Jar>("jar") {
  enabled = false
}

tasks.named<ProcessResources>("processResources") {
  from(rootProject.file("licenses")) {
    into("META-INF/licenses")
  }
}

// 1. 重定位发行版新增的 Agent 类，并排除上游 Bootstrap 已提供的依赖。
val relocateJavaagentLibs = tasks.register<ShadowJar>("relocateJavaagentLibs") {
  configurations = listOf(javaagentLibs)
  archiveFileName.set("javaagentLibs-relocated.jar")
  duplicatesStrategy = DuplicatesStrategy.FAIL

  filesMatching("META-INF/*.kotlin_module") {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
  }

  dependencies {
    exclude(dependency("org.slf4j:slf4j-api"))
    exclude(dependency("io.opentelemetry:opentelemetry-api"))
    exclude(dependency("io.opentelemetry:opentelemetry-common"))
    exclude(dependency("io.opentelemetry:opentelemetry-context"))
    exclude(dependency("io.opentelemetry.semconv:opentelemetry-semconv"))
    exclude(dependency("io.opentelemetry.semconv:opentelemetry-semconv-incubating"))
    exclude(dependency("io.opentelemetry:opentelemetry-api-incubator"))
  }

  exclude("META-INF/LICENSE")
  exclude("META-INF/LICENSE.txt")
  exclude("LICENSE.txt")
  exclude("THIRD_PARTY_LICENSES.txt")
  exclude("META-INF/maven/**")
  exclude("META-INF/versions/*/OSGI-INF/MANIFEST.MF")
}

// 2. 将新增 Agent 类移入 inst/，使用上游 AgentClassLoader 识别的 .classdata 布局。
val isolateJavaagentLibs = tasks.register<Copy>("isolateJavaagentLibs") {
  dependsOn(relocateJavaagentLibs)
  from(relocateJavaagentLibs.map { zipTree(it.archiveFile.get().asFile) }) {
    into("inst")
    rename("^(.*)\\.class$", "$1.classdata")
    exclude("META-INF/LICENSE")
    exclude("META-INF/INDEX.LIST")
    exclude("META-INF/*.DSA")
    exclude("META-INF/*.SF")
    exclude("META-INF/MANIFEST.MF")
  }
  into(layout.buildDirectory.dir("isolated/javaagentLibs"))
}

// 3. 将上游 Agent、Bootstrap 类和隔离后的发行版类组装成最终 JAR。
tasks.named<ShadowJar>("shadowJar") {
  configurations = listOf(bootstrapLibs, upstreamAgent)
  dependsOn(isolateJavaagentLibs)
  from(isolateJavaagentLibs)

  filesMatching("META-INF/licenses/licenses.md") {
    path = path.replace("licenses.md", "licenses-datafox-javaagent.md")
  }

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
        "Agent-Class" to "com.datafox.opentelemetry.javaagent.DatafoxAgent",
        "Premain-Class" to "com.datafox.opentelemetry.javaagent.DatafoxAgent",
        "Can-Redefine-Classes" to true,
        "Can-Retransform-Classes" to true,
        "Implementation-Vendor" to "Datafox",
        "Implementation-Version" to "datafox-${project.version}-otel-2.32.0",
      )
    )
  }
}

// Maven 主制品使用无 classifier 的 JAR；-all JAR 保留为直接下载的发行包。
val mainShadowJar = tasks.register<Jar>("mainShadowJar") {
  archiveClassifier.set("")
  from(zipTree(tasks.named<ShadowJar>("shadowJar").get().archiveFile))
  manifest {
    attributes(tasks.named<ShadowJar>("shadowJar").get().manifest.attributes)
  }
}

tasks.named("assemble") {
  dependsOn(tasks.named("shadowJar"), mainShadowJar)
}

publishing {
  publications {
    create<MavenPublication>("maven") {
      groupId = "com.datafox"
      artifactId = "datafox-javaagent"
      version = project.version.toString()
      artifact(mainShadowJar)
      artifact(tasks.named("sourcesJar"))
      artifact(tasks.named("javadocJar"))
      pom {
        name.set("Datafox Java Agent")
        description.set("Datafox distribution of the OpenTelemetry Java Agent")
        url.set("https://github.com/datafoxlabs/datafox-java-agent")
        licenses {
          license {
            name.set("The Apache License, Version 2.0")
            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
          }
        }
        scm {
          url.set("https://github.com/datafoxlabs/datafox-java-agent")
          connection.set("scm:git:https://github.com/datafoxlabs/datafox-java-agent.git")
        }
      }
    }
  }
}

val gpgSecretKey = System.getenv("GPG_SECRET_KEY")
val gpgPassword = System.getenv("GPG_PASSWORD")
if (gpgSecretKey != null && gpgPassword != null) {
  signing {
    useInMemoryPgpKeys(gpgSecretKey, gpgPassword)
    sign(publishing.publications["maven"])
  }
}

licenseReport {
  outputDir = rootProject.file("licenses").absolutePath
  configurations = arrayOf(licenseReportDependencies.name)
  excludeBoms = true
}
