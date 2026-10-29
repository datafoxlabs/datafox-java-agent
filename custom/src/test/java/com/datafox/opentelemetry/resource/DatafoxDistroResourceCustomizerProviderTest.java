/*
 * Copyright Datafox Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.datafox.opentelemetry.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.opentelemetry.sdk.autoconfigure.declarativeconfig.DeclarativeConfigurationBuilder;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.OpenTelemetryConfigurationModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.ResourceModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.internal.ExperimentalResourceDetectionModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.internal.ExperimentalResourceDetectorModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.internal.ResourceModelAccessor;
import java.util.ArrayList;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class DatafoxDistroResourceCustomizerProviderTest {

  @Test
  void addsDetectorOnlyOnceAndKeepsExistingDetectors() {
    ExperimentalResourceDetectorModel existing = new ExperimentalResourceDetectorModel();
    existing.getAdditionalProperties().put("existing_resource", null);
    OpenTelemetryConfigurationModel model =
        new OpenTelemetryConfigurationModel()
            .setResource(
                ResourceModelAccessor.setDetection(
                    new ResourceModel(),
                    new ExperimentalResourceDetectionModel()
                        .setDetectors(new ArrayList<>(Collections.singletonList(existing)))));

    DeclarativeConfigurationBuilder builder = new DeclarativeConfigurationBuilder();
    new DatafoxDistroResourceCustomizerProvider().customize(builder);
    builder.customizeModel(model);
    builder.customizeModel(model);

    ExperimentalResourceDetectionModel detection =
        ResourceModelAccessor.getDetection(model.getResource());
    assertNotNull(detection);
    assertEquals(2, detection.getDetectors().size());
    assertEquals("datafox_distro_version", detectorName(detection.getDetectors().get(0)));
    assertEquals("existing_resource", detectorName(detection.getDetectors().get(1)));
  }

  @Test
  void doesNotDuplicatePreexistingDatafoxDetector() {
    ExperimentalResourceDetectorModel datafox = new ExperimentalResourceDetectorModel();
    datafox.getAdditionalProperties().put("datafox_distro_version", null);
    OpenTelemetryConfigurationModel model =
        new OpenTelemetryConfigurationModel()
            .setResource(
                ResourceModelAccessor.setDetection(
                    new ResourceModel(),
                    new ExperimentalResourceDetectionModel()
                        .setDetectors(new ArrayList<>(Collections.singletonList(datafox)))));

    DeclarativeConfigurationBuilder builder = new DeclarativeConfigurationBuilder();
    new DatafoxDistroResourceCustomizerProvider().customize(builder);
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
