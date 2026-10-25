apply(from = "version.gradle.kts")

group = "com.datafox"

subprojects {
  group = rootProject.group
  version = rootProject.version
  if (name != "dependencyManagement" && name != "instrumentation") {
    apply(plugin = "datafox.java-conventions")
    apply(plugin = "datafox.spotless-conventions")
  }
}
