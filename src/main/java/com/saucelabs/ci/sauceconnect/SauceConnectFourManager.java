package com.saucelabs.ci.sauceconnect;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.LazyInitializer;
import org.apache.commons.lang3.concurrent.LazyInitializer.Builder;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import java.net.URL;

/**
 * Handles launching Sauce Connect v4 (binary executable).
 *
 * @author Ross Rowe
 */
public class SauceConnectFourManager extends AbstractSauceTunnelManager
    implements SauceTunnelManager {
  private boolean useLatestSauceConnect = false;

  /** Remove all created files and directories on exit */
  private boolean cleanUpOnExit;

  /** System which runs SauceConnect, this info is added to '--extra-info' argument */
  private final String runner;

  /** Represents the operating system-specific Sauce Connect binary. */
  public enum OperatingSystem {
    OSX("osx", "zip", null, UNIX_TEMP_DIR),
    WINDOWS("win32", "zip", null, WINDOWS_TEMP_DIR, "sc.exe"),
    LINUX("linux", "tar", "gz", UNIX_TEMP_DIR),
    LINUX_ARM64("linux-arm64", "tar", "gz", UNIX_TEMP_DIR);

    private final String directoryEnding;
    private final String archiveExtension;
    private final String archiveFormat;
    private final String compressionAlgorithm;
    private final String executable;
    private final String tempDirectory;

    OperatingSystem(
        String directoryEnding, String archiveFormat, String compressionAlgorithm, String tempDirectory,
        String executable) {
      this.directoryEnding = directoryEnding;
      this.archiveExtension = compressionAlgorithm == null ? archiveFormat : archiveFormat + "." + compressionAlgorithm;
      this.archiveFormat = archiveFormat;
      this.compressionAlgorithm = compressionAlgorithm;
      this.executable = "bin" + File.separatorChar + executable;
      this.tempDirectory = tempDirectory;
    }

    OperatingSystem(String directoryEnding, String archiveFormat, String compressionAlgorithm,
        String tempDirectory) {
      this(directoryEnding, archiveFormat, compressionAlgorithm, tempDirectory, "sc");
    }

    public static OperatingSystem getOperatingSystem() {
      String os = System.getProperty("os.name").toLowerCase();
      if (isWindows(os)) {
        return WINDOWS;
      }
      if (isMac(os)) {
        return OSX;
      }
      if (isUnix(os)) {
        String arch = System.getProperty("os.arch").toLowerCase();

        if (isArm(arch)) {
          return LINUX_ARM64;
        }
        return LINUX;
      }
      throw new IllegalStateException("Unsupported OS: " + os);
    }

    private static boolean isWindows(String os) {
      return os.contains("win");
    }

    private static boolean isMac(String os) {
      return os.contains("mac");
    }

    private static boolean isUnix(String os) {
      return os.contains("nux");
    }

    private static boolean isArm(String arch) {
      return arch.startsWith("arm") || arch.startsWith("aarch");
    }

    public String getDirectory(boolean useLatestSauceConnect) {
      return SAUCE_CONNECT + getVersion(useLatestSauceConnect) + '-' + directoryEnding;
    }

    public String getFileName(boolean useLatestSauceConnect) {
      return getDirectory(useLatestSauceConnect) + '.' + archiveExtension;
    }

    public String getExecutable() {
      return executable;
    }

    public String getDefaultSauceConnectLogDirectory() {
      return tempDirectory;
    }
  }

  private static final String UNIX_TEMP_DIR = "/tmp";

  private static final String WINDOWS_TEMP_DIR = System.getProperty("java.io.tmpdir");

  /** Output from Sauce Connect process which indicates that it has been started. */
  private static final String SAUCE_CONNECT_4_STARTED =
      "Sauce Connect is up, you may start your tests";

  public static final String CURRENT_SC_VERSION = "4.9.1";
  public static final LazyInitializer<String> LATEST_SC_VERSION = new Builder<LazyInitializer<String>, String>()
      .setInitializer(SauceConnectFourManager::getLatestSauceConnectVersion)
      .get();

  private static final String SAUCE_CONNECT = "sc-";
  public static final String SAUCE_CONNECT_4 = SAUCE_CONNECT + CURRENT_SC_VERSION;

  /** Constructs a new instance with quiet mode disabled. */
  public SauceConnectFourManager() {
    this(false);
  }

  /**
   * Constructs a new instance with quiet mode disabled.
   *
   * @param runner System which runs SauceConnect, this info is added to '--extra-info' argument
   */
  public SauceConnectFourManager(String runner) {
    this(false, runner);
  }

  /**
   * Constructs a new instance.
   *
   * @param quietMode indicates whether Sauce Connect output should be suppressed
   */
  public SauceConnectFourManager(boolean quietMode) {
    this(quietMode, "jenkins");
  }

  /**
   * Constructs a new instance.
   *
   * @param quietMode indicates whether Sauce Connect output should be suppressed
   * @param runner System which runs SauceConnect, this info is added to '--extra-info' argument
   */
  public SauceConnectFourManager(boolean quietMode, String runner) {
    super(quietMode);
    this.runner = runner;
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
   * @return new ProcessBuilder instance which will launch Sauce Connect
   * @throws SauceConnectException thrown if an error occurs extracting the Sauce Connect binary
   *     from the CI jar file
   */
  @Override
  protected Process prepAndCreateProcess(
      String username,
      String apiKey,
      int port,
      File sauceConnectJar,
      String options,
      PrintStream printStream,
      String sauceConnectPath)
      throws SauceConnectException {

    // find zip file to extract
    try {
      File sauceConnectBinary;
      if (sauceConnectPath == null || sauceConnectPath.isEmpty()) {
        File workingDirectory = null;
        if (sauceConnectJar != null && sauceConnectJar.exists()) {
          workingDirectory = sauceConnectJar.getParentFile();
        }
        if (workingDirectory == null) {
          workingDirectory = new File(getSauceConnectWorkingDirectory());
        }
        if (!workingDirectory.canWrite()) {
          throw new SauceConnectException(
              "Can't write to "
                  + workingDirectory.getAbsolutePath()
                  + ", please check the directory permissions");
        }
        OperatingSystem operatingSystem = OperatingSystem.getOperatingSystem();
        File unzipDirectory = getUnzipDir(workingDirectory, operatingSystem);
        sauceConnectBinary = new File(unzipDirectory, operatingSystem.getExecutable());
        if (!sauceConnectBinary.exists()) {
          synchronized (this) {
            if (!sauceConnectBinary.exists()) {
              extractZipFile(workingDirectory, operatingSystem);
            }
          }
        } else {
          logMessage(printStream, sauceConnectBinary + " already exists, so not extracting");
        }
      } else {
        sauceConnectBinary = new File(sauceConnectPath);
        if (!sauceConnectBinary.exists()) {
          throw new SauceConnectException(
              sauceConnectPath + " doesn't exist, please check the location");
        }
      }

      // although we are setting the working directory, we need to specify the full path to the exe
      String[] args = {sauceConnectBinary.getPath()};
      args = generateSauceConnectArgs(args, username, apiKey, port, options);
      args = addExtraInfo(args);

      LOGGER.info("Launching Sauce Connect {} {}", getCurrentVersion(), hideSauceConnectCommandlineSecrets(args));
      return createProcess(args, sauceConnectBinary.getParentFile());
    } catch (IOException e) {
      throw new SauceConnectException(e);
    }
  }

  public String hideSauceConnectCommandlineSecrets(String[] args) {
    HashMap<String, String> map = new HashMap<>();
    map.put("-k", "()\\w+-\\w+-\\w+-\\w+-\\w+");
    map.put("--api-key", "()\\w+-\\w+-\\w+-\\w+-\\w+");
    map.put("-w", "(\\S+:)\\S+");
    map.put("--proxy-userpwd", "(\\S+:)\\S+");
    map.put("-a", "(\\S+:\\d+:\\S+:)\\S+");
    map.put("--auth", "(\\S+:\\d+:\\S+:)\\S+");
    String regexpForNextElement = null;
    List<String> hiddenArgs = new ArrayList<>();

    for (String arg : args) {
      if (regexpForNextElement != null) {
        hiddenArgs.add(arg.replaceAll(regexpForNextElement, "$1****"));
        regexpForNextElement = null;
      } else {
        hiddenArgs.add(arg);
        regexpForNextElement = map.getOrDefault(arg, null);
      }
    }
    return Arrays.toString(hiddenArgs.toArray());
  }

  public void setUseLatestSauceConnect(boolean useLatestSauceConnect) {
    this.useLatestSauceConnect = useLatestSauceConnect;
  }

  public void setCleanUpOnExit(boolean cleanUpOnExit) {
    this.cleanUpOnExit = cleanUpOnExit;
  }

  public static String getLatestSauceConnectVersion() {
    try {
      URI url = URI.create("https://saucelabs.com/versions.json");
      HttpRequest request = HttpRequest.newBuilder(url).build();
      String versionsJson = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString()).body();
      return new JSONObject(versionsJson).getJSONObject("Sauce Connect").getString("version");
    } catch (IOException | InterruptedException e) {
      return null;
    }
  }

  /**
   * @param args the initial Sauce Connect command line args
   * @param username name of the user which launched Sauce Connect
   * @param apiKey the access key for the Sauce user
   * @param port the port that Sauce Connect should be launched on
   * @param options command line args specified by the user
   * @return String array representing the command line args to be used to launch Sauce Connect
   */
  protected String[] generateSauceConnectArgs(
      String[] args, String username, String apiKey, int port, String options) {
    String[] result =
        joinArgs(args, "-u", username.trim(), "-k", apiKey.trim(), "-P", String.valueOf(port));
    result = addElement(result, options);
    return result;
  }

  protected String[] addExtraInfo(String[] args) {
    String[] result;
    OperatingSystem operatingSystem = OperatingSystem.getOperatingSystem();
    if (operatingSystem == OperatingSystem.WINDOWS) {
      result = joinArgs(args, "--extra-info", "{\\\"runner\\\": \\\"" + runner + "\\\"}");
    } else {
      result = joinArgs(args, "--extra-info", "{\"runner\": \"" + runner + "\"}");
    }
    return result;
  }

  /**
   * @param workingDirectory the destination directory
   * @param operatingSystem represents the current operating system
   * @return the directory containing the extracted files
   * @throws IOException thrown if an error occurs extracting the files
   */
  public File extractZipFile(File workingDirectory, OperatingSystem operatingSystem) throws IOException {
    String archiveFileName = operatingSystem.getFileName(useLatestSauceConnect);

    InputStream archiveInputStream = useLatestSauceConnect ?
      new URL("https://saucelabs.com/downloads/" + archiveFileName).openStream() :
      getClass().getClassLoader().getResourceAsStream(archiveFileName);
    extract(archiveInputStream, workingDirectory.toPath(), operatingSystem.archiveFormat,
      operatingSystem.compressionAlgorithm);

    File unzipDir = getUnzipDir(workingDirectory, operatingSystem);
    if (cleanUpOnExit) {
      unzipDir.deleteOnExit();
    }

    File sauceConnectBinary = new File(unzipDir, operatingSystem.getExecutable());
    if (!sauceConnectBinary.canExecute() && !sauceConnectBinary.setExecutable(true)) {
      LOGGER.warn("Unable to set the execute permission for SauceConnect binary file located at {}",
          sauceConnectBinary);
    }
    return unzipDir;
  }

  private static void extract(InputStream archiveInputStream, Path workingDirectory, String archiveFormat,
    String compressionAlgorithm) throws IOException {
      try (InputStream is = archiveInputStream;
        InputStream bis = new BufferedInputStream(is);
        InputStream cis = compressionAlgorithm == null ? bis :
            CompressorStreamFactory.getSingleton().createCompressorInputStream(compressionAlgorithm, bis);
        ArchiveInputStream ais = ArchiveStreamFactory.DEFAULT.createArchiveInputStream(archiveFormat, cis)) {
          ArchiveEntry archiveEntry;
          while ((archiveEntry = ais.getNextEntry()) != null) {
            Path path = workingDirectory.resolve(archiveEntry.getName());
            if (archiveEntry.isDirectory()) {
              Files.createDirectories(path);
            } else {
              Files.copy(ais, path);
            }
          }
      } catch (ArchiveException | CompressorException e) {
        throw new IOException(e);
      }
  }

  private File getUnzipDir(File workingDirectory, OperatingSystem operatingSystem) {
    return new File(workingDirectory, operatingSystem.getDirectory(useLatestSauceConnect));
  }

  /** {@inheritDoc} */
  protected String getSauceStartedMessage() {
    return SAUCE_CONNECT_4_STARTED;
  }

  @Override
  protected String getCurrentVersion() {
    return getVersion(useLatestSauceConnect);
  }

  private static String getVersion(boolean useLatestSauceConnect) {
    if (useLatestSauceConnect) {
      try {
        String latestVersion = LATEST_SC_VERSION.get();
        if (latestVersion != null) {
          return latestVersion;
        }
      }
      catch (ConcurrentException e) {
        // Never happens
      }
    }
    return CURRENT_SC_VERSION;
  }

  /**
   * Attempts to find the Sauce Connect log file. If the --logfile argument has been specified, then
   * use that location, otherwise look at the operating system/tunnel identifer to determine the
   * location.
   *
   * @param options the Sauce Connect command line options, can be null
   * @return File representing the Sauce Connect log file, can be null
   */
  @Override
  public File getSauceConnectLogFile(String options) {

    // Has --logfile arg been specified
    String logfile = getLogfile(options);
    if (logfile != null) {

      File sauceConnectLogFile = new File(logfile);
      if (sauceConnectLogFile.exists()) {
        return sauceConnectLogFile;
      } else {
        return null;
      }
    }

    // otherwise, try to work out location
    String fileName = "sc.log";
    File logFileDirectory =
        new File(OperatingSystem.getOperatingSystem().getDefaultSauceConnectLogDirectory());

    // has --tunnel-name been specified?
    String tunnelName = getTunnelName(options, null);
    if (tunnelName != null) {
      fileName = MessageFormat.format("sc-{0}.log", tunnelName);
    }
    File sauceConnectLogFile = new File(logFileDirectory, fileName);
    if (!sauceConnectLogFile.exists()) {
      // try working directory
      sauceConnectLogFile = new File(getSauceConnectWorkingDirectory(), fileName);
      if (!sauceConnectLogFile.exists()) {
        return null;
      }
    }
    return sauceConnectLogFile;
  }
}
