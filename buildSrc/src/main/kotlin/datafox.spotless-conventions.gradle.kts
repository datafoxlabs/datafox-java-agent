import com.diffplug.gradle.spotless.SpotlessExtension

plugins {
  id("com.diffplug.spotless")
}

extensions.configure<SpotlessExtension>("spotless") {
  java {
    googleJavaFormat()
    licenseHeaderFile(rootProject.file("gradle/spotless.license.java"), "(package|import|public|// Includes work from:)")
    target("src/**/*.java")
    toggleOffOn()
  }
  kotlinGradle {
    ktlint().editorConfigOverride(
      mapOf(
        "indent_size" to "2",
        "continuation_indent_size" to "2",
        "max_line_length" to "160",
        "insert_final_newline" to "true",
        "ktlint_standard_no-wildcard-imports" to "disabled",
        "ktlint_standard_max-line-length" to "disabled",
        "ktlint_standard_trailing-comma-on-call-site" to "disabled",
        "ktlint_standard_trailing-comma-on-declaration-site" to "disabled",
        "ktlint_standard_wrapping" to "disabled"
      )
    )
    target("**/*.gradle.kts")
  }
  format("misc") {
    target(".gitignore", "*.md", "docs/**/*.md", "*.sh", "src/**/*.properties")
    leadingTabsToSpaces()
    trimTrailingWhitespace()
    endWithNewline()
  }
}

tasks.register("formatCode") {
  dependsOn(tasks.named("spotlessApply"))
}

tasks.named("check").configure {
  dependsOn(tasks.named("spotlessCheck"))
}
