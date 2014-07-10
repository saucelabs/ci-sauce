package com.saucelabs.ci.sauceconnect;

import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.logging.Level;


/**
 * Handles opening a SSH Tunnel using the Sauce Connect v3 (Java based).
 *
 * @author Ross Rowe
 */
public class SauceConnectTwoManager extends AbstractSauceTunnelManager implements SauceTunnelManager {

    private static final String SAUCE_CONNECT_CLASS = "com.saucelabs.sauceconnect.SauceConnect";

    private static final String SAUCE_CONNECT_STARTED = "Connected! You may start your tests";

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
     * @param username        name of the user which launched Sauce Connect
     * @param apiKey          api key corresponding to the user
     * @param port            port which Sauce Connect should be launched on
     * @param sauceConnectJar File which contains the Sauce Connect executables (typically the CI plugin Jar file)
     * @param options         the command line options used to launch Sauce Connect
     * @param httpsProtocol   Value to be used for -Dhttps.protocol command line argument
     * @param printStream     the output stream to send log messages
     * @return
     * @throws URISyntaxException thrown if an error occurs extracting the Sauce Connect jar file from the plugin jar file
     * @throws IOException
     */
    @Override
    protected ProcessBuilder createProcessBuilder(String username, String apiKey, int port, File sauceConnectJar, String options, String httpsProtocol, PrintStream printStream) throws SauceConnectException {
        //if not, start the process
        File workingDirectory = null;
        StringBuilder builder = new StringBuilder();
        if (sauceConnectJar != null && sauceConnectJar.exists()) {
            builder.append(sauceConnectJar.getPath());
            workingDirectory = sauceConnectJar.getParentFile();
        } else {
            File jarFile;
            try {
                jarFile = SauceConnectUtils.extractSauceConnectJarFile();
            } catch (URISyntaxException e) {
                throw new SauceConnectException(e);
            } catch (IOException e) {
                throw new SauceConnectException(e);
            }
            if (jarFile == null) {
                if (printStream != null) {
                    printStream.print("Unable to find sauce connect jar");
                    return null;
                }
            } else {
                builder.append(jarFile.getPath());
            }
        }

        String fileSeparator = File.separator;
        String path = System.getProperty("java.home")
                + fileSeparator + "bin" + fileSeparator + "java";
        String[] args;
        if (StringUtils.isBlank(httpsProtocol)) {
            args = new String[]{path, "-cp",
                    builder.toString(),
                    SAUCE_CONNECT_CLASS,

            };
        } else {
            args = new String[]{path, "-Dhttps.protocols=" + httpsProtocol, "-cp",
                    builder.toString(),
                    SAUCE_CONNECT_CLASS,
                    username,
                    apiKey,
                    "-P",
                    String.valueOf(port)
            };
        }
        args = generateSauceConnectArgs(args, username, apiKey, port, options);

        ProcessBuilder processBuilder = new ProcessBuilder(args);
        if (workingDirectory == null) {
            workingDirectory = new File(getSauceConnectWorkingDirectory());
        }
        processBuilder.directory(workingDirectory);
        julLogger.log(Level.INFO, "Launching Sauce Connect " + Arrays.toString(args));
        return processBuilder;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getSauceStartedMessage() {
        return SAUCE_CONNECT_STARTED;
    }
}
