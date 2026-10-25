val instrumentationTest = tasks.named("test")

subprojects {
  val subProject = this

  plugins.withId("java") {
    instrumentationTest.configure {
      dependsOn(subProject.tasks.named("test"))
    }
  }

  tasks.withType<JavaCompile>().configureEach {
    options.release.set(8)
  }
}

tasks.register("listMuzzleInstrumentations") {
  group = "Help"
  description = "List instrumentation modules that apply Muzzle"
  doLast {
    subprojects
      .filter { it.plugins.hasPlugin("io.opentelemetry.instrumentation.muzzle-check") }
      .map { it.path }
      .forEach(::println)
  }
}

tasks.register("listInstrumentationModules") {
  group = "Help"
  description = "List instrumentation modules using Datafox instrumentation conventions"
  doLast {
    subprojects
      .filter { it.plugins.hasPlugin("datafox.instrumentation-testing-conventions") }
      .map { it.path }
      .forEach(::println)
  }
}
