package com.saucelabs.ci.sauceconnect;

import com.saucelabs.saucerest.SauceREST;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

/**
 * Provides common logic for the invocation of Sauce Connect v3 and v4 processes.  The class maintains a cache of {@link Process } instances mapped against
 * the corresponding Sauce user which invoked Sauce Connect.
 *
 * @author Ross Rowe
 */
public abstract class AbstractSauceTunnelManager {

    /**
     * Logger instance.
     */
    protected static final java.util.logging.Logger julLogger = java.util.logging.Logger.getLogger(SauceConnectTwoManager.class.getName());

    /**
     * Should Sauce Connect output be suppressed?
     */
    protected boolean quietMode;

    /**
     * Maps the Sauce Connect process to the user which invoked it.
     */
    protected Map<String, Process> tunnelMap = new HashMap<String, Process>();

    protected Map<String, String> tunnelIdentifierMap = new HashMap<String, String>();
    /**
     * Lock which ensures thread safety for opening and closing tunnels.
     */
    protected Lock accessLock = new ReentrantLock();

    /**
     * Maps the number of invocations of Sauce Connect to the user which invoked it.
     */
    protected Map<String, Integer> processMap = new HashMap<String, Integer>();

    private SauceREST sauceRest;

    /**
     * Constructs a new instance.
     *
     * @param quietMode indicates whether Sauce Connect output should be suppressed
     */
    public AbstractSauceTunnelManager(boolean quietMode) {
        this.quietMode = quietMode;
    }

    /**
     * Closes the Sauce Connect process
     *
     * @param userName    name of the user which launched Sauce Connect
     * @param options     the command line options used to launch Sauce Connect
     * @param printStream the output stream to send log messages
     */
    public void closeTunnelsForPlan(String userName, String options, PrintStream printStream) {
        try {
            accessLock.lock();
            String identifier = getTunnelIdentifier(options, userName);
            if (tunnelMap.containsKey(identifier)) {
                Integer count = decrementProcessCountForUser(identifier, printStream);
                if (count == 0) {
                    //we can now close the process
                    final Process sauceConnect = tunnelMap.get(identifier);
                    closeSauceConnectProcess(printStream, sauceConnect);
                    tunnelMap.remove(identifier);
                    String tunnelId = tunnelIdentifierMap.remove(identifier);
                    if (tunnelId != null && sauceRest != null) {
                        //forcibly delete tunnel
                        sauceRest.deleteTunnel(tunnelId);
                    }
                    logMessage(printStream, "Sauce Connect stopped for: " + identifier);
                } else {
                    logMessage(printStream, "Jobs still running, not closing Sauce Connect");
                }
            }
        } finally {
            accessLock.unlock();
        }
    }

    private void closeSauceConnectProcess(PrintStream printStream, final Process sauceConnect) {
        logMessage(printStream, "Flushing Sauce Connect Input Stream");
        new Thread(new Runnable() {
            public void run() {
                try {
                    IOUtils.copy(sauceConnect.getInputStream(), new NullOutputStream());
                } catch (IOException e) {
                    //ignore
                }
            }
        }).start();
        logMessage(printStream, "Flushing Sauce Connect Error Stream");
        new Thread(new Runnable() {
            public void run() {
                try {
                    IOUtils.copy(sauceConnect.getErrorStream(), new NullOutputStream());
                } catch (IOException e) {
                    //ignore
                }
            }
        }).start();
        logMessage(printStream, "Closing Sauce Connect process");
        sauceConnect.destroy();
    }

    /**
     * Reduces the count of active Sauce Connect processes for the user by 1.
     *
     * @param identifier  the tunnel identifier
     * @param printStream the output stream to send log messages
     * @return current count of active Sauce Connect processes for the user
     */
    private Integer decrementProcessCountForUser(String identifier, PrintStream printStream) {
        Integer count = getProcessCountForUser(identifier) - 1;
        logMessage(printStream, "Decremented process count for " + identifier + ", now " + count);
        processMap.put(identifier, count);
        return count;
    }

    /**
     * Logs a message to the print stream (if not null), and to the logger instance for the class.
     *
     * @param printStream the output stream to send log messages
     * @param message     the message to be logged
     */
    protected void logMessage(PrintStream printStream, String message) {
        if (printStream != null) {
            printStream.println(message);
        }
        julLogger.log(Level.INFO, message);
    }

    /**
     * @param identifier the tunnel identifier
     * @return current count of active Sauce Connect processes for the user
     */
    protected Integer getProcessCountForUser(String identifier) {
        Integer count = processMap.get(identifier);
        if (count == null) {
            count = 0;
            processMap.put(identifier, count);
        }
        return count;
    }

    /**
     * @param identifier the tunnel identifier
     * @param tunnel     Sauce Connect process to add
     */
    public void addTunnelToMap(String identifier, Object tunnel) {
        //parse the options, if a tunnel identifier has been specified, use it as the key
        if (!tunnelMap.containsKey(identifier)) {
            tunnelMap.put(identifier, (Process) tunnel);
        }
    }

    /**
     * @param options      the command line options used to launch Sauce Connect
     * @param defaultValue
     * @return
     */
    public static String getTunnelIdentifier(String options, String defaultValue) {
        if (options != null && !options.equals("")) {
            String[] split = options.split(" ");
            for (int i = 0; i < split.length; i++) {
                String option = split[i];
                if (option.equals("-i") || option.equals("--tunnel-identifier")) {
                    //next option is identifier
                    return split[i + 1];
                }
            }
        }
        return defaultValue;
    }

    /**
     * Adds an element to an array
     *
     * @param original the original array
     * @param added    the element to add
     * @return a new array with the element added to the end
     */
    protected String[] addElement(String[] original, String added) {
        //split added on space
        String[] split = added.split(" ");
        String[] result = original;
        for (String arg : split) {
            String[] newResult = Arrays.copyOf(result, result.length + 1);
            newResult[result.length] = arg;
            result = newResult;
        }
        return result;
    }

    /**
     * Increases the number of Sauce Connect invocations for the user by 1.
     *
     * @param identifier  the tunnel identifier
     * @param printStream the output stream to send log messages
     */
    protected void incrementProcessCountForUser(String identifier, PrintStream printStream) {
        Integer count = getProcessCountForUser(identifier);
        count = count + 1;
        logMessage(printStream, "Incremented process count for " + identifier + ", now " + count);
        processMap.put(identifier, count);
    }

    /**
     * @param username        name of the user which launched Sauce Connect
     * @param apiKey          api key corresponding to the user
     * @param port            port which Sauce Connect should be launched on
     * @param sauceConnectJar File which contains the Sauce Connect executables (typically the CI plugin Jar file)
     * @param options         the command line options used to launch Sauce Connect
     * @param httpsProtocol   Value to be used for -Dhttps.protocol command line argument
     * @param printStream     the output stream to send log messages
     * @return new ProcessBuilder instance which will launch Sauce Connect
     * @throws SauceConnectException thrown if an error occurs launching the Sauce Connect process
     */
    protected abstract ProcessBuilder createProcessBuilder(String username, String apiKey, int port, File sauceConnectJar, String options, String httpsProtocol, PrintStream printStream) throws SauceConnectException;

    /**
     * Creates a new process to run Sauce Connect.
     *
     * @param username        the name of the Sauce OnDemand user
     * @param apiKey          the API Key for the Sauce OnDemand user
     * @param port            the port which Sauce Connect should be run on
     * @param sauceConnectJar the Jar file containing Sauce Connect.  If null, then we attempt to find Sauce Connect from the classpath
     * @param printStream     A print stream in which to redirect the output from Sauce Connect to.  Can be null
     * @return a {@link Process} instance which represents the Sauce Connect instance
     * @throws SauceConnectException thrown if an error occurs launching Sauce Connect
     */
    public Process openConnection(String username, String apiKey, int port, File sauceConnectJar, String options, String httpsProtocol, PrintStream printStream, Boolean verboseLogging) throws SauceConnectException {

        //ensure that only a single thread attempts to open a connection
        try {
            accessLock.lock();
            if (options == null) {
                options = "";
            }
            if (verboseLogging != null) {
                this.quietMode = !verboseLogging;
            }
            String identifier = getTunnelIdentifier(options, username);
            //do we have an instance for the tunnel identifier?
            if (getProcessCountForUser(identifier) != 0) {
                //if so, increment counter and return
                logMessage(printStream, "Sauce Connect already running for " + identifier);
                incrementProcessCountForUser(identifier, printStream);
                return tunnelMap.get(identifier);
            }
            ProcessBuilder processBuilder = createProcessBuilder(username, apiKey, port, sauceConnectJar, options, httpsProtocol, printStream);
            if (processBuilder == null) return null;


            final Process process = processBuilder.start();
            try {
                Semaphore semaphore = new Semaphore(1);
                semaphore.acquire();
                StreamGobbler errorGobbler = new SystemErrorGobbler("ErrorGobbler", process.getErrorStream(), printStream);
                errorGobbler.start();
                SystemOutGobbler outputGobbler = new SystemOutGobbler("OutputGobbler", process.getInputStream(), semaphore, printStream);
                outputGobbler.start();

                boolean sauceConnectStarted = semaphore.tryAcquire(3, TimeUnit.MINUTES);
                if (sauceConnectStarted) {
                    if (outputGobbler.isFailed()) {
                        String message = "Error launching Sauce Connect";
                        logMessage(printStream, message);
                        //ensure that Sauce Connect process is closed
                        closeSauceConnectProcess(printStream, process);
                        throw new SauceConnectDidNotStartException(message);
                    } else {
                        if (outputGobbler.getTunnelId() != null) {
                            tunnelIdentifierMap.put(identifier, outputGobbler.getTunnelId());
                            sauceRest = new SauceREST(username, apiKey);
                        }
                        logMessage(printStream, "Sauce Connect " + getCurrentVersion() + " now launched for: " + identifier);
                    }
                } else {
                    String message = "Time out while waiting for Sauce Connect to start, please check the Sauce Connect log";
                    logMessage(printStream, message);
                    //ensure that Sauce Connect process is closed
                    closeSauceConnectProcess(printStream, process);
                    throw new SauceConnectDidNotStartException(message);
                }
            } catch (InterruptedException e) {
                //continue;
                julLogger.log(Level.WARNING, "Exception occurred during invocation of Sauce Connect", e);
            }

            incrementProcessCountForUser(identifier, printStream);
            addTunnelToMap(identifier, process);
            return process;
        } catch (SauceConnectException e) {
            throw e;
        } catch (IOException e) {
            //thrown if an error occurs starting the process builder
            julLogger.log(Level.WARNING, "Exception occurred during invocation of Sauce Connect", e);
            throw new SauceConnectException(e);
        } finally {
            //release the access lock
            accessLock.unlock();
        }

    }

    protected abstract String getCurrentVersion();

    /**
     * Returns the arguments to be used to launch Sauce Connect
     *
     * @param args     the initial Sauce Connect command line args
     * @param username name of the user which launched Sauce Connect
     * @param apiKey   the access key for the Sauce user
     * @param port     the port that Sauce Connect should be launched on
     * @param options  command line args specified by the user
     * @return String array representing the command line args to be used to launch Sauce Connect
     */
    protected String[] generateSauceConnectArgs(String[] args, String username, String apiKey, int port, String options) {

        args = addElement(args, username);
        args = addElement(args, apiKey);
        args = addElement(args, "-P");
        args = addElement(args, String.valueOf(port));
        if (StringUtils.isNotBlank(options)) {
            args = addElement(args, options);
        }
        return args;
    }

    /**
     * @return the user's home directory
     */
    public String getSauceConnectWorkingDirectory() {
        return System.getProperty("user.home");
    }

    /**
     * Handles receiving and processing the output of an external process.
     */
    protected abstract class StreamGobbler extends Thread {
        private final PrintStream printStream;
        private final InputStream is;

        public StreamGobbler(String name, InputStream is, PrintStream printStream) {
            super(name);
            this.is = is;
            this.printStream = printStream;
        }

        /**
         * Opens a BufferedReader over the input stream, reads and processes each line.
         */
        public void run() {
            try {
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String line;
                while ((line = br.readLine()) != null) {
                    processLine(line);
                }
            } catch (IOException ioe) {
                //ignore stream closed errors
                if (!(ioe.getMessage().equalsIgnoreCase("stream closed"))) {
                    ioe.printStackTrace();
                }
            }
        }

        /**
         * Processes a line of output received by the stream gobbler.
         *
         * @param line line to process
         */
        protected void processLine(String line) {
            if (!quietMode) {
                if (printStream != null) {
                    printStream.println(line);
                }
                System.out.println(line);
                julLogger.info(line);
            }
        }
    }

    /**
     * Handles processing Sauce Connect output sent to stdout.
     */
    public class SystemOutGobbler extends StreamGobbler {

        private final Semaphore semaphore;

        private String tunnelId;
        private boolean failed;

        public SystemOutGobbler(String name, InputStream is, final Semaphore semaphore, PrintStream printStream) {
            super(name, is, printStream);
            this.semaphore = semaphore;
        }

        /**
         * {@inheritDoc}
         * <p/>
         * If the line contains the Sauce Connect started message, then release the semaphone, which will allow the
         * build to resume.
         *
         * @param line Line being processed
         */
        @Override
        protected void processLine(String line) {
            super.processLine(line);
            if (StringUtils.containsIgnoreCase(line, "Tunnel ID:")) {
                tunnelId = StringUtils.substringAfter(line, "Tunnel ID: ");
            }
            if (StringUtils.containsIgnoreCase(line, "Goodbye")) {
                failed = true;
            }
            if (StringUtils.containsIgnoreCase(line, getSauceStartedMessage()) || failed) {
                //unlock processMonitor
                semaphore.release();
            }
        }

        public String getTunnelId() {
            return tunnelId;
        }

        public boolean isFailed() {
            return failed;
        }
    }

    /**
     * @return Text which indicates that Sauce Connect has started
     */
    protected abstract String getSauceStartedMessage();

    /**
     * Handles processing Sauce Connect output sent to stderr.
     */
    public class SystemErrorGobbler extends StreamGobbler {

        public SystemErrorGobbler(String name, InputStream is, PrintStream printStream) {
            super(name, is, printStream);
        }
    }

    /**
     * Base exception class which is thrown if an error occurs launching Sauce Connect.
     */
    public static class SauceConnectException extends IOException {

        public SauceConnectException(String message) {
            super(message);
        }

        public SauceConnectException(Exception cause) {
            super(cause);
        }
    }

    /**
     * Exception which is thrown when Sauce Connect does not start within the timeout period.
     */
    public static class SauceConnectDidNotStartException extends SauceConnectException {
        public SauceConnectDidNotStartException(String message) {
            super(message);
        }
    }
}
