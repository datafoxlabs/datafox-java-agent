/*
 * Copyright Datafox Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.datafox.opentelemetry.smoketest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;

class AgentSmokeTest {

  @Test
  void packagedAgentExportsSpanThroughOtlpHttp() throws Exception {
    String agentJar = System.getProperty("datafox.agent.jar");
    String classpath = System.getProperty("datafox.smoke.classpath");
    assertNotNull(agentJar);
    assertNotNull(classpath);

    try (MockWebServer collector = new MockWebServer()) {
      collector.enqueue(new MockResponse().setResponseCode(200));
      collector.start();

      Path output = Files.createTempFile("datafox-agent-smoke", ".log");
      Process process = createApplicationProcess(agentJar, classpath, collector.url("/").toString(), output);
      try {
        assertTrue(process.waitFor(30, TimeUnit.SECONDS), "Smoke application did not exit");
        String log = Files.readString(output);
        assertEquals(0, process.exitValue(), log);

        RecordedRequest request = collector.takeRequest(Duration.ofSeconds(10).toMillis(), TimeUnit.MILLISECONDS);
        assertNotNull(request, "Agent did not send an OTLP request");
        assertEquals("/v1/traces", request.getPath());
      } finally {
        process.destroyForcibly();
        Files.deleteIfExists(output);
      }
    }
  }

  private static Process createApplicationProcess(
      String agentJar, String classpath, String endpoint, Path output) throws IOException {
    String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
    ProcessBuilder builder =
        new ProcessBuilder(
            java,
            "-javaagent:" + agentJar,
            "-Dotel.traces.exporter=otlp",
            "-Dotel.metrics.exporter=none",
            "-Dotel.logs.exporter=none",
            "-Dotel.exporter.otlp.protocol=http/protobuf",
            "-Dotel.exporter.otlp.endpoint=" + endpoint.substring(0, endpoint.length() - 1),
            "-Dotel.bsp.schedule.delay=50",
            "-Dotel.service.name=datafox-smoke-test",
            "-cp",
            classpath,
            SmokeApplication.class.getName());
    builder.redirectErrorStream(true);
    builder.redirectOutput(output.toFile());
    return builder.start();
  }
}
