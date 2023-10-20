package com.saucelabs.ci.sauceconnect;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SauceConnectDidNotStartExceptionTest {

  @Test
  public void shouldPropagateConstructionParams() {
    String message = "an error occurred " + System.currentTimeMillis();
    assertEquals(
        message,
        new AbstractSauceTunnelManager.SauceConnectDidNotStartException(message).getMessage());
  }
}
