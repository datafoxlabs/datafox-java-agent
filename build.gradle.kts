apply(from = "version.gradle.kts")

group = "com.datafox"

subprojects {
  group = rootProject.group
  version = rootProject.version
}
