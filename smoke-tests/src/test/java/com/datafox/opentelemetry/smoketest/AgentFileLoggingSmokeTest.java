/*
 * Copyright Datafox Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.datafox.opentelemetry.smoketest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AgentFileLoggingSmokeTest {

  private static final String APP_STDERR = "datafox-application-stderr-marker";
  private static final String UPSTREAM_LOG_FILE_PROPERTY =
      "io.opentelemetry.javaagent.slf4j.simpleLogger.logFile";

  @Test
  void writesAgentLogsToFileWithoutCapturingApplicationStderr(@TempDir Path tempDir)
      throws Exception {
    Path agentLog = tempDir.resolve("agent.log");

    String console = runApplication(tempDir, null, "-Ddatafox.javaagent.logging.file=" + agentLog);

    assertTrue(console.contains(APP_STDERR));
    assertTrue(Files.size(agentLog) > 0, "Agent should write diagnostic logs to its file");
    assertFalse(Files.readString(agentLog).contains(APP_STDERR));
  }

  @Test
  void supportsAgentArguments(@TempDir Path tempDir) throws Exception {
    Path agentLog = tempDir.resolve("agent-argument.log");

    runApplicationWithAgentArgs(tempDir, "datafox.javaagent.logging.file=" + agentLog, null);

    assertTrue(Files.size(agentLog) > 0);
  }

  @Test
  void agentArgumentsOverrideJvmProperties(@TempDir Path tempDir) throws Exception {
    Path agentLog = tempDir.resolve("agent-argument.log");
    Path systemLog = tempDir.resolve("system-property.log");

    runApplicationWithAgentArgs(
        tempDir,
        "datafox.javaagent.logging.file=" + agentLog,
        null,
        "-Ddatafox.javaagent.logging.file=" + systemLog);

    assertTrue(Files.size(agentLog) > 0);
    assertFalse(Files.exists(systemLog));
  }

  @Test
  void readsFilePathFromAgentConfigurationFile(@TempDir Path tempDir) throws Exception {
    Path agentLog = tempDir.resolve("configuration.log");
    Path config = tempDir.resolve("otel.properties");
    Files.writeString(
        config, "datafox.javaagent.logging.file=" + agentLog.toString().replace('\\', '/') + "\n");

    runApplication(tempDir, null, "-Dotel.javaagent.configuration-file=" + config);

    assertTrue(Files.size(agentLog) > 0);
  }

  @Test
  void respectsLoggingModeFromAgentConfigurationFile(@TempDir Path tempDir) throws Exception {
    Path agentLog = tempDir.resolve("disabled.log");
    Path config = tempDir.resolve("otel.properties");
    Files.writeString(
        config,
        "otel.javaagent.logging=none\n" + "datafox.javaagent.logging.file=" + agentLog + "\n");

    String console = runApplication(tempDir, null, "-Dotel.javaagent.configuration-file=" + config);

    assertTrue(console.contains(APP_STDERR));
    assertFalse(Files.exists(agentLog));
  }

  @Test
  void preservesAnExplicitUpstreamSimpleLoggerFile(@TempDir Path tempDir) throws Exception {
    Path requested = tempDir.resolve("requested.log");
    Path upstream = tempDir.resolve("upstream.log");

    runApplication(
        tempDir,
        null,
        "-Ddatafox.javaagent.logging.file=" + requested,
        "-D" + UPSTREAM_LOG_FILE_PROPERTY + "=" + upstream);

    assertFalse(Files.exists(requested), "Distro configuration must not override upstream");
    assertTrue(Files.size(upstream) > 0);
  }

  @Test
  void respectsExplicitlyDisabledAgentLogging(@TempDir Path tempDir) throws Exception {
    Path agentLog = tempDir.resolve("disabled.log");

    String console =
        runApplication(
            tempDir,
            null,
            "-Ddatafox.javaagent.logging.file=" + agentLog,
            "-Dotel.javaagent.logging=none");

    assertTrue(console.contains(APP_STDERR));
    assertFalse(Files.exists(agentLog), "File logging must not override the none mode");
  }

  @Test
  void respectsApplicationLoggingMode(@TempDir Path tempDir) throws Exception {
    Path agentLog = tempDir.resolve("application.log");

    String console =
        runApplication(
            tempDir,
            null,
            "-Ddatafox.javaagent.logging.file=" + agentLog,
            "-Dotel.javaagent.logging=application");

    assertTrue(console.contains(APP_STDERR));
    assertFalse(Files.exists(agentLog));
  }

  @Test
  void acceptsEnvironmentVariableForAgentLogFile(@TempDir Path tempDir) throws Exception {
    Path agentLog = tempDir.resolve("environment.log");

    runApplication(tempDir, agentLog);

    assertTrue(Files.size(agentLog) > 0);
  }

  @Test
  void systemPropertyOverridesEnvironmentVariable(@TempDir Path tempDir) throws Exception {
    Path environment = tempDir.resolve("environment.log");
    Path system = tempDir.resolve("system.log");

    runApplication(tempDir, environment, "-Ddatafox.javaagent.logging.file=" + system);

    assertTrue(Files.size(system) > 0);
    assertFalse(Files.exists(environment));
  }

  @Test
  void reportsBlankLogFileAndKeepsUpstreamLogging(@TempDir Path tempDir) throws Exception {
    String console = runApplication(tempDir, null, "-Ddatafox.javaagent.logging.file= ");

    assertTrue(console.contains("Invalid datafox.javaagent.logging.file"));
    assertTrue(console.contains(APP_STDERR));
  }

  @Test
  void initializesFileWithoutDebugLogging(@TempDir Path tempDir) throws Exception {
    Path agentLog = tempDir.resolve("default.log");

    runApplication(
        tempDir,
        null,
        "-Ddatafox.javaagent.logging.file=" + agentLog,
        "-Dotel.javaagent.debug=false");

    assertTrue(Files.exists(agentLog), "SimpleLogger should open its file in normal mode");
  }

  @Test
  void truncatesAnExistingAgentLogFile(@TempDir Path tempDir) throws Exception {
    Path agentLog = tempDir.resolve("existing.log");
    Files.writeString(agentLog, "old-agent-log-marker");

    runApplication(tempDir, null, "-Ddatafox.javaagent.logging.file=" + agentLog);

    assertTrue(Files.size(agentLog) > 0);
    assertFalse(Files.readString(agentLog).contains("old-agent-log-marker"));
  }

  @Test
  void fallsBackToStderrWhenTheFileCannotBeOpened(@TempDir Path tempDir) throws Exception {
    Path agentLog = tempDir.resolve("missing").resolve("agent.log");

    String console = runApplication(tempDir, null, "-Ddatafox.javaagent.logging.file=" + agentLog);

    assertTrue(console.contains("Could not open ["), "SimpleLogger must report the file error");
    assertTrue(console.contains("Defaulting to System.err"));
    assertTrue(console.contains("[otel.javaagent "), "Agent logs must fall back to stderr");
    assertFalse(Files.exists(agentLog));
  }

  private static String runApplication(Path tempDir, Path environmentLogFile, String... args)
      throws IOException, InterruptedException {
    return runApplicationWithAgentArgs(tempDir, null, environmentLogFile, args);
  }

  private static String runApplicationWithAgentArgs(
      Path tempDir, String agentArgs, Path environmentLogFile, String... args)
      throws IOException, InterruptedException {
    String javaHome = System.getProperty("datafox.smoke.javaHome", System.getProperty("java.home"));
    String java = Path.of(javaHome, "bin", "java").toString();
    String agentJar = System.getProperty("datafox.agent.jar");
    String classpath = System.getProperty("datafox.smoke.classpath");

    List<String> command = new ArrayList<>();
    command.add(java);
    command.add("-javaagent:" + agentJar + (agentArgs == null ? "" : "=" + agentArgs));
    command.add("-Dotel.javaagent.debug=true");
    command.add("-Dotel.traces.exporter=none");
    command.add("-Dotel.metrics.exporter=none");
    command.add("-Dotel.logs.exporter=none");
    command.addAll(Arrays.asList(args));
    command.add("-cp");
    command.add(classpath);
    command.add(LoggingSmokeApplication.class.getName());

    Path output = tempDir.resolve("console.log");
    ProcessBuilder builder = new ProcessBuilder(command);
    if (environmentLogFile != null) {
      builder.environment().put("DATAFOX_JAVAAGENT_LOGGING_FILE", environmentLogFile.toString());
    }
    Process process = builder.redirectErrorStream(true).redirectOutput(output.toFile()).start();
    try {
      assertTrue(process.waitFor(45, TimeUnit.SECONDS), "Smoke application did not exit");
      String console = Files.readString(output);
      assertEquals(0, process.exitValue(), console);
      return console;
    } finally {
      process.destroyForcibly();
    }
  }
}
