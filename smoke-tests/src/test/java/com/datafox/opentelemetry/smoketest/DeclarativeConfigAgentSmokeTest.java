/*
 * Copyright Datafox Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.datafox.opentelemetry.smoketest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Span;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DeclarativeConfigAgentSmokeTest {

  @Test
  void packagedAgentUsesYamlToExportSpanAndDatafoxResource(@TempDir Path tempDir) throws Exception {
    String agentJar = System.getProperty("datafox.agent.jar");
    String classpath = System.getProperty("datafox.smoke.classpath");
    assertNotNull(agentJar);
    assertNotNull(classpath);

    try (MockWebServer collector = new MockWebServer()) {
      collector.enqueue(new MockResponse().setResponseCode(200));
      collector.start();
      Path config = tempDir.resolve("otel-config.yaml");
      Files.writeString(
          config,
          "file_format: \"1.1\"\n"
              + "resource:\n"
              + "  attributes:\n"
              + "    - name: service.name\n"
              + "      value: datafox-yaml-smoke\n"
              + "tracer_provider:\n"
              + "  processors:\n"
              + "    - batch:\n"
              + "        schedule_delay: 50\n"
              + "        exporter:\n"
              + "          otlp_http:\n"
              + "            endpoint: "
              + collector.url("/v1/traces")
              + "\n");

      String javaHome =
          System.getProperty("datafox.smoke.javaHome", System.getProperty("java.home"));
      String java = Path.of(javaHome, "bin", "java").toString();
      Path output = tempDir.resolve("console.log");
      Path agentLog = tempDir.resolve("agent.log");
      Process process =
          new ProcessBuilder(
                  java,
                  "-javaagent:" + agentJar,
                  "-Dotel.config.file=" + config,
                  "-Dotel.traces.exporter=none",
                  "-Dotel.service.name=ignored-property",
                  "-Ddatafox.javaagent.logging.file=" + agentLog,
                  "-cp",
                  classpath,
                  SmokeApplication.class.getName())
              .redirectErrorStream(true)
              .redirectOutput(output.toFile())
              .start();

      try {
        assertTrue(process.waitFor(40, TimeUnit.SECONDS), "Smoke application did not exit");
        String log = Files.readString(output);
        assertEquals(0, process.exitValue(), log);
        assertTrue(Files.exists(agentLog), "Early Agent file logging must still work");
        RecordedRequest request =
            collector.takeRequest(Duration.ofSeconds(10).toMillis(), TimeUnit.MILLISECONDS);
        assertNotNull(request, "Declarative OTLP exporter did not send a request: " + log);
        assertEquals("/v1/traces", request.getPath());
        ExportTraceServiceRequest export =
            ExportTraceServiceRequest.parseFrom(request.getBody().readByteArray());
        boolean foundSpan = false;
        boolean foundDistroName = false;
        boolean foundDistroVersion = false;
        boolean foundYamlService = false;
        for (ResourceSpans resource : export.getResourceSpansList()) {
          for (KeyValue attribute : resource.getResource().getAttributesList()) {
            if (attribute.getKey().equals("telemetry.distro.name")
                && attribute.getValue().getStringValue().equals("datafox")) {
              foundDistroName = true;
            }
            if (attribute.getKey().equals("telemetry.distro.version")
                && attribute.getValue().getStringValue().equals("0.1.0-SNAPSHOT")) {
              foundDistroVersion = true;
            }
            if (attribute.getKey().equals("service.name")
                && attribute.getValue().getStringValue().equals("datafox-yaml-smoke")) {
              foundYamlService = true;
            }
          }
          for (ScopeSpans scope : resource.getScopeSpansList()) {
            for (Span span : scope.getSpansList()) {
              if (span.getName().equals("datafox-smoke-span")) {
                foundSpan = true;
              }
            }
          }
        }
        assertTrue(foundSpan, "Declarative OTLP export is missing the application Span");
        assertTrue(foundDistroName, "YAML Resource must use datafox distro name");
        assertTrue(foundDistroVersion, "YAML Resource must include the Datafox distro version");
        assertTrue(foundYamlService, "YAML Resource must not be overridden by System Properties");
      } finally {
        process.destroyForcibly();
      }
    }
  }
}
