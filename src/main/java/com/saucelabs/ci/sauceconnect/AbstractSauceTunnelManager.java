package com.saucelabs.ci.sauceconnect;

import com.saucelabs.saucerest.DataCenter;
import com.saucelabs.saucerest.SauceException;
import com.saucelabs.saucerest.SauceREST;
import com.saucelabs.saucerest.api.SauceConnectEndpoint;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Provides common logic for the invocation of Sauce Connect processes. The class
 * maintains a cache of {@link Process } instances mapped against the corresponding Sauce user which
 * invoked Sauce Connect.
 *
 * @author Ross Rowe
 */
public abstract class AbstractSauceTunnelManager implements SauceTunnelManager {
  private static final Duration READINESS_CHECK_TIMEOUT = Duration.ofSeconds(15);
  private static final Duration READINESS_CHECK_POLLING_INTERVAL = Duration.ofSeconds(3);

  protected Logger logger;

  /** Should Sauce Connect output be suppressed? */
  protected boolean quietMode;

  protected Map<String, TunnelInformation> tunnelInformationMap = new ConcurrentHashMap<>();

  /** Contains all the Sauce Connect {@link Process} instances that have been launched. */
  private Map<String, List<Process>> openedProcesses = new HashMap<>();

  private SauceREST sauceRest;
  private SauceConnectEndpoint scEndpoint;
  private SCMonitor scMonitor;
  private ProcessOutputPrinter processOutputPrinter = new DefaultProcessOutputPrinter();

  private AtomicInteger launchAttempts = new AtomicInteger(0);

  /**
   * Constructs a new instance.
   *
   * @param quietMode indicates whether Sauce Connect output should be suppressed
   */
  public AbstractSauceTunnelManager(boolean quietMode, Logger logger) {
    this.quietMode = quietMode;
    this.logger = logger;
  }

  public AbstractSauceTunnelManager(boolean quietMode) {
    this(quietMode, LoggerFactory.getLogger(AbstractSauceTunnelManager.class));
  }

  /**
   * @param options the command line options used to launch Sauce Connect
   * @param defaultValue the default value to use for the tunnel name if none specified in the
   *     options
   * @return String representing the tunnel name
   */
  public static String getTunnelName(String options, String defaultValue) {
    if (options == null || options.isEmpty()) {
      return defaultValue;
    }

    String name = null;
    String[] split = options.split(" ");
    for (int i = 0; i < split.length; i++) {
      String option = split[i];
      if (option.equals("-i")
          || option.equals("--tunnel-name")) {
        // next option is name
        name = split[i + 1];
      }
    }
    if (name != null) {
      return name;
    }

    return defaultValue;
  }

  /**
   * @param options the command line options used to launch Sauce Connect
   * @return String representing the logfile location
   */
  public static String getLogfile(String options) {
    if (options == null || options.isEmpty()) {
      return null;
    }
    String logFile = null;
    String[] split = options.split(" ");
    for (int i = 0; i < split.length; i++) {
      String option = split[i];
      if (option.equals("-l") || option.equals("--logfile")) {
        // next option is logfile
        logFile = split[i + 1];
      }
    }
    return logFile;
  }

  public void setSauceRest(SauceREST sauceRest) {
    this.sauceRest = sauceRest;
    this.scEndpoint = sauceRest.getSauceConnectEndpoint();
  }

  public void setSCMonitor(SCMonitor scMonitor) {
    this.scMonitor = scMonitor;
  }

  public void setProcessOutputPrinter(ProcessOutputPrinter processOutputPrinter) {
    this.processOutputPrinter = processOutputPrinter;
  }

  /**
   * Closes the Sauce Connect process
   *
   * @param userName name of the user which launched Sauce Connect
   * @param options the command line options used to launch Sauce Connect
   * @param printStream the output stream to send log messages
   */
  public void closeTunnelsForPlan(String userName, String options, PrintStream printStream) {
    String tunnelName = getTunnelName(options, userName);
    TunnelInformation tunnelInformation = getTunnelInformation(tunnelName);
    if (tunnelInformation == null) {
      return;
    }
    try {
      tunnelInformation.getLock().lock();
      int count = decrementProcessCountForUser(tunnelInformation, printStream);
      if (count == 0) {
        // we can now close the process
        final Process sauceConnect = tunnelInformation.getProcess();
        closeSauceConnectProcess(printStream, sauceConnect);
        String tunnelId = tunnelInformation.getTunnelId();
        if (tunnelId != null && scEndpoint != null) {
          logMessage(printStream, "Stopping Sauce Connect tunnel: " + tunnelId);
          // forcibly delete tunnel
          try {
            scEndpoint.stopTunnel(tunnelId);
            logMessage(printStream, "Deleted tunnel");
          } catch (java.io.IOException | SauceException.UnknownError e) {
            logMessage(printStream, "Error during tunnel removal: " + e);
          } catch (SauceException.NotFound e) {
            // Tunnel has already been cleaned up, no need to do anything
          } catch (NullPointerException e) {
            logMessage(printStream, "Error connecting to REST API: " + e);
          }
        }
        tunnelInformationMap.remove(tunnelName);
        List<Process> processes = openedProcesses.get(tunnelName);
        if (processes != null) {
          processes.remove(sauceConnect);
        }
        logMessage(printStream, "Sauce Connect stopped for: " + tunnelName);
      } else {
        logMessage(printStream, "Jobs still running, not closing Sauce Connect");
      }

    } finally {
      tunnelInformation.getLock().unlock();
    }
  }

  private void closeSauceConnectProcess(PrintStream printStream, final Process sauceConnect) {
    logMessage(printStream, "Flushing Sauce Connect Input Stream");
    new Thread(() -> flushInputStream(sauceConnect.getInputStream())).start();
    logMessage(printStream, "Flushing Sauce Connect Error Stream");
    new Thread(() -> flushInputStream(sauceConnect.getErrorStream())).start();
    logMessage(printStream, "Closing Sauce Connect process");
    sauceConnect.destroy();
    try {
      if (sauceConnect.waitFor(30, TimeUnit.SECONDS)) {
        logMessage(printStream, "Sauce Connect process has exited");
      } else {
        logMessage(printStream, "Sauce Connect process has not exited during 30 seconds");
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private static void flushInputStream(InputStream inputStream) {
    try {
        inputStream.skip(inputStream.available());
    } catch (IOException e) {
        // ignore
    }
  }

  /**
   * Reduces the count of active Sauce Connect processes for the user by 1.
   *
   * @param tunnelInfo the tunnel
   * @param printStream the output stream to send log messages
   * @return current count of active Sauce Connect processes for the user
   */
  private int decrementProcessCountForUser(TunnelInformation tunnelInfo, PrintStream printStream) {
    int count = tunnelInfo.getProcessCount() - 1;
    tunnelInfo.setProcessCount(count);
    logMessage(printStream, "Decremented process count for " + tunnelInfo + ", now " + count);
    return count;
  }

  /**
   * Logs a message to the print stream (if not null), and to the logger instance for the class.
   *
   * @param printStream the output stream to send log messages
   * @param message the message to be logged
   */
  protected void logMessage(PrintStream printStream, String message) {
    if (printStream != null) {
      printStream.println(message);
    }
    this.logger.info(message);
  }

  /**
   * Adds an element to an array
   *
   * @param original the original array
   * @param added the element to add
   * @return a new array with the element added to the end
   */
  protected String[] addElement(String[] original, String added) {
    // split added on space
    String[] split = StringUtils.split(added, ' ');
    return joinArgs(original, split);
  }

  protected String[] joinArgs(String[] initial, String... toAdd) {
    String[] result = Arrays.copyOf(initial, initial.length + toAdd.length);
    System.arraycopy(toAdd, 0, result, initial.length, toAdd.length);
    return result;
  }

  /**
   * Increases the number of Sauce Connect invocations for the user by 1.
   *
   * @param name the tunnel name
   * @param printStream the output stream to send log messages
   */
  protected void incrementProcessCountForUser(TunnelInformation name, PrintStream printStream) {
    int processCount = name.getProcessCount() + 1;
    name.setProcessCount(processCount);
    logMessage(printStream, "Incremented process count for " + name + ", now " + processCount);
  }

  /**
   * @param username name of the user which launched Sauce Connect
   * @param apiKey api key corresponding to the user
   * @param port port which Sauce Connect should be launched on
   * @param sauceConnectJar File which contains the Sauce Connect executables (typically the CI
   *     plugin Jar file)
   * @param options the command line options used to launch Sauce Connect
   * @param printStream the output stream to send log messages
   * @param sauceConnectPath if defined, Sauce Connect will be launched from the specified path and
   *     won't be extracted from the jar file
   * @param legacy options uses SC4 style CLI
   * @return new ProcessBuilder instance which will launch Sauce Connect
   * @throws SauceConnectException thrown if an error occurs launching the Sauce Connect process
   */
  protected abstract Process prepAndCreateProcess(
      String username,
      String apiKey,
      int port,
      File sauceConnectJar,
      String options,
      PrintStream printStream,
      String sauceConnectPath,
      boolean legacy)
      throws SauceConnectException;

  /**
   * @param args Arguments to run
   * @param directory Directory to run in
   * @throws IOException thrown if an error occurs launching the Sauce Connect process
   * @return Processbuilder to run
   */
  protected Process createProcess(String[] args, File directory) throws IOException {
    ProcessBuilder processBuilder = new ProcessBuilder(args);
    processBuilder.directory(directory);
    return processBuilder.start();
  }

  /**
   * Creates a new process to run Sauce Connect on a randomly allocated port in the US Data Center.
   *
   * @param username the name of the Sauce OnDemand user
   * @param apiKey the API Key for the Sauce OnDemand user
   * @param sauceConnectJar the Jar file containing Sauce Connect. If null, then we attempt to find
   *     Sauce Connect from the classpath (only used by SauceConnectTwoManager)
   * @param options the command line options to pass to Sauce Connect
   * @param printStream A print stream in which to redirect the output from Sauce Connect to. Can be
   *     null
   * @param verboseLogging indicates whether verbose logging should be output
   * @param sauceConnectPath if defined, Sauce Connect will be launched from the specified path and
   *     won't be extracted from the jar file
   * @return a {@link Process} instance which represents the Sauce Connect instance
   * @throws SauceConnectException thrown if an error occurs launching Sauce Connect
   */
  public Process openConnection(
      String username,
      String apiKey,
      File sauceConnectJar,
      String options,
      PrintStream printStream,
      Boolean verboseLogging,
      String sauceConnectPath)
      throws SauceConnectException {
    return openConnection(
        username,
        apiKey,
        DataCenter.US_WEST,
        findFreePort(),
        sauceConnectJar,
        options,
        printStream,
        verboseLogging,
        sauceConnectPath);
  }

  /**
   * Creates a new process to run Sauce Connect in the US Data Center.
   *
   * @param username the name of the Sauce OnDemand user
   * @param apiKey the API Key for the Sauce OnDemand user
   * @param port the port which Sauce Connect should be run on
   * @param sauceConnectJar the Jar file containing Sauce Connect. If null, then we attempt to find
   *     Sauce Connect from the classpath (only used by SauceConnectTwoManager)
   * @param options the command line options to pass to Sauce Connect
   * @param printStream A print stream in which to redirect the output from Sauce Connect to. Can be
   *     null
   * @param verboseLogging indicates whether verbose logging should be output
   * @param sauceConnectPath if defined, Sauce Connect will be launched from the specified path and
   *     won't be extracted from the jar file
   * @return a {@link Process} instance which represents the Sauce Connect instance
   * @throws SauceConnectException thrown if an error occurs launching Sauce Connect
   */
  public Process openConnection(
      String username,
      String apiKey,
      int port,
      File sauceConnectJar,
      String options,
      PrintStream printStream,
      Boolean verboseLogging,
      String sauceConnectPath)
      throws SauceConnectException {
    return openConnection(
        username,
        apiKey,
        DataCenter.US_WEST,
        port,
        sauceConnectJar,
        options,
        printStream,
        verboseLogging,
        sauceConnectPath);
  }

  /**
   * Creates a new process to run Sauce Connect.
   *
   * @param username the name of the Sauce OnDemand user
   * @param apiKey the API Key for the Sauce OnDemand user
   * @param dataCenter the Sauce Labs Data Center name (US_WEST, EU_CENTRAL, US_EAST,
   *     APAC_SOUTHEAST)
   * @param port the port which Sauce Connect should be run on
   * @param sauceConnectJar the Jar file containing Sauce Connect. If null, then we attempt to find
   *     Sauce Connect from the classpath (only used by SauceConnectTwoManager)
   * @param options the command line options to pass to Sauce Connect
   * @param printStream A print stream in which to redirect the output from Sauce Connect to. Can be
   *     null
   * @param verboseLogging indicates whether verbose logging should be output
   * @param sauceConnectPath if defined, Sauce Connect will be launched from the specified path and
   *     won't be extracted from the jar file
   * @return a {@link Process} instance which represents the Sauce Connect instance
   * @throws SauceConnectException thrown if an error occurs launching Sauce Connect
   * @deprecated Use {@link #openConnection(String, String, DataCenter, int, File, String,
   *     PrintStream, Boolean, String)} instead
   */
  @Deprecated
  @Override
  public Process openConnection(
      String username,
      String apiKey,
      String dataCenter,
      int port,
      File sauceConnectJar,
      String options,
      PrintStream printStream,
      Boolean verboseLogging,
      String sauceConnectPath)
      throws SauceConnectException {
    return openConnection(
        username,
        apiKey,
        DataCenter.fromString(dataCenter),
        port,
        sauceConnectJar,
        options,
        printStream,
        verboseLogging,
        sauceConnectPath);
  }

    /**
     * Creates a new process to run Sauce Connect on a randomly allocated port.
     *
     * @param username the name of the Sauce OnDemand user
     * @param apiKey the API Key for the Sauce OnDemand user
     * @param dataCenter the Sauce Labs Data Center
     * @param sauceConnectJar the Jar file containing Sauce Connect. If null, then we attempt to find
     *     Sauce Connect from the classpath (only used by SauceConnectTwoManager)
     * @param options the command line options to pass to Sauce Connect
     * @param printStream A print stream in which to redirect the output from Sauce Connect to. Can be
     *     null
     * @param verboseLogging indicates whether verbose logging should be output
     * @param sauceConnectPath if defined, Sauce Connect will be launched from the specified path and
     *     won't be extracted from the jar file
     * @return a {@link Process} instance which represents the Sauce Connect instance
     * @throws SauceConnectException thrown if an error occurs launching Sauce Connect
     */
    @Override
    public Process openConnection(
        String username,
        String apiKey,
        DataCenter dataCenter,
        File sauceConnectJar,
        String options,
        PrintStream printStream,
        Boolean verboseLogging,
        String sauceConnectPath)
        throws SauceConnectException {
        return openConnection(
            username,
            apiKey,
            dataCenter,
            findFreePort(),
            sauceConnectJar,
            options,
            printStream,
            verboseLogging,
            sauceConnectPath);
    }

  /**
   * Creates a new process to run Sauce Connect.
   *
   * @param username the name of the Sauce OnDemand user
   * @param apiKey the API Key for the Sauce OnDemand user
   * @param dataCenter the Sauce Labs Data Center
   * @param port the port which Sauce Connect should be run on
   * @param sauceConnectJar the Jar file containing Sauce Connect. If null, then we attempt to find
   *     Sauce Connect from the classpath (only used by SauceConnectTwoManager)
   * @param options the command line options to pass to Sauce Connect
   * @param printStream A print stream in which to redirect the output from Sauce Connect to. Can be
   *     null
   * @param verboseLogging indicates whether verbose logging should be output
   * @param sauceConnectPath if defined, Sauce Connect will be launched from the specified path and
   *     won't be extracted from the jar file
   * @return a {@link Process} instance which represents the Sauce Connect instance
   * @throws SauceConnectException thrown if an error occurs launching Sauce Connect
   */
  @Override
  public Process openConnection(
      String username,
      String apiKey,
      DataCenter dataCenter,
      int port,
      File sauceConnectJar,
      String options,
      PrintStream printStream,
      Boolean verboseLogging,
      String sauceConnectPath)
      throws SauceConnectException {
        return openConnection(username, apiKey, dataCenter, port, sauceConnectJar, options, printStream, verboseLogging, sauceConnectPath, false);
  }

  /**
   * Creates a new process to run Sauce Connect.
   *
   * @param username the name of the Sauce OnDemand user
   * @param apiKey the API Key for the Sauce OnDemand user
   * @param dataCenter the Sauce Labs Data Center
   * @param port the port which Sauce Connect should be run on
   * @param sauceConnectJar the Jar file containing Sauce Connect. If null, then we attempt to find
   *     Sauce Connect from the classpath (only used by SauceConnectTwoManager)
   * @param options the command line options to pass to Sauce Connect
   * @param printStream A print stream in which to redirect the output from Sauce Connect to. Can be
   *     null
   * @param verboseLogging indicates whether verbose logging should be output
   * @param sauceConnectPath if defined, Sauce Connect will be launched from the specified path and
   *     won't be extracted from the jar file
   * @param legacy options are in SC4 CLI style
   * @return a {@link Process} instance which represents the Sauce Connect instance
   * @throws SauceConnectException thrown if an error occurs launching Sauce Connect
   */
  @Override
  public Process openConnection(
      String username,
      String apiKey,
      DataCenter dataCenter,
      int port,
      File sauceConnectJar,
      String options,
      PrintStream printStream,
      Boolean verboseLogging,
      String sauceConnectPath,
      boolean legacy)
      throws SauceConnectException {

    // ensure that only a single thread attempts to open a connection
    if (sauceRest == null) {
      setSauceRest(new SauceREST(username, apiKey, dataCenter));
    }
    String name = getTunnelName(options, username);
    TunnelInformation tunnelInformation = getTunnelInformation(name);
    try {

      tunnelInformation.getLock().lock();
      if (options == null) {
        options = "";
      }
      if (verboseLogging != null) {
        this.quietMode = !verboseLogging;
      }

      // do we have an instance for the tunnel name?
      String tunnelID = activeTunnelID(username, name);
      if (tunnelInformation.getProcessCount() == 0) {
        // if the count is zero, check to see if there are any active tunnels

        if (tunnelID != null) {
          // if we have an active tunnel, but the process count is zero, we have an orphaned SC
          // process
          // instead of deleting the tunnel, log a message
          logMessage(printStream, "Detected active tunnel: " + tunnelID);
        }
      } else {

        // check active tunnels via Sauce REST API
        if (tunnelID == null) {
          logMessage(
              printStream, "Process count non-zero, but no active tunnels found for name: " + name);
          logMessage(printStream, "Process count reset to zero");
          // if no active tunnels, we have a mismatch of the tunnel count
          // reset tunnel count to zero and continue to launch Sauce Connect
          tunnelInformation.setProcessCount(0);
        } else {
          // if we have an active tunnel, increment counter and return
          logMessage(printStream, "Sauce Connect already running for " + name);
          incrementProcessCountForUser(tunnelInformation, printStream);
          return tunnelInformation.getProcess();
        }
      }
      final Process process =
          prepAndCreateProcess(
              username, apiKey, port, sauceConnectJar, options, printStream, sauceConnectPath, legacy);

      // Print sauceconnect process stdout/stderr
      if (!quietMode) {
        new Thread(processOutputPrinter.getStdoutPrinter(process.getInputStream(), printStream)).start();
        new Thread(processOutputPrinter.getStderrPrinter(process.getErrorStream(), printStream)).start();
      }

      List<Process> openedProcesses = this.openedProcesses.get(name);
      SCMonitor scMonitor;
      if (this.scMonitor == null) {
        scMonitor = new DefaultSCMonitor(port, this.logger);
      } else {
        scMonitor = this.scMonitor;
      }

      try {
        Semaphore semaphore = new Semaphore(1);
        semaphore.acquire();
        scMonitor.setSemaphore(semaphore);
        new Thread(scMonitor).start();

        boolean sauceConnectStarted = semaphore.tryAcquire(3, TimeUnit.MINUTES);
        if (sauceConnectStarted && !scMonitor.isFailed()) {
          // everything okay, continue the build
          String provisionedTunnelId = scMonitor.getTunnelId();
          if (provisionedTunnelId != null) {
            tunnelInformation.setTunnelId(provisionedTunnelId);
            waitForReadiness(provisionedTunnelId);
          }
          logMessage(printStream, "Sauce Connect " + getCurrentVersion() + " now launched for: " + name);
        } else {
          String message = scMonitor.isFailed()
            ? "Error launching Sauce Connect."
            : "Time out while waiting for Sauce Connect to start.";

          File sauceConnectLogFile = getSauceConnectLogFile(options);
          if (sauceConnectLogFile == null) {
            message += " Please check the Sauce Connect log";
          } else {
            message += " Please check the Sauce Connect log located in " + sauceConnectLogFile.getAbsoluteFile();
          }

          logMessage(printStream, message);
          printHealthcheckException(scMonitor, printStream);

          // ensure that Sauce Connect process is closed
          closeSauceConnectProcess(printStream, process);
          throw new SauceConnectDidNotStartException(message);
        }
      } catch (InterruptedException e) {
        // continue;
        this.logger.warn("Exception occurred during invocation of Sauce Connect", e);
      }

      incrementProcessCountForUser(tunnelInformation, printStream);
      tunnelInformation.setProcess(process);
      List<Process> processes = openedProcesses;
      if (processes == null) {
        processes = new ArrayList<>();
        this.openedProcesses.put(name, processes);
      }
      processes.add(process);
      return process;
    } finally {
      // release the access lock
      tunnelInformation.getLock().unlock();
      launchAttempts.set(0);
    }
  }

  private void printHealthcheckException(SCMonitor scMonitor, PrintStream printStream) {
      Exception healthcheckException = scMonitor.getLastHealtcheckException();
      if (healthcheckException != null) {
          logMessage(printStream, "Healthcheck exception: " + healthcheckException.getMessage());
      }
  }

  private void waitForReadiness(String tunnelId) {
    long pollingIntervalMillis = READINESS_CHECK_POLLING_INTERVAL.toMillis();
    long endTime = System.currentTimeMillis() + READINESS_CHECK_TIMEOUT.toMillis();
    try {
      do {
        long iterationStartTime = System.currentTimeMillis();
        Boolean isReady = scEndpoint.getTunnelInformation(tunnelId).isReady;
        if (Boolean.TRUE.equals(isReady)) {
            this.logger.info("Tunnel with ID {} is ready for use", tunnelId);
            return;
        }
        this.logger.info("Waiting for readiness of tunnel with ID {}", tunnelId);
        long iterationEndTime = System.currentTimeMillis();

        long iterationPollingTimeout = pollingIntervalMillis - (iterationEndTime - iterationStartTime);
        if (iterationPollingTimeout > 0) {
            TimeUnit.MILLISECONDS.sleep(iterationPollingTimeout);
        }
      }
      while (System.currentTimeMillis() <= endTime);
      this.logger.warn("Wait for readiness of tunnel with ID {} is timed out", tunnelId);
    }
    catch (IOException | InterruptedException e) {
      this.logger.warn("Unable to check readiness of tunnel with ID {}", tunnelId, e);
    }
  }

  private TunnelInformation getTunnelInformation(String name) {
    if (name == null) {
      return null;
    }
    return tunnelInformationMap.computeIfAbsent(name, TunnelInformation::new);
  }

  /**
   * Queries the Sauce REST API to find the active tunnel for the user/tunnel name.
   *
   * @param username the Sauce username
   * @param tunnelName tunnel name, can be the same as the username
   * @return String the internal Sauce tunnel id
   */
  private String activeTunnelID(String username, String tunnelName) {
    try {
      List<com.saucelabs.saucerest.model.sauceconnect.TunnelInformation> tunnelsInformation =
        scEndpoint.getTunnelsInformationForAUser();

      for (com.saucelabs.saucerest.model.sauceconnect.TunnelInformation tunnelInformation : tunnelsInformation) {
        String configName = tunnelInformation.tunnelIdentifier;
        String status = tunnelInformation.status;
        if ("running".equalsIgnoreCase(status)
                && ("null".equalsIgnoreCase(configName) && tunnelName.equals(username))
            || !"null".equalsIgnoreCase(configName) && configName.equals(tunnelName)) {
          // we have an active tunnel
          return tunnelInformation.id;
        }
      }
    } catch (JSONException | IOException e) {
      // log error and return false
      this.logger.warn("Exception occurred retrieving tunnel information", e);
    }
    return null;
  }

  protected abstract String getCurrentVersion();

  /**
   * Returns the arguments to be used to launch Sauce Connect
   *
   * @param args the initial Sauce Connect command line args
   * @param username name of the user which launched Sauce Connect
   * @param apiKey the access key for the Sauce user
   * @param options command line args specified by the user
   * @return String array representing the command line args to be used to launch Sauce Connect
   */
  protected abstract String[] generateSauceConnectArgs(
      String[] args, String username, String apiKey, String options);

  protected abstract String[] addExtraInfo(String[] args);

  /**
   * @return the user's home directory
   */
  public String getSauceConnectWorkingDirectory() {
    return System.getProperty("user.home");
  }

  public abstract File getSauceConnectLogFile(String options);

  /** Base exception class which is thrown if an error occurs launching Sauce Connect. */
  public static class SauceConnectException extends IOException {

    public SauceConnectException(String message) {
      super(message);
    }

    public SauceConnectException(Exception cause) {
      super(cause);
    }

    public SauceConnectException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  /** Exception which is thrown when Sauce Connect does not start within the timeout period. */
  public static class SauceConnectDidNotStartException extends SauceConnectException {
    public SauceConnectDidNotStartException(String message) {
      super(message);
    }
  }

  private int findFreePort() throws SauceConnectException {
    try (ServerSocket socket = new ServerSocket(0)) {
        return socket.getLocalPort();
    } catch (IOException e) {
        throw new SauceConnectException("Unable to find free port", e);
    }
  }
}
