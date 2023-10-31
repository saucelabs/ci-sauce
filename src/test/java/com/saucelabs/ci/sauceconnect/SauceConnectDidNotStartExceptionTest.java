package com.saucelabs.ci.sauceconnect;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SauceConnectDidNotStartExceptionTest {

  @Test
  void shouldPropagateConstructionParams() {
    String message = "an error occurred " + System.currentTimeMillis();
    assertEquals(
        message,
        new AbstractSauceTunnelManager.SauceConnectDidNotStartException(message).getMessage());
  }
}
