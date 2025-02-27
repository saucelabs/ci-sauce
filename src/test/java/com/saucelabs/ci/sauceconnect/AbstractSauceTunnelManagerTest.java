package com.saucelabs.ci.sauceconnect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.Semaphore;

import org.junit.jupiter.api.Test;

/** Created by gavinmogan on 2016-07-13. */
class AbstractSauceTunnelManagerTest {

  @Test
  void testGetTunnelName() {
    assertEquals(
        "default",
        AbstractSauceTunnelManager.getTunnelName("basic -c", "default"),
        "missing parameter");
    assertEquals(
        "basic", AbstractSauceTunnelManager.getTunnelName("-i basic -c", "default"), "basic -i");
    assertEquals(
        "basic",
        AbstractSauceTunnelManager.getTunnelName("--tunnel-name basic -c", "default"),
        "basic --tunnel-name");
    assertEquals(
        "third",
        AbstractSauceTunnelManager.getTunnelName(
            "-i first --tunnel-name second -c -i third", "default"),
        "mix of -i and --tunnel-name still returns the last one");
  }

  @Test
  void testGetLogfile() {
    assertNull(AbstractSauceTunnelManager.getLogfile("basic -c"), "missing parameter");
    assertEquals("basic", AbstractSauceTunnelManager.getLogfile("-l basic -c"), "basic -l");
    assertEquals(
        "basic", AbstractSauceTunnelManager.getLogfile("--logfile basic -c"), "basic --logfile");
    assertEquals(
        "third",
        AbstractSauceTunnelManager.getLogfile("-l first --logfile second -c -l third"),
        "mix of -l and --logfile still returns the last one");
  }
}
