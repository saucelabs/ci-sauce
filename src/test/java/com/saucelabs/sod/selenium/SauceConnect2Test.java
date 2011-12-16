package com.saucelabs.bamboo.sod.selenium;

import com.saucelabs.bamboo.sod.AbstractTestHelper;
import com.saucelabs.ci.sauceconnect.SauceConnectTwoManager;
import com.saucelabs.ci.sauceconnect.SauceTunnelManager;
import com.saucelabs.rest.Credential;

import com.saucelabs.selenium.client.factory.SeleniumFactory;
import com.thoughtworks.selenium.Selenium;
import org.eclipse.jetty.server.Server;

import org.junit.Test;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * @author Ross Rowe
 */
public class SauceConnect2Test extends AbstractTestHelper {
    private Server server;

    /**
     * Start a web server locally, set up an SSH tunnel, and have Sauce OnDemand connect to the local server.
     */
    @Test
    //@Ignore("Test should pass okay, however invoking Sauce Connect 2 library requires the bamboo_sauce jar file to be on the classpath")
    public void fullRun() throws Exception {
        Server server = startWebServer();

        try {
            // start a tunnel
            System.out.println("Starting a tunnel");
            final Credential c = new Credential();
            Authenticator.setDefault(
                    new Authenticator() {
                        public PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(
                                    c.getUsername(), c.getKey().toCharArray());
                        }
                    }
            );            
            SauceTunnelManager sauceTunnelManager = new SauceConnectTwoManager();
            Process sauceConnect = (Process) sauceTunnelManager.openConnection(c.getUsername(), c.getKey());
            sauceTunnelManager.addTunnelToMap("TEST", sauceConnect);
            System.out.println("tunnel established");
            String driver = System.getenv("SELENIUM_DRIVER");
            if (driver == null || driver.equals("")) {
                System.setProperty("SELENIUM_DRIVER", DEFAULT_SAUCE_DRIVER);
            }

            String originalUrl = System.getenv("SELENIUM_STARTING_URL");
            System.setProperty("SELENIUM_STARTING_URL", "http://localhost:8080/");
            Selenium selenium = SeleniumFactory.create();            
            try {
                selenium.start();
                selenium.open("/");
                // if the server really hit our Jetty, we should see the same title that includes the secret code.
                assertEquals("test" + code, selenium.getTitle());                
            } finally {
                sauceTunnelManager.closeTunnelsForPlan("TEST");
                selenium.stop();
                if (originalUrl != null && !originalUrl.equals("")) {
                     System.setProperty("SELENIUM_STARTING_URL", originalUrl);
                }
            }
        } finally {
            server.stop();
        }
    }

}
