/*
 * Copyright Datafox Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.datafox.opentelemetry.logging;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.logging.simple.Slf4jSimpleLoggingCustomizer;
import io.opentelemetry.javaagent.tooling.LoggingCustomizer;
import io.opentelemetry.javaagent.tooling.config.EarlyInitAgentConfig;

/** Configures the upstream simple logger for the Datafox distribution. */
@AutoService(LoggingCustomizer.class)
public final class DatafoxSimpleLoggingCustomizer implements LoggingCustomizer {

  private static final String FILE_PROPERTY = "datafox.javaagent.logging.file";
  private static final String SIMPLE_LOG_FILE_PROPERTY =
      "io.opentelemetry.javaagent.slf4j.simpleLogger.logFile";

  private final Slf4jSimpleLoggingCustomizer delegate = new Slf4jSimpleLoggingCustomizer();

  @Override
  public String name() {
    return delegate.name();
  }

  @Override
  public void init() {
    String file = EarlyInitAgentConfig.get().getString(FILE_PROPERTY);
    if (file != null) {
      if (file.trim().isEmpty()) {
        System.err.println("Invalid " + FILE_PROPERTY + ": file path must not be blank");
      } else if (System.getProperty(SIMPLE_LOG_FILE_PROPERTY) == null) {
        System.setProperty(SIMPLE_LOG_FILE_PROPERTY, file);
      }
    }
    delegate.init();
  }

  @Override
  public void onStartupSuccess() {
    delegate.onStartupSuccess();
  }

  @Override
  public void onStartupFailure(Throwable throwable) {
    delegate.onStartupFailure(throwable);
  }
}
