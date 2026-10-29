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
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

class KafkaAgentSmokeTest {

  private static final String TOPIC = "datafox-agent-traces";

  @Test
  @EnabledIfEnvironmentVariable(named = "DATAFOX_KAFKA_SMOKE", matches = "true")
  void packagedAgentExportsSpansThroughKafkaOnJavaEight() throws Exception {
    String agentJar = System.getProperty("datafox.agent.jar");
    String classpath = System.getProperty("datafox.smoke.classpath");
    assertNotNull(agentJar);
    assertNotNull(classpath);

    try (KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("apache/kafka:3.9.1"))) {
      kafka.start();

      try (AdminClient admin =
          AdminClient.create(
              Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers()))) {
        admin
            .createTopics(Collections.singletonList(new NewTopic(TOPIC, 1, (short) 1)))
            .all()
            .get(30, TimeUnit.SECONDS);
      }

      Properties consumerConfig = new Properties();
      consumerConfig.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
      consumerConfig.put(ConsumerConfig.GROUP_ID_CONFIG, "datafox-smoke-" + UUID.randomUUID());
      consumerConfig.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

      try (KafkaConsumer<String, byte[]> consumer =
          new KafkaConsumer<>(
              consumerConfig, new StringDeserializer(), new ByteArrayDeserializer())) {
        consumer.subscribe(Collections.singletonList(TOPIC));

        for (boolean declarative : new boolean[] {false, true}) {
          String expectedService =
              declarative ? "datafox-kafka-declarative" : "datafox-kafka-legacy";
          Path output = Files.createTempFile("datafox-kafka-agent-smoke", ".log");
          Path configFile = Files.createTempFile("datafox-kafka-agent-smoke", ".yaml");
          try {
            if (declarative) {
              Files.writeString(
                  configFile,
                  "file_format: \"1.1\"\n"
                      + "resource:\n"
                      + "  attributes:\n"
                      + "    - name: service.name\n"
                      + "      value: "
                      + expectedService
                      + "\n"
                      + "tracer_provider:\n"
                      + "  processors:\n"
                      + "    - batch:\n"
                      + "        schedule_delay: 50\n"
                      + "        exporter:\n"
                      + "          kafka:\n"
                      + "            bootstrap_servers: "
                      + kafka.getBootstrapServers()
                      + "\n"
                      + "            topic: "
                      + TOPIC
                      + "\n"
                      + "            producer:\n"
                      + "              acks: all\n"
                      + "              linger.ms: \"5\"\n"
                      + "              bootstrap.servers: localhost:1\n");
            }

            String javaHome =
                System.getProperty("datafox.smoke.javaHome", System.getProperty("java.home"));
            String java = Path.of(javaHome, "bin", "java").toString();
            java.util.List<String> command = new java.util.ArrayList<>();
            command.add(java);
            command.add("-javaagent:" + agentJar);
            if (declarative) {
              command.add("-Dotel.config.file=" + configFile);
              command.add("-Dotel.traces.exporter=none");
              command.add("-Dotel.service.name=ignored-property");
            } else {
              command.add("-Dotel.traces.exporter=kafka");
              command.add("-Dotel.metrics.exporter=none");
              command.add("-Dotel.logs.exporter=none");
              command.add("-Dotel.bsp.schedule.delay=50");
              command.add(
                  "-Ddatafox.exporter.kafka.bootstrap-servers=" + kafka.getBootstrapServers());
              command.add("-Ddatafox.exporter.kafka.topic=" + TOPIC);
              command.add("-Dotel.service.name=" + expectedService);
            }
            command.add("-cp");
            command.add(classpath);
            command.add(SmokeApplication.class.getName());
            Process process =
                new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .redirectOutput(output.toFile())
                    .start();

            try {
              assertTrue(process.waitFor(40, TimeUnit.SECONDS), "Smoke application did not exit");
              String log = Files.readString(output);
              assertEquals(0, process.exitValue(), log);

              boolean foundSpan = false;
              long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(25);
              while (System.nanoTime() < deadline && !foundSpan) {
                ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, byte[]> record : records) {
                  ExportTraceServiceRequest request =
                      ExportTraceServiceRequest.parseFrom(record.value());
                  for (ResourceSpans resource : request.getResourceSpansList()) {
                    boolean matchingService = false;
                    for (KeyValue attribute : resource.getResource().getAttributesList()) {
                      if ("service.name".equals(attribute.getKey())
                          && expectedService.equals(attribute.getValue().getStringValue())) {
                        matchingService = true;
                        break;
                      }
                    }
                    if (!matchingService) {
                      continue;
                    }
                    for (ScopeSpans scope : resource.getScopeSpansList()) {
                      for (Span span : scope.getSpansList()) {
                        if ("datafox-smoke-span".equals(span.getName())) {
                          foundSpan = true;
                        }
                      }
                    }
                  }
                }
              }
              assertTrue(
                  foundSpan,
                  "No OTLP span arrived via Kafka for "
                      + expectedService
                      + ". Agent output:\n"
                      + log);
            } finally {
              process.destroyForcibly();
            }
          } finally {
            Files.deleteIfExists(output);
            Files.deleteIfExists(configFile);
          }
        }
      }
    }
  }
}
