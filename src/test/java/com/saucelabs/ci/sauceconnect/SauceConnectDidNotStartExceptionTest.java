package com.saucelabs.ci.sauceconnect;

import static org.junit.Assert.*;

import org.junit.Test;

public class SauceConnectDidNotStartExceptionTest {

    @Test
    public void shouldPropagateConstructionParams(){
        String message = "an error occurred " + System.currentTimeMillis();
        assertEquals(message, new AbstractSauceTunnelManager.SauceConnectDidNotStartException(message).getMessage());
    }

}