/*
 * Copyright Datafox Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.datafox.opentelemetry.resource;

import com.google.auto.service.AutoService;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.resources.Resource;

/** Provides Datafox distribution metadata in declarative SDK configuration. */
@AutoService(ComponentProvider.class)
public final class DatafoxDistroResourceComponentProvider implements ComponentProvider {
  static final String NAME = "datafox_distro_version";

  @Override
  public Class<Resource> getType() {
    return Resource.class;
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public Resource create(DeclarativeConfigProperties config) {
    return DatafoxDistroResourceProvider.distroResource();
  }
}
