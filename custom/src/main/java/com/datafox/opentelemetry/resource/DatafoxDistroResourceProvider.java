/*
 * Copyright Datafox Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.datafox.opentelemetry.resource;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.resources.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** Adds the Datafox distribution name and version to the telemetry resource. */
public final class DatafoxDistroResourceProvider implements ResourceProvider {

  private static final Resource DISTRO_RESOURCE = createDistroResource();

  @Override
  public Resource createResource(ConfigProperties config) {
    return DISTRO_RESOURCE;
  }

  @Override
  public int order() {
    return Integer.MAX_VALUE - 1;
  }

  private static Resource createDistroResource() {
    try (InputStream stream =
        DatafoxDistroResourceProvider.class.getClassLoader().getResourceAsStream("datafox.properties")) {
      if (stream == null) {
        return Resource.empty();
      }

      Properties properties = new Properties();
      properties.load(stream);
      return Resource.create(
          Attributes.of(
              AttributeKey.stringKey("telemetry.distro.name"),
              "datafox",
              AttributeKey.stringKey("telemetry.distro.version"),
              properties.getProperty("telemetry.distro.version")));
    } catch (IOException e) {
      return Resource.empty();
    }
  }
}
