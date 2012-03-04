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

    void closeTunnelsForPlan(String username, PrintStream printStream);

    Process openConnection(String username, String apiKey, int port, File sauceConnectJar, PrintStream printStream) throws IOException;

}
