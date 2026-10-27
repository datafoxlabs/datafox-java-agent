/*
 * Copyright Datafox Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.datafox.opentelemetry.resource;

import com.google.auto.service.AutoService;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.DeclarativeConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.DeclarativeConfigurationCustomizerProvider;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.ResourceModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.internal.ExperimentalResourceDetectionModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.internal.ExperimentalResourceDetectorModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.internal.ResourceModelAccessor;
import java.util.ArrayList;
import java.util.List;

/** Appends Datafox distribution metadata after upstream resource detectors. */
@AutoService(DeclarativeConfigurationCustomizerProvider.class)
public final class DatafoxDistroResourceCustomizerProvider
    implements DeclarativeConfigurationCustomizerProvider {

  @Override
  public void customize(DeclarativeConfigurationCustomizer customizer) {
    customizer.addModelCustomizer(
        model -> {
          ResourceModel resource = model.getResource();
          if (resource == null) {
            resource = new ResourceModel();
            model.setResource(resource);
          }
          ExperimentalResourceDetectionModel detection =
              ResourceModelAccessor.getDetection(resource);
          if (detection == null) {
            detection = new ExperimentalResourceDetectionModel();
          }
          List<ExperimentalResourceDetectorModel> detectors = detection.getDetectors();
          if (detectors == null) {
            detectors = new ArrayList<>();
            detection.setDetectors(detectors);
          }
          boolean present =
              detectors.stream()
                  .anyMatch(
                      detector ->
                          detector
                              .getAdditionalProperties()
                              .containsKey(DatafoxDistroResourceComponentProvider.NAME));
          if (!present) {
            ExperimentalResourceDetectorModel detector = new ExperimentalResourceDetectorModel();
            detector.getAdditionalProperties().put(DatafoxDistroResourceComponentProvider.NAME, null);
            detectors.add(detector);
          }
          ResourceModelAccessor.setDetection(resource, detection);
          return model;
        });
  }
}
