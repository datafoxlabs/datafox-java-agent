/*
 * Copyright Datafox Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.datafox.opentelemetry.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.resources.Resource;
import java.util.ServiceLoader;
import org.junit.jupiter.api.Test;

class DatafoxDistroResourceComponentProviderTest {

  @Test
  void suppliesDistributionMetadataThroughDeclarativeSpi() {
    ComponentProvider provider = new DatafoxDistroResourceComponentProvider();
    assertEquals("datafox_distro_version", provider.getName());
    assertEquals(Resource.class, provider.getType());
    Resource resource = (Resource) provider.create(null);
    assertEquals("datafox", resource.getAttribute(AttributeKey.stringKey("telemetry.distro.name")));
    assertNotNull(resource.getAttribute(AttributeKey.stringKey("telemetry.distro.version")));
    boolean found = false;
    for (ComponentProvider loaded : ServiceLoader.load(ComponentProvider.class)) {
      if (loaded.getClass().equals(DatafoxDistroResourceComponentProvider.class)) {
        found = true;
      }
    }
    assertTrue(found);
  }
}
