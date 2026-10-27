/*
 * Copyright Datafox Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.datafox.opentelemetry.javaagent;

import io.opentelemetry.javaagent.OpenTelemetryAgent;
import java.lang.instrument.Instrumentation;

/** Delegates JVM agent startup to the upstream OpenTelemetry Java Agent. */
public final class DatafoxAgent {

  private static final String FILE_PROPERTY = "datafox.javaagent.logging.file";
  private static final String FILE_ENV = "DATAFOX_JAVAAGENT_LOGGING_FILE";
  private static final String AGENT_LOGGING_PROPERTY = "otel.javaagent.logging";
  private static final String AGENT_LOGGING_ENV = "OTEL_JAVAAGENT_LOGGING";
  private static final String SIMPLE_LOG_FILE_PROPERTY =
      "io.opentelemetry.javaagent.slf4j.simpleLogger.logFile";

  private DatafoxAgent() {}

  public static void premain(String agentArgs, Instrumentation instrumentation) {
    agentmain(agentArgs, instrumentation);
  }

  public static void agentmain(String agentArgs, Instrumentation instrumentation) {
    configureFileLogging();
    OpenTelemetryAgent.agentmain(agentArgs, instrumentation);
  }

  private static void configureFileLogging() {
    String file = System.getProperty(FILE_PROPERTY);
    if (file == null) {
      file = System.getenv(FILE_ENV);
    }
    if (file == null) {
      return;
    }
    if (file.trim().isEmpty()) {
      System.err.println("Invalid " + FILE_PROPERTY + ": file path must not be blank");
      return;
    }

    String loggingMode = System.getProperty(AGENT_LOGGING_PROPERTY);
    if (loggingMode == null) {
      loggingMode = System.getenv(AGENT_LOGGING_ENV);
    }
    if (loggingMode != null && !loggingMode.equalsIgnoreCase("simple")) {
      return;
    }

    // Do not override a direct upstream SimpleLogger configuration.
    if (System.getProperty(SIMPLE_LOG_FILE_PROPERTY) == null) {
      System.setProperty(SIMPLE_LOG_FILE_PROPERTY, file);
    }
  }
}
