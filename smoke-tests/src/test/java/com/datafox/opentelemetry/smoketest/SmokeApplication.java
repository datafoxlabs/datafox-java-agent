/*
 * Copyright Datafox Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.datafox.opentelemetry.smoketest;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;

/** Small child-JVM application used to exercise the packaged Agent. */
public final class SmokeApplication {

  private SmokeApplication() {}

  public static void main(String[] args) {
    Span span =
        GlobalOpenTelemetry.get()
            .getTracer("datafox.smoke-test")
            .spanBuilder("datafox-smoke-span")
            .startSpan();
    span.end();
  }
}
