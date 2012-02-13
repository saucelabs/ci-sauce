package com.saucelabs.ci.sauceconnect;

import com.saucelabs.sauceconnect.SauceConnect;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
 * Handles opening a SSH Tunnel using the Sauce Connect 2 logic. The class  maintains a cache of {@link Process } instances mapped against
 * the corresponding plan key.  This class can be considered a singleton, and is instantiated via the 'component' element of the atlassian-plugin.xml
 * file (ie. using Spring).
 *
 * @author Ross Rowe
 */
public class SauceConnectTwoManager implements SauceTunnelManager {

    private static final Logger logger = Logger.getLogger(SauceConnectTwoManager.class);
    private Map<String, Process> tunnelMap = new HashMap<String, Process>();

    /**
     * Semaphore initialized with a single permit that is used to ensure that the main worker thread
     * waits until the Sauce Connect process is fully initialized before tests are run.
     */
    private final Semaphore semaphore = new Semaphore(1);

    /**
     * Lock which ensures thread safety for opening and closing tunnels.
     */
    private Lock accessLock = new ReentrantLock();
    private Map<String, Integer> processMap = new HashMap<String, Integer>();

    public SauceConnectTwoManager() {
    }

    public void closeTunnelsForPlan(String userName) {
        try {
            accessLock.lock();
            if (tunnelMap.containsKey(userName)) {
                Integer count = decrementProcessCountForUser(userName);
                if (count == 0) {
                    //we can now close the process
                    Process sauceConnect = tunnelMap.get(userName);
                    logger.info("Closing Sauce Connect");
                    closeStream(sauceConnect.getInputStream());
                    closeStream(sauceConnect.getOutputStream());
                    closeStream(sauceConnect.getErrorStream());
                    sauceConnect.destroy();
                    tunnelMap.remove(userName);
                }
            }
        } finally {
            accessLock.unlock();
        }
    }

    private Integer decrementProcessCountForUser(String userName) {
        Integer count = getProcessCountForUser(userName) - 1;
        processMap.put(userName, count);
        return count;
    }

    private void closeStream(OutputStream outputStream) {
        try {
            outputStream.close();
        } catch (IOException e) {
            logger.error("Error closing stream", e);
        }
    }

    private void closeStream(InputStream inputStream) {
        try {
            inputStream.close();
        } catch (IOException e) {
            logger.error("Error closing stream", e);
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
    public Object openConnection(String username, String apiKey, int port, File sauceConnectJar, PrintStream printStream) throws IOException {

        //ensure that only a single thread attempts to open a connection
        try {
            accessLock.lock();
            //do we have an instance for the user?
            if (getProcessCountForUser(username) != 0) {
                //if so, increment counter and return
                incrementProcessCountForUser(username);
                return tunnelMap.get(username);
            }
            //if not, start the process
            StringBuilder builder = new StringBuilder();
            if (sauceConnectJar != null && sauceConnectJar.exists()) {
                //copy the file to the user home, sauce connect fails to run when the jar is held in the temp directory
                File userHome = new File(System.getProperty("user.home"));
                File newJar = new File(userHome, "sauce-connect.jar");
                FileUtils.copyFile(sauceConnectJar, newJar);
                builder.append(newJar.getPath());
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
            String[] args = new String[]{path, "-cp",
                    builder.toString(),
                    SauceConnect.class.getName(),
                    username,
                    apiKey,
                    "-P",
                    String.valueOf(port)
            };

            ProcessBuilder processBuilder = new ProcessBuilder(args);
            if (logger.isInfoEnabled()) {
                logger.info("Launching Sauce Connect " + Arrays.toString(args));
            }
            if (printStream != null) {
                printStream.println("Launching Sauce Connect " + Arrays.toString(args));
            }
            final Process process = processBuilder.start();
            try {
                semaphore.acquire();
                StreamGobbler errorGobbler = new SystemErrorGobbler("ErrorGobbler", process.getErrorStream());
                errorGobbler.start();
                StreamGobbler outputGobbler = new SystemOutGobbler("OutputGobbler", process.getInputStream(), printStream);
                outputGobbler.start();

                boolean sauceConnectStarted = semaphore.tryAcquire(2, TimeUnit.MINUTES);
                if (!sauceConnectStarted) {
                    //log an error message
                    logger.error("Time out while waiting for Sauce Connect to start, attempting to continue");
                }
            } catch (InterruptedException e) {
                //continue;
            }
            logger.info("Sauce Connect now launched");
            incrementProcessCountForUser(username);
            return process;


        } catch (URISyntaxException e) {
            //shouldn't happen
            logger.error("Exception occured during retrieval of sauce connect jar URL", e);
        } finally {
            //release the semaphore when we're finished
            semaphore.release();
            //release the access lock
            accessLock.unlock();
        }

        return null;
    }

    private void incrementProcessCountForUser(String username) {
        Integer count = getProcessCountForUser(username);
        processMap.put(username, count + 1);
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
                ioe.printStackTrace();
            }
        }

        protected void processLine(String line) {
            getPrintStream().println(line);
            logger.info(line);
        }

        public abstract PrintStream getPrintStream();
    }

    private class SystemOutGobbler extends StreamGobbler {

        private PrintStream printStream;

        SystemOutGobbler(String name, InputStream is, PrintStream printStream) {
            super(name, is);
            this.printStream = printStream;
        }

        @Override
        public PrintStream getPrintStream() {
            if (printStream != null) {
                return printStream;
            } else {
                return System.out;
            }
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