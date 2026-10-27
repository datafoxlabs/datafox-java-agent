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
  void acceptsEnvironmentVariableForAgentLogFile(@TempDir Path tempDir) throws Exception {
    Path agentLog = tempDir.resolve("environment.log");

    runApplication(tempDir, agentLog);

    assertTrue(Files.size(agentLog) > 0);
  }

  @Test
  void reportsBlankLogFileAndKeepsUpstreamLogging(@TempDir Path tempDir) throws Exception {
    String console = runApplication(tempDir, null, "-Ddatafox.javaagent.logging.file= ");

    assertTrue(console.contains("Invalid datafox.javaagent.logging.file"));
    assertTrue(console.contains(APP_STDERR));
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

    assertTrue(console.contains(APP_STDERR));
    assertFalse(Files.exists(agentLog));
  }

  private static String runApplication(Path tempDir, Path environmentLogFile, String... args)
      throws IOException, InterruptedException {
    String javaHome = System.getProperty("datafox.smoke.javaHome", System.getProperty("java.home"));
    String java = Path.of(javaHome, "bin", "java").toString();
    String agentJar = System.getProperty("datafox.agent.jar");
    String classpath = System.getProperty("datafox.smoke.classpath");

    List<String> command = new ArrayList<>();
    command.add(java);
    command.add("-javaagent:" + agentJar);
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
