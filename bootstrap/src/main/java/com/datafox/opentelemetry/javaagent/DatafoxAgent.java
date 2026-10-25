/*
 * Copyright Datafox Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.datafox.opentelemetry.javaagent;

import io.opentelemetry.javaagent.OpenTelemetryAgent;
import java.lang.instrument.Instrumentation;

/** Delegates JVM agent startup to the upstream OpenTelemetry Java Agent. */
public final class DatafoxAgent {

  private DatafoxAgent() {}

  public static void premain(String agentArgs, Instrumentation instrumentation) {
    agentmain(agentArgs, instrumentation);
  }

  public static void agentmain(String agentArgs, Instrumentation instrumentation) {
    OpenTelemetryAgent.agentmain(agentArgs, instrumentation);
  }
}
