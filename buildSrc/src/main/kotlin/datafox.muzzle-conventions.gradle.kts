plugins {
  id("datafox.java-conventions")
  id("io.opentelemetry.instrumentation.muzzle-generation")
  id("io.opentelemetry.instrumentation.muzzle-check")
}

dependencies {
  add("muzzleTooling", platform(project(":dependencyManagement")))
  add("muzzleBootstrap", platform(project(":dependencyManagement")))
  add("codegen", platform(project(":dependencyManagement")))
  add("muzzleTooling", "io.opentelemetry.javaagent:opentelemetry-javaagent-tooling")
  add("muzzleTooling", "io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api")
  add("muzzleBootstrap", project(":bootstrap"))
  add("muzzleBootstrap", "io.opentelemetry:opentelemetry-api")
  add("codegen", "io.opentelemetry.javaagent:opentelemetry-muzzle")
}
