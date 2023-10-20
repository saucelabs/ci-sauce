package com.saucelabs.ci.sauceconnect;

import org.junit.Test;

import java.util.concurrent.Semaphore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/** Created by gavinmogan on 2016-07-13. */
public class AbstractSauceTunnelManagerTest {

  @Test
  public void testGetTunnelName() {
    assertEquals(
        "missing parameter",
        "default",
        AbstractSauceTunnelManager.getTunnelName("basic -c", "default"));
    assertEquals(
        "basic -i", "basic", AbstractSauceTunnelManager.getTunnelName("-i basic -c", "default"));
    assertEquals(
        "basic --tunnel-name",
        "basic",
        AbstractSauceTunnelManager.getTunnelName("--tunnel-name basic -c", "default"));
    assertEquals(
        "old --tunnel-identifier",
        "basic",
        AbstractSauceTunnelManager.getTunnelName("--tunnel-identifier basic -c", "default"));
    assertEquals(
        "mix of -i and --tunnel-name still returns the last one",
        "third",
        AbstractSauceTunnelManager.getTunnelName(
            "-i first --tunnel-name second -c -i third", "default"));
  }

  @Test
  public void testGetLogfile() {
    assertNull("missing parameter", AbstractSauceTunnelManager.getLogfile("basic -c"));
    assertEquals("basic -l", "basic", AbstractSauceTunnelManager.getLogfile("-l basic -c"));
    assertEquals(
        "basic --logfile", "basic", AbstractSauceTunnelManager.getLogfile("--logfile basic -c"));
    assertEquals(
        "mix of -l and --logfile still returns the last one",
        "third",
        AbstractSauceTunnelManager.getLogfile("-l first --logfile second -c -l third"));
  }

  @Test
  public void testSystemOutGobbler_ProcessLine() {
    Semaphore semaphore = new Semaphore(1);
    SauceConnectFourManager man = new SauceConnectFourManager(true);
    AbstractSauceTunnelManager.SystemOutGobbler sot = man.makeOutputGobbler(null, null, semaphore);
    sot.processLine("Provisioned tunnel:tunnelId1");
    assertEquals(sot.getTunnelId(), "tunnelId1");
    sot.processLine("Provisioned tunnel:    tunnelId2    ");
    assertEquals(sot.getTunnelId(), "tunnelId2");
    sot.processLine("Provisioned tunnel:    tunnelId2    ");
    assertEquals(sot.getTunnelId(), "tunnelId2");
  }
}
