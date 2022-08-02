package com.saucelabs.ci.sauceconnect;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Interface which defines the behaviour for Sauce Connect Tunnel implementations.
 *
 * @author <a href="http://www.sysbliss.com">Jonathan Doklovic</a>
 * @author Ross Rowe
 */
public interface SauceTunnelManager {

    /**
     * Closes the Sauce Connect process
     *
     * @param username    name of the user which launched Sauce Connect
     * @param options     the command line options used to launch Sauce Connect
     * @param printStream the output stream to send log messages
     */
    void closeTunnelsForPlan(String username, String options, PrintStream printStream);

    /**
     * Creates a new process to run Sauce Connect in the US Data Center
     *
     * @param username         the name of the Sauce OnDemand user
     * @param apiKey           the API Key for the Sauce OnDemand user
     * @param port             the port which Sauce Connect should be run on
     * @param sauceConnectJar  the Jar file containing Sauce Connect.  If null, then we attempt to find Sauce Connect from the classpath (only used by SauceConnectTwoManager)
     * @param options          the command line options to pass to Sauce Connect
     * @param printStream      A print stream in which to redirect the output from Sauce Connect to.  Can be null
     * @param verboseLogging   indicates whether verbose logging should be output
     * @param sauceConnectPath if defined, Sauce Connect will be launched from the specified path and won't be extracted from the jar file
     * @return a {@link Process} instance which represents the Sauce Connect instance
     * @throws IOException thrown if an error occurs launching Sauce Connect
     */
    Process openConnection(String username, String apiKey, int port, File sauceConnectJar, String options, PrintStream printStream, Boolean verboseLogging, String sauceConnectPath) throws IOException;

    /**
     * Creates a new process to run Sauce Connect.
     *
     * @param username         the name of the Sauce OnDemand user
     * @param apiKey           the API Key for the Sauce OnDemand user
     * @param dataCenter       the Sauce Labs Data Center name (US, EU, US_EAST)
     * @param port             the port which Sauce Connect should be run on
     * @param sauceConnectJar  the Jar file containing Sauce Connect.  If null, then we attempt to find Sauce Connect from the classpath (only used by SauceConnectTwoManager)
     * @param options          the command line options to pass to Sauce Connect
     * @param printStream      A print stream in which to redirect the output from Sauce Connect to.  Can be null
     * @param verboseLogging   indicates whether verbose logging should be output
     * @param sauceConnectPath if defined, Sauce Connect will be launched from the specified path and won't be extracted from the jar file
     * @return a {@link Process} instance which represents the Sauce Connect instance
     * @throws IOException thrown if an error occurs launching Sauce Connect
     */
    Process openConnection(String username, String apiKey, String dataCenter, int port, File sauceConnectJar, String options, PrintStream printStream, Boolean verboseLogging, String sauceConnectPath) throws IOException;

}
