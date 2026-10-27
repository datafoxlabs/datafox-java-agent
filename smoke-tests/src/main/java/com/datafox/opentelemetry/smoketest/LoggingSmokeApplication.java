/*
 * Copyright Datafox Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.datafox.opentelemetry.smoketest;

/** Small child-JVM application for testing Agent diagnostic log isolation. */
public final class LoggingSmokeApplication {

  private LoggingSmokeApplication() {}

  public static void main(String[] args) {
    System.err.println("datafox-application-stderr-marker");
  }
}
