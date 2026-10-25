/*
 * Copyright Datafox Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.datafox.opentelemetry.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.resources.Resource;
import org.junit.jupiter.api.Test;

class DatafoxDistroResourceProviderTest {

  @Test
  void addsDistributionNameAndVersion() {
    Resource resource = new DatafoxDistroResourceProvider().createResource(null);

    assertEquals("datafox", resource.getAttribute(AttributeKey.stringKey("telemetry.distro.name")));
    assertEquals(
        "0.1.0-SNAPSHOT",
        resource.getAttribute(AttributeKey.stringKey("telemetry.distro.version")));
  }
}
