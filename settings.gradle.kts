pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
  }
  plugins {
    id("com.gradleup.shadow") version "9.6.1"
  }
}

dependencyResolutionManagement {
  repositories {
    mavenCentral()
  }
}

rootProject.name = "datafox-java-agent"

include("agent")
include("custom")
include("dependencyManagement")
