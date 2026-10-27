/*
 * Copyright Datafox Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.datafox.opentelemetry.exporter.kafka;

import com.google.auto.service.AutoService;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.Collections;
import java.util.Map;

/** Makes the Datafox Kafka span exporter available to declarative SDK configuration. */
@AutoService(ComponentProvider.class)
public final class KafkaSpanExporterComponentProvider implements ComponentProvider {

  @Override
  public Class<SpanExporter> getType() {
    return SpanExporter.class;
  }

  @Override
  public String getName() {
    return "kafka";
  }

  @Override
  public SpanExporter create(DeclarativeConfigProperties config) {
    DeclarativeConfigProperties producer = config.getStructured("producer");
    Map<String, Object> properties =
        producer == null ? Collections.emptyMap() : DeclarativeConfigProperties.toMap(producer);
    return KafkaSpanExporterProvider.createExporter(
        config.getString("bootstrap_servers"), config.getString("topic"), properties);
  }
}
