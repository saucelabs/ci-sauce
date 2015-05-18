package com.saucelabs.ci.sauceconnect;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;


/**
 * Handles opening a SSH Tunnel using the Sauce Connect v3 (Java based).
 *
 * @author Ross Rowe
 * @deprecated Sauce Connect v3 is in the process of being EOLd
 */
public class SauceConnectTwoManager extends AbstractSauceTunnelManager implements SauceTunnelManager {

    private static final String SAUCE_CONNECT_STARTED = "Connected! You may start your tests";
    private static final String CURRENT_SC_VERSION = "3.1.26";

    /**
     * Constructs an instance with quiet mode disabled.
     */
    public SauceConnectTwoManager() {
        this(false);
    }

    /**
     * Constructs a new instance.
     *
     * @param quietMode indicates whether Sauce Connect output should be suppressed
     */
    public SauceConnectTwoManager(boolean quietMode) {
        super(quietMode);
    }

    /**
     * @param username         name of the user which launched Sauce Connect
     * @param apiKey           api key corresponding to the user
     * @param port             port which Sauce Connect should be launched on
     * @param sauceConnectJar  File which contains the Sauce Connect executables (typically the CI plugin Jar file)
     * @param options          the command line options used to launch Sauce Connect
     * @param httpsProtocol    Value to be used for -Dhttps.protocol command line argument
     * @param printStream      the output stream to send log messages
     * @param sauceConnectPath
     * @return
     * @throws URISyntaxException thrown if an error occurs extracting the Sauce Connect jar file from the plugin jar file
     * @throws IOException
     */
    @Override
    protected ProcessBuilder createProcessBuilder(String username, String apiKey, int port, File sauceConnectJar, String options, String httpsProtocol, PrintStream printStream, String sauceConnectPath) throws SauceConnectException {
        throw new SauceConnectException("Sauce Connect 3 is not supported, please use Sauce Connect 4");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getSauceStartedMessage() {
        return SAUCE_CONNECT_STARTED;
    }

    @Override
    protected String getCurrentVersion() {
        return CURRENT_SC_VERSION;
    }
}
