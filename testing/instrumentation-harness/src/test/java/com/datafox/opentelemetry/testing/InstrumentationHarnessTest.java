/*
 * Copyright Datafox Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.datafox.opentelemetry.testing;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.management.ManagementFactory;
import org.junit.jupiter.api.Test;

class InstrumentationHarnessTest {

  @Test
  void runsWithPackagedTestingAgentAndInitializer() {
    assertTrue(
        ManagementFactory.getRuntimeMXBean().getInputArguments().stream()
            .anyMatch(argument -> argument.startsWith("-javaagent:")));
    assertNotNull(System.getProperty("otel.javaagent.experimental.initializer.jar"));
  }
}
