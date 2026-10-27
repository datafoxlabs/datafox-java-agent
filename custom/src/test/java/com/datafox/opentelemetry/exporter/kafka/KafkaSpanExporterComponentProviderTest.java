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

import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurationException;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.Collections;
import java.util.ServiceLoader;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class KafkaSpanExporterComponentProviderTest {

  @Test
  void registersKafkaForDeclarativeConfig() {
    ComponentProvider provider = new KafkaSpanExporterComponentProvider();
    assertEquals("kafka", provider.getName());
    assertEquals(SpanExporter.class, provider.getType());
    boolean found = false;
    for (ComponentProvider loaded : ServiceLoader.load(ComponentProvider.class)) {
      if (loaded.getClass().equals(KafkaSpanExporterComponentProvider.class)) {
        found = true;
      }
    }
    assertTrue(found);
  }

  @Test
  void validatesConfigurationBeforeCreatingProducer() {
    DeclarativeConfigProperties config = mock(DeclarativeConfigProperties.class);
    when(config.getString("topic")).thenReturn("traces");
    assertThrows(
        ConfigurationException.class,
        () -> new KafkaSpanExporterComponentProvider().create(config));
  }

  @Test
  void rejectsProducerWhenItIsNotAMapping() {
    DeclarativeConfigProperties config = mock(DeclarativeConfigProperties.class);
    when(config.getPropertyKeys()).thenReturn(Collections.singleton("producer"));
    when(config.getString("bootstrap_servers")).thenReturn("localhost:9092");
    when(config.getString("topic")).thenReturn("traces");

    ConfigurationException exception =
        assertThrows(
            ConfigurationException.class,
            () -> new KafkaSpanExporterComponentProvider().create(config));
    assertEquals("kafka.producer must be a mapping", exception.getMessage());
  }

  @Test
  void createsAndClosesExporter() {
    DeclarativeConfigProperties config = mock(DeclarativeConfigProperties.class);
    when(config.getString("bootstrap_servers")).thenReturn("localhost:9092");
    when(config.getString("topic")).thenReturn("traces");
    SpanExporter exporter = new KafkaSpanExporterComponentProvider().create(config);
    assertNotNull(exporter);
    assertTrue(exporter.shutdown().join(10, TimeUnit.SECONDS).isSuccess());
  }
}
