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
public interface SauceTunnelManager
{

    /**
     *
     * @param username
     * @param options
     * @param printStream
     */
    void closeTunnelsForPlan(String username, String options, PrintStream printStream);

    /**
     *
     * @param username
     * @param apiKey
     * @param port
     * @param sauceConnectJar
     * @param httpsProtocol
     * @param printStream
     * @return
     * @throws IOException
     */
    Process openConnection(String username, String apiKey, int port, File sauceConnectJar, String options, String httpsProtocol, PrintStream printStream, Boolean verboseLogging) throws IOException;

}
