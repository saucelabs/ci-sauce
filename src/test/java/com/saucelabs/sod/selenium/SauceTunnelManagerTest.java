package com.saucelabs.sod.selenium;


import com.saucelabs.ci.sauceconnect.SauceConnectFourManager;
import com.saucelabs.ci.sauceconnect.SauceConnectTwoManager;
import com.saucelabs.ci.sauceconnect.SauceTunnelManager;
import com.saucelabs.common.SauceOnDemandAuthentication;
import com.saucelabs.selenium.client.factory.SeleniumFactory;
import com.saucelabs.sod.AbstractTestHelper;
import com.thoughtworks.selenium.Selenium;
import org.eclipse.jetty.server.Server;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import java.net.Authenticator;
import java.net.PasswordAuthentication;

import static org.junit.Assert.assertEquals;

/**
 * @author Ross Rowe
 */
public class SauceTunnelManagerTest extends AbstractTestHelper {
    private static Server server;

    @Before
    public void before() throws Exception {
        synchronized (this) {
            if (server == null) {
                server = startWebServer();
            }
        }
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (server != null) {
            server.stop();
        }
    }

    /**
     * Start a web server locally, set up an SSH tunnel, and have Sauce OnDemand connect to the local server.
     */
    @Test
    public void fullRun_SauceConnectTwoManager() throws Exception {
        fullRun(new SauceConnectTwoManager());
    }

    /**
     * Start a web server locally, set up an SSH tunnel, and have Sauce OnDemand connect to the local server.
     */
    @Test
    public void fullRun_SauceConnectFourManager() throws Exception {
        fullRun(new SauceConnectFourManager());
    }

    private void fullRun(SauceTunnelManager sauceTunnelManager) throws Exception {
        // start a tunnel
        System.out.println("Starting a tunnel");
        final SauceOnDemandAuthentication c = new SauceOnDemandAuthentication();
        Authenticator.setDefault(
            new Authenticator() {
                public PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(
                        c.getUsername(), c.getAccessKey().toCharArray());
                }
            }
        );

        long tStart = System.currentTimeMillis();
        sauceTunnelManager.openConnection(c.getUsername(), c.getAccessKey(), 4445, null, null, null, null, false);
        long tElapsed = System.currentTimeMillis() - tStart;
        System.out.println("tunnel opened in " + tElapsed + "ms");

        String driver = System.getenv("SELENIUM_DRIVER");
        if (driver == null || driver.equals("")) {
            System.setProperty("SELENIUM_DRIVER", DEFAULT_SAUCE_DRIVER);
        }

        String originalUrl = System.getenv("SELENIUM_STARTING_URL");
        System.setProperty("SELENIUM_STARTING_URL", "http://localhost:" + PORT + "/");

        tStart = System.currentTimeMillis();
        Selenium selenium = SeleniumFactory.create();
        tElapsed = System.currentTimeMillis() - tStart;
        System.out.println("SeleniumFactory.create() took " + tElapsed + "ms");

        try {
            tStart = System.currentTimeMillis();
            System.out.println("about to call selenium.start(): " + tStart);
            selenium.start();
            tElapsed = System.currentTimeMillis() - tStart;
            System.out.println("selenium.start() took: " + tElapsed);

            System.out.println("about to call selenium.open(): " + System.currentTimeMillis());
            selenium.open("/");
            System.out.println("called selenium.open(): " + System.currentTimeMillis());

            // if the server really hit our Jetty, we should see the same title that includes the secret code.
            assertEquals("test" + code, selenium.getTitle());
        } finally {
            sauceTunnelManager.closeTunnelsForPlan(c.getUsername(), null, null);
            selenium.stop();
            if (originalUrl != null && !originalUrl.equals("")) {
                System.setProperty("SELENIUM_STARTING_URL", originalUrl);
            }
        }
    }

}
