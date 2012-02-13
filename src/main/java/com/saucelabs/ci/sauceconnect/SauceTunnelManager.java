package com.saucelabs.ci.sauceconnect;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;

/**
 * Interface which defines the behaviour for Sauce Connect Tunnel implementations.
 *
 * @author <a href="http://www.sysbliss.com">Jonathan Doklovic</a>
 * @author Ross Rowe
 */
public interface SauceTunnelManager
{

    public void closeTunnelsForPlan(String username);

    public void addTunnelToMap(String userName, Object tunnel);

    Object openConnection(String username, String apiKey, int port, File sauceConnectJar, PrintStream printStream) throws IOException;

    Map getTunnelMap();

}
