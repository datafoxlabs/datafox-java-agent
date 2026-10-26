/*
 * Copyright Datafox Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.datafox.opentelemetry.exporter.kafka;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurationException;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.Collections;
import java.util.ServiceLoader;
import org.junit.jupiter.api.Test;

class KafkaSpanExporterProviderTest {

  @Test
  void hasTheKafkaExporterName() {
    assertEquals("kafka", new KafkaSpanExporterProvider().getName());
  }

  @Test
  void isDiscoveredThroughSpi() {
    boolean found = false;
    for (ConfigurableSpanExporterProvider provider :
        ServiceLoader.load(ConfigurableSpanExporterProvider.class)) {
      if (provider.getName().equals("kafka")) {
        assertTrue(provider instanceof KafkaSpanExporterProvider);
        found = true;
      }
    }
    assertTrue(found);
  }

  @Test
  void requiresBootstrapServersBeforeConstructingProducer() {
    ConfigProperties config = mock(ConfigProperties.class);
    when(config.getString("datafox.exporter.kafka.topic")).thenReturn("traces");
    assertThrows(
        ConfigurationException.class, () -> new KafkaSpanExporterProvider().createExporter(config));
  }

  @Test
  void requiresTopicBeforeConstructingProducer() {
    ConfigProperties config = mock(ConfigProperties.class);
    when(config.getString("datafox.exporter.kafka.bootstrap-servers")).thenReturn("localhost:9092");
    assertThrows(
        ConfigurationException.class, () -> new KafkaSpanExporterProvider().createExporter(config));
  }

  @Test
  void buildsAndClosesExporter() {
    ConfigProperties config = mock(ConfigProperties.class);
    when(config.getString("datafox.exporter.kafka.bootstrap-servers")).thenReturn("localhost:9092");
    when(config.getString("datafox.exporter.kafka.topic")).thenReturn("traces");
    when(config.getMap("datafox.exporter.kafka.producer"))
        .thenReturn(Collections.singletonMap("client.id", "datafox-test"));
    SpanExporter exporter = new KafkaSpanExporterProvider().createExporter(config);
    assertNotNull(exporter);
    assertTrue(exporter.shutdown().join(10, java.util.concurrent.TimeUnit.SECONDS).isSuccess());
  }
}
