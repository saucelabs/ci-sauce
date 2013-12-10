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
 * Handles opening a SSH Tunnel using the Sauce Connect 2 logic. The class  maintains a cache of {@link Process } instances mapped against
 * the corresponding plan key.  This class can be considered a singleton, and is instantiated via the 'component' element of the atlassian-plugin.xml
 * file (ie. using Spring).
 *
 * @author Ross Rowe
 */
public class SauceConnectTwoManager implements SauceTunnelManager {

    private static final java.util.logging.Logger julLogger = java.util.logging.Logger.getLogger(SauceConnectTwoManager.class.getName());
    private static final String SAUCE_CONNECT_CLASS = "com.saucelabs.sauceconnect.SauceConnect";
    private final boolean quietMode;
    private Map<String, Process> tunnelMap = new HashMap<String, Process>();

    /**
     * Lock which ensures thread safety for opening and closing tunnels.
     */
    private Lock accessLock = new ReentrantLock();

    private Map<String, Integer> processMap = new HashMap<String, Integer>();

    public SauceConnectTwoManager() {
        this(false);
    }

    public SauceConnectTwoManager(boolean quietMode) {
        this.quietMode = quietMode;
    }

    public void closeTunnelsForPlan(String userName, PrintStream printStream) {
        try {
            accessLock.lock();
            if (tunnelMap.containsKey(userName)) {
                Integer count = decrementProcessCountForUser(userName, printStream);
                if (count == 0) {
                    //we can now close the process
                    final Process sauceConnect = tunnelMap.get(userName);
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

    private void logMessage(PrintStream printStream, String message) {
        if (printStream != null) {
            printStream.println(message);
        }
        julLogger.log(Level.INFO, message);
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

    public void addTunnelToMap(String userName, Object tunnel) {
        if (!tunnelMap.containsKey(userName)) {
            tunnelMap.put(userName, (Process) tunnel);
        }
    }

    /**
     * Creates a new Java process to run the Sauce Connect 2 library.
     *
     * @param username        the name of the Sauce OnDemand user
     * @param apiKey          the API Key for the Sauce OnDemand user
     * @param port            the port which Sauce Connect should be run on
     * @param sauceConnectJar the Jar file containing Sauce Connect.  If null, then we attempt to find Sauce Connect from the classpath
     * @param printStream     A print stream in which to redirect the output from Sauce Connect to.  Can be null
     * @return a {@link Process} instance which represents the Sauce Connect instance
     * @throws IOException
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
                        username,
                        apiKey,
                        "-P",
                        String.valueOf(port),
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

            if (StringUtils.isNotBlank(options)) {
                args = addElement(args, options);
            }

            ProcessBuilder processBuilder = new ProcessBuilder(args);
            if (workingDirectory == null) {
                workingDirectory = new File(getSauceConnectWorkingDirectory());
            }
            processBuilder.directory(workingDirectory);
            julLogger.log(Level.INFO, "Launching Sauce Connect " + Arrays.toString(args));

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
            addTunnelToMap(username, process);
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

    private String[] addElement(String[] original, String added) {
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

    public String getSauceConnectWorkingDirectory() {
        return System.getProperty("user.home");
    }

    private void incrementProcessCountForUser(String username, PrintStream printStream) {
        Integer count = getProcessCountForUser(username);
        count = count + 1;
        logMessage(printStream, "Incremented process count for " + username + ", now" + count);
        processMap.put(username, count);
    }

    private Integer getProcessCountForUser(String username) {
        Integer count = processMap.get(username);
        if (count == null) {
            count = 0;
            processMap.put(username, count);
        }
        return count;
    }

    public Map getTunnelMap() {
        return tunnelMap;
    }

    private abstract class StreamGobbler extends Thread {
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

    private class SystemOutGobbler extends StreamGobbler {

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
            if (StringUtils.containsIgnoreCase(line, "Connected! You may start your tests")) {
                //unlock processMonitor
                semaphore.release();
            }
        }
    }

    private class SystemErrorGobbler extends StreamGobbler {

        SystemErrorGobbler(String name, InputStream is) {
            super(name, is);
        }

        @Override
        public PrintStream getPrintStream() {
            return System.err;
        }
    }
}
