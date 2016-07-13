package com.saucelabs.ci.sauceconnect;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by gavinmogan on 2016-07-13.
 */
public class AbstractSauceTunnelManagerTest {

    @Test
    public void testGetTunnelIdentifier() throws Exception {
        assertEquals("missing parameter", "default", AbstractSauceTunnelManager.getTunnelIdentifier("basic -c", "default"));
        assertEquals("basic -i", "basic", AbstractSauceTunnelManager.getTunnelIdentifier("-i basic -c", "default"));
        assertEquals("basic --tunnel-identifier", "basic", AbstractSauceTunnelManager.getTunnelIdentifier("--tunnel-identifier basic -c", "default"));
        assertEquals("mix of -i and --tunnel-identifier still returns the last one", "third", AbstractSauceTunnelManager.getTunnelIdentifier("-i first --tunnel-identifier second -c -i third", "default"));


    }

    @Test
    public void testGetLogfile() throws Exception {
        assertNull("missing parameter", AbstractSauceTunnelManager.getLogfile("basic -c"));
        assertEquals("basic -l", "basic", AbstractSauceTunnelManager.getLogfile("-l basic -c"));
        assertEquals("basic --logfile", "basic", AbstractSauceTunnelManager.getLogfile("--logfile basic -c"));
        assertEquals("mix of -l and --logfile still returns the last one", "third", AbstractSauceTunnelManager.getLogfile("-l first --logfile second -c -l third"));

    }
}
