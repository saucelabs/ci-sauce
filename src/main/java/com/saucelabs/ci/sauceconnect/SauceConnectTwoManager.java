package com.saucelabs.ci.sauceconnect;

import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.logging.Level;


/**
 * Handles opening a SSH Tunnel using the Sauce Connect 2 logic. The class  maintains a cache of {@link Process } instances mapped against
 * the corresponding plan key.  This class can be considered a singleton, and is instantiated via the 'component' element of the atlassian-plugin.xml
 * file (ie. using Spring).
 *
 * @author Ross Rowe
 */
public class SauceConnectTwoManager extends AbstractSauceTunnelManager implements SauceTunnelManager {

    private static final String SAUCE_CONNECT_CLASS = "com.saucelabs.sauceconnect.SauceConnect";

    public SauceConnectTwoManager() {
        this(false);
    }

    public SauceConnectTwoManager(boolean quietMode) {
        super(quietMode);
    }

    @Override
    protected ProcessBuilder createProcessBuilder(String username, String apiKey, int port, File sauceConnectJar, String options, String httpsProtocol, PrintStream printStream) throws URISyntaxException, IOException {
        //if not, start the process
        File workingDirectory = null;
        StringBuilder builder = new StringBuilder();
        if (sauceConnectJar != null && sauceConnectJar.exists()) {
            builder.append(sauceConnectJar.getPath());
            workingDirectory = sauceConnectJar.getParentFile();
        } else {
            File jarFile = SauceConnectUtils.extractSauceConnectJarFile();
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
        args = generateSauceConnectArgs(args, username, apiKey, port, options, httpsProtocol, builder, path);

        ProcessBuilder processBuilder = new ProcessBuilder(args);
        if (workingDirectory == null) {
            workingDirectory = new File(getSauceConnectWorkingDirectory());
        }
        processBuilder.directory(workingDirectory);
        julLogger.log(Level.INFO, "Launching Sauce Connect " + Arrays.toString(args));
        return processBuilder;
    }

}
