package com.saucelabs.ci.sauceconnect;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

/**
 * @author Ross Rowe
 */
public abstract class AbstractSauceTunnelManager {

    public static final String SAUCE_CONNECT_STARTED = "Connected! You may start your tests";

    protected static final java.util.logging.Logger julLogger = java.util.logging.Logger.getLogger(SauceConnectTwoManager.class.getName());
    protected final boolean quietMode;
    protected Map<String, Process> tunnelMap = new HashMap<String, Process>();
    /**
     * Lock which ensures thread safety for opening and closing tunnels.
     */
    protected Lock accessLock = new ReentrantLock();
    protected Map<String, Integer> processMap = new HashMap<String, Integer>();

    public AbstractSauceTunnelManager(boolean quietMode) {
        this.quietMode = quietMode;
    }

    public void closeTunnelsForPlan(String userName, String options, PrintStream printStream) {
        try {
            accessLock.lock();
            String identifier = getTunnelIdentifier(options, userName);
            if (tunnelMap.containsKey(identifier)) {
                Integer count = decrementProcessCountForUser(userName, printStream);
                if (count == 0) {
                    //we can now close the process
                    final Process sauceConnect = tunnelMap.get(identifier);
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
                    tunnelMap.remove(userName);
                } else {
                    logMessage(printStream, "Jobs still running, not closing Sauce Connect");
                }
            }
        } finally {
            accessLock.unlock();
        }
    }

    private Integer decrementProcessCountForUser(String userName, PrintStream printStream) {
        Integer count = getProcessCountForUser(userName) - 1;
        logMessage(printStream, "Decremented process count for " + userName + ", now " + count);
        processMap.put(userName, count);
        return count;
    }

    protected void logMessage(PrintStream printStream, String message) {
        if (printStream != null) {
            printStream.println(message);
        }
        julLogger.log(Level.INFO, message);
    }

    protected Integer getProcessCountForUser(String username) {
        Integer count = processMap.get(username);
        if (count == null) {
            count = 0;
            processMap.put(username, count);
        }
        return count;
    }

    private void closeStream(OutputStream outputStream) {
        try {
            outputStream.close();
        } catch (IOException e) {
            julLogger.log(Level.WARNING, "Error closing stream", e);
        }
    }

    private void closeStream(InputStream inputStream) {
        try {
            inputStream.close();
        } catch (IOException e) {
            julLogger.log(Level.WARNING, "Error closing stream", e);
        }
    }

    public void addTunnelToMap(String userName, String options, Object tunnel) {
        //parse the options, if a tunnel identifer has been specified, use it as the key
        String identifier = getTunnelIdentifier(options, userName);

        if (!tunnelMap.containsKey(identifier)) {
            tunnelMap.put(identifier, (Process) tunnel);
        }
    }

    private String getTunnelIdentifier(String options, String defaultValue) {
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

    protected void incrementProcessCountForUser(String username, PrintStream printStream) {
        Integer count = getProcessCountForUser(username);
        count = count + 1;
        logMessage(printStream, "Incremented process count for " + username + ", now " + count);
        processMap.put(username, count);
    }

    public Map getTunnelMap() {
        return tunnelMap;
    }

    protected abstract ProcessBuilder createProcessBuilder(String username, String apiKey, int port, File sauceConnectJar, String options, String httpsProtocol, PrintStream printStream) throws URISyntaxException, IOException;

    /**
     * Creates a new Java process to run the Sauce Connect 2 library.
     *
     * @param username        the name of the Sauce OnDemand user
     * @param apiKey          the API Key for the Sauce OnDemand user
     * @param port            the port which Sauce Connect should be run on
     * @param sauceConnectJar the Jar file containing Sauce Connect.  If null, then we attempt to find Sauce Connect from the classpath
     * @param printStream     A print stream in which to redirect the output from Sauce Connect to.  Can be null
     * @return a {@link Process} instance which represents the Sauce Connect instance
     * @throws java.io.IOException
     */
    //@Override
    public Process openConnection(String username, String apiKey, int port, File sauceConnectJar, String options, String httpsProtocol, PrintStream printStream) throws IOException {

        //ensure that only a single thread attempts to open a connection
        try {
            accessLock.lock();
            //do we have an instance for the user?
            if (getProcessCountForUser(username) != 0) {
                //if so, increment counter and return
                logMessage(printStream, "Sauce Connect already running for " + username);
                incrementProcessCountForUser(username, printStream);
                return tunnelMap.get(username);
            }
            ProcessBuilder processBuilder = createProcessBuilder(username, apiKey, port, sauceConnectJar, options, httpsProtocol, printStream);
            if (processBuilder == null) return null;


            final Process process = processBuilder.start();
            try {
                Semaphore semaphore = new Semaphore(1);
                semaphore.acquire();
                StreamGobbler errorGobbler = new SystemErrorGobbler("ErrorGobbler", process.getErrorStream());
                errorGobbler.start();
                StreamGobbler outputGobbler = new SystemOutGobbler("OutputGobbler", process.getInputStream(), semaphore);
                outputGobbler.start();

                boolean sauceConnectStarted = semaphore.tryAcquire(2, TimeUnit.MINUTES);
                if (!sauceConnectStarted) {
                    //log an error message
                    logMessage(printStream, "Time out while waiting for Sauce Connect to start, attempting to continue");
                }
            } catch (InterruptedException e) {
                //continue;
            }
            logMessage(printStream, "Sauce Connect now launched");
            incrementProcessCountForUser(username, printStream);
            addTunnelToMap(username, options, process);
            return process;


        } catch (URISyntaxException e) {
            //shouldn't happen
            julLogger.log(Level.WARNING, "Exception occured during retrieval of sauce connect jar URL", e);
        } finally {
            //release the access lock
            accessLock.unlock();
        }

        return null;
    }

    protected String[] generateSauceConnectArgs(String[] args, String username, String apiKey, int port, String options, String httpsProtocol, StringBuilder builder, String path) {

        args = addElement(args, username);
        args = addElement(args, apiKey);
        args = addElement(args, "-P");
        args = addElement(args, String.valueOf(port));
        if (StringUtils.isNotBlank(options)) {
            args = addElement(args, options);
        }
        return args;
    }

    public String getSauceConnectWorkingDirectory() {
        return System.getProperty("user.home");
    }

    protected abstract class StreamGobbler extends Thread {
        private InputStream is;

        private StreamGobbler(String name, InputStream is) {
            super(name);
            this.is = is;
        }

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

        protected void processLine(String line) {
            if (!quietMode) {
                getPrintStream().println(line);
                julLogger.info(line);
            }
        }

        public abstract PrintStream getPrintStream();
    }

    protected class SystemOutGobbler extends StreamGobbler {


        private final Semaphore semaphore;

        SystemOutGobbler(String name, InputStream is, final Semaphore semaphore) {
            super(name, is);
            this.semaphore = semaphore;
        }

        @Override
        public PrintStream getPrintStream() {
            return System.out;
        }

        @Override
        protected void processLine(String line) {
            super.processLine(line);
            if (StringUtils.containsIgnoreCase(line, getSauceStartedMessage())) {
                //unlock processMonitor
                semaphore.release();
            }
        }
    }

    protected String getSauceStartedMessage() {
        return SAUCE_CONNECT_STARTED;
    }

    protected class SystemErrorGobbler extends StreamGobbler {

        SystemErrorGobbler(String name, InputStream is) {
            super(name, is);
        }

        @Override
        public PrintStream getPrintStream() {
            return System.err;
        }
    }
}
