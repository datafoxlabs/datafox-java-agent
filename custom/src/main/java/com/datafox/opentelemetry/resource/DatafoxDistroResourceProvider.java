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

/** Adds the Datafox distribution name to the telemetry resource. */
public final class DatafoxDistroResourceProvider implements ResourceProvider {

  private static final Resource DISTRO_RESOURCE =
      Resource.create(Attributes.of(AttributeKey.stringKey("telemetry.distro.name"), "datafox"));

  @Override
  public Resource createResource(ConfigProperties config) {
    return DISTRO_RESOURCE;
  }
}
