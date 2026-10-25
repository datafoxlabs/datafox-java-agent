plugins {
  id("idea")
  id("io.github.gradle-nexus.publish-plugin")
}

apply(from = "version.gradle.kts")

group = "com.datafox"

subprojects {
  group = rootProject.group
  version = rootProject.version
  if (name != "dependencyManagement") {
    apply(plugin = "datafox.java-conventions")
    apply(plugin = "datafox.spotless-conventions")
  }
}

nexusPublishing {
  packageGroup.set("com.datafox")
  repositories {
    sonatype {
      nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
      snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
      username.set(System.getenv("SONATYPE_USER"))
      password.set(System.getenv("SONATYPE_KEY"))
    }
  }
}

if (System.getenv("CI") != null) {
  subprojects {
    configurations.configureEach {
      resolutionStrategy.cacheChangingModulesFor(0, "seconds")
    }
  }
}
