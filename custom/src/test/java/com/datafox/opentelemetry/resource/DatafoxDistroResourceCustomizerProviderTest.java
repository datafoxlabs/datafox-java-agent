/*
 * Copyright Datafox Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.datafox.opentelemetry.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.opentelemetry.sdk.autoconfigure.declarativeconfig.DeclarativeConfigurationBuilder;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.OpenTelemetryConfigurationModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.internal.ExperimentalResourceDetectionModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.internal.ExperimentalResourceDetectorModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.internal.ResourceModelAccessor;
import org.junit.jupiter.api.Test;

class DatafoxDistroResourceCustomizerProviderTest {

  @Test
  void addsDetectorOnlyOnceAndKeepsExistingDetectors() {
    OpenTelemetryConfigurationModel model = new OpenTelemetryConfigurationModel();
    DeclarativeConfigurationBuilder builder = new DeclarativeConfigurationBuilder();
    new DatafoxDistroResourceCustomizerProvider().customize(builder);
    builder.customizeModel(model);
    builder.customizeModel(model);
    ExperimentalResourceDetectionModel detection =
        ResourceModelAccessor.getDetection(model.getResource());
    assertNotNull(detection);
    assertEquals(1, detection.getDetectors().size());
    assertEquals("datafox_distro_version", detectorName(detection.getDetectors().get(0)));
  }

  private static String detectorName(ExperimentalResourceDetectorModel detector) {
    return detector.getAdditionalProperties().keySet().iterator().next();
  }
}
