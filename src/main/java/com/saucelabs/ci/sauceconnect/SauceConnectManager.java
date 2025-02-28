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
import org.slf4j.Logger;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Handles launching Sauce Connect (binary executable).
 *
 * @author Ross Rowe
 */
public class SauceConnectManager extends AbstractSauceTunnelManager implements SauceTunnelManager {
  private boolean useLatestSauceConnect = false;

  /** Remove all created files and directories on exit */
  private boolean cleanUpOnExit;

  /** System which runs SauceConnect, this info is added to '--metadata runner=' argument */
  private final String runner;

  /** Represents the operating system-specific Sauce Connect binary. */
  public enum OperatingSystem {
    OSX("darwin.all", "zip", null, UNIX_TEMP_DIR, "darwin"),
    WINDOWS_AMD64("windows.x86_64", "zip", null, WINDOWS_TEMP_DIR, "windows-amd64", "sauce-connect.exe"),
    WINDOWS_ARM64("windows.aarch64", "zip", null, WINDOWS_TEMP_DIR, "windows-arm64", "sauce-connect.exe"),
    LINUX_AMD64("linux.x86_64", "tar", "gz", UNIX_TEMP_DIR, "linux-amd64"),
    LINUX_ARM64("linux.aarch64", "tar", "gz", UNIX_TEMP_DIR, "linux-arm64");

    private final String directoryEnding;
    private final String archiveExtension;
    private final String archiveFormat;
    private final String compressionAlgorithm;
    private final String executable;
    private final String tempDirectory;
    private final String downloadPlatform;

    OperatingSystem(
        String directoryEnding, String archiveFormat, String compressionAlgorithm, String tempDirectory,
        String downloadPlatform, String executable) {
      this.directoryEnding = directoryEnding;
      this.archiveExtension = compressionAlgorithm == null ? archiveFormat : archiveFormat + "." + compressionAlgorithm;
      this.archiveFormat = archiveFormat;
      this.compressionAlgorithm = compressionAlgorithm;
      this.executable = executable;
      this.tempDirectory = tempDirectory;
      this.downloadPlatform = downloadPlatform;
    }

    OperatingSystem(String directoryEnding, String archiveFormat, String compressionAlgorithm,
        String tempDirectory, String downloadPlatform) {
      this(directoryEnding, archiveFormat, compressionAlgorithm, tempDirectory, downloadPlatform, "sc");
    }

    public static OperatingSystem getOperatingSystem() {
      String os = System.getProperty("os.name").toLowerCase();
      if (isWindows(os)) {
        String arch = System.getProperty("os.arch").toLowerCase();

        if (isArm(arch)) {
          return WINDOWS_ARM64;
        }
        return WINDOWS_AMD64;
      }
      if (isMac(os)) {
        return OSX;
      }
      if (isUnix(os)) {
        String arch = System.getProperty("os.arch").toLowerCase();

        if (isArm(arch)) {
          return LINUX_ARM64;
        }
        return LINUX_AMD64;
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
      return SAUCE_CONNECT_PREFIX + getVersion(useLatestSauceConnect) + '_' + directoryEnding;
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

  public static final String CURRENT_SC_VERSION = "5.2.2";
  public static final LazyInitializer<String> LATEST_SC_VERSION = new Builder<LazyInitializer<String>, String>()
      .setInitializer(SauceConnectManager::getLatestSauceConnectVersion)
      .get();

  private static final String SAUCE_CONNECT_PREFIX = "sauce-connect-";
  public static final String SAUCE_CONNECT = SAUCE_CONNECT_PREFIX + CURRENT_SC_VERSION;

  private static final int DEFAULT_API_PORT = 9000;
  private int apiPort;

  /** Constructs a new instance with quiet mode disabled. */
  public SauceConnectManager() {
    this(false);
  }

  /**
   * Constructs a new instance with quiet mode disabled.
   *
   * @param runner System which runs SauceConnect, this info is added to '--metadata runner=' argument
   */
  public SauceConnectManager(String runner) {
    this(false, runner, DEFAULT_API_PORT);
  }

  /**
   * Constructs a new instance.
   *
   * @param quietMode indicates whether Sauce Connect output should be suppressed
   */
  public SauceConnectManager(boolean quietMode) {
    this(quietMode, "jenkins", DEFAULT_API_PORT);
  }

  /**
   * Constructs a new instance.
   *
   * @param quietMode indicates whether Sauce Connect output should be suppressed
   * @param runner System which runs SauceConnect, this info is added to '--metadata runner=' argument
   * @param apiPort Port the Sauce Connect process will listen on
   */
  public SauceConnectManager(boolean quietMode, String runner, int apiPort) {
    super(quietMode);
    this.runner = runner;
    this.apiPort = apiPort;
  }

  /**
   * @param username name of the user which launched Sauce Connect
   * @param accessKey api key corresponding to the user
   * @param sauceConnectJar File which contains the Sauce Connect executables (typically the CI
   *     plugin Jar file)
   * @param options the command line options used to launch Sauce Connect
   * @param logger used for logging
   * @param sauceConnectPath if defined, Sauce Connect will be launched from the specified path and
   *     won't be extracted from the jar file
   * @return new ProcessBuilder instance which will launch Sauce Connect
   * @throws SauceConnectException thrown if an error occurs extracting the Sauce Connect binary
   *     from the CI jar file
   */
  @Override
  protected Process prepAndCreateProcess(
      String username,
      String accessKey,
      int apiPort,
      File sauceConnectJar,
      String options,
      Logger logger,
      String sauceConnectPath,
      boolean legacy)
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
              extractZipFile(workingDirectory, operatingSystem, logger);
            }
          }
        } else {
          logger.info("File {} already exists, so not extracting", sauceConnectBinary);
        }
      } else {
        sauceConnectBinary = new File(sauceConnectPath);
        if (!sauceConnectBinary.exists()) {
          throw new SauceConnectException(
              sauceConnectPath + " doesn't exist, please check the location");
        }
      }

      if ( apiPort != 0 ) {
        this.apiPort = apiPort;
      }

      // although we are setting the working directory, we need to specify the full path to the exe
      String[] args = {sauceConnectBinary.getPath()};
      if (legacy) {
          args = generateSauceConnectArgsLegacy(args, username, accessKey, options);
          args = addExtraInfoLegacy(args);
      } else {
          args = generateSauceConnectArgs(args, username, accessKey, options);
          args = addExtraInfo(args);
      }

      logger.info("Launching Sauce Connect {} {}", getCurrentVersion(), hideSauceConnectCommandlineSecrets(args));
      return createProcess(args, sauceConnectBinary.getParentFile());
    } catch (IOException e) {
      throw new SauceConnectException(e);
    }
  }

  public String hideSauceConnectCommandlineSecrets(String[] args) {
    var map = new HashMap<String, String>();
    map.put("-k", "^().*");
    map.put("--access-key", "^().*");
    map.put("--api-key", "^().*"); // Legacy command
    map.put("-a", "^().*");
    map.put("--auth", "^().*");
    map.put("--api-basic-auth", "^([^:]*:).*");
    map.put("-x", "^(.*:).*(@.*)");
    map.put("--proxy", "^(.*:).*(@.*)");
    String regexpForNextElement = null;

    var replaceMap = new HashMap<String, String>();
    replaceMap.put("-k", "****");
    replaceMap.put("--api-key", "****");
    replaceMap.put("--access-key", "****");
    replaceMap.put("-a", "****");
    replaceMap.put("--auth", "****");
    replaceMap.put("--api-basic-auth", "$1****");
    replaceMap.put("-x", "$1****$2");
    replaceMap.put("--proxy", "$1****$2");
    String replaceForNextElement = null;
    var hiddenArgs = new ArrayList<String>();

    for (String arg : args) {
      if (regexpForNextElement != null) {
        hiddenArgs.add(arg.replaceAll(regexpForNextElement, replaceForNextElement));
        regexpForNextElement = null;
      } else {
        hiddenArgs.add(arg);
        regexpForNextElement = map.getOrDefault(arg, null);
        replaceForNextElement = replaceMap.getOrDefault(arg, null);
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
      OperatingSystem operatingSystem = OperatingSystem.getOperatingSystem();
      String os = System.getProperty("os.name").toLowerCase();
      String arch = System.getProperty("os.arch").toLowerCase();

      String query;

      if (operatingSystem.isArm(arch)) {
        query = "arch=arm64&";
      } else {
        query = "arch=x86_64&";
      }

      if (operatingSystem.isWindows(os)) {
        query += "os=windows";
      } else if (operatingSystem.isMac(os)) {
        query += "os=macos";
      } else {
        query += "os=linux";
      }

      URI url = URI.create("https://api.us-west-1.saucelabs.com/rest/v1/public/tunnels/sauce-connect/download?" + query);
      HttpRequest request = HttpRequest.newBuilder(url).build();
      String versionsJson = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString()).body();
      return new JSONObject(versionsJson).getJSONObject("download").getString("version");
    } catch (IOException | InterruptedException e) {
      return null;
    }
  }

  /**
   * @param args the initial Sauce Connect command line args
   * @param username name of the user which launched Sauce Connect
   * @param accessKey the access key for the Sauce user
   * @param options command line args specified by the user
   * @return String array representing the command line args to be used to launch Sauce Connect
   */
  protected String[] generateSauceConnectArgs(
      String[] args, String username, String accessKey, String options) {
    String[] result =
        joinArgs(args, "run", "--username", username.trim(), "--access-key", accessKey.trim(), "--api-address", ":" + String.valueOf(this.apiPort));
    result = addElement(result, options);
    return result;
  }

  protected String[] addExtraInfo(String[] args) {
    String[] result = joinArgs(args, "--metadata", "runner=" + this.runner);
    return result;
  }

  /**
   * @param args the initial Sauce Connect command line args
   * @param username name of the user which launched Sauce Connect
   * @param accessKey the access key for the Sauce user
   * @param options command line args specified by the user
   * @return String array representing the command line args to be used to launch Sauce Connect
   */
  @Deprecated
  protected String[] generateSauceConnectArgsLegacy(
      String[] args, String username, String accessKey, String options) {
    String[] result;

    result = joinArgs(args, "legacy", "--user", username.trim(), "--api-key", accessKey.trim(), "--status-address", "0.0.0.0:" + String.valueOf(this.apiPort));
    result = addElement(result, options);
    return result;
  }

  @Deprecated
  protected String[] addExtraInfoLegacy(String[] args) {
    String[] result;
    OperatingSystem operatingSystem = OperatingSystem.getOperatingSystem();
    if (operatingSystem == OperatingSystem.WINDOWS_ARM64 || operatingSystem == OperatingSystem.WINDOWS_AMD64) {
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
  public File extractZipFile(File workingDirectory, OperatingSystem operatingSystem, Logger logger) throws IOException {
    String archiveFileName = operatingSystem.getFileName(useLatestSauceConnect);
    File unzipDir = getUnzipDir(workingDirectory, operatingSystem);
    unzipDir.mkdirs();

    InputStream archiveInputStream = useLatestSauceConnect ?
      new URL("https://saucelabs.com/downloads/sauce-connect/" + getCurrentVersion() + "/" + archiveFileName).openStream() :
      getClass().getClassLoader().getResourceAsStream(archiveFileName);
    extract(archiveInputStream, unzipDir.toPath(), operatingSystem.archiveFormat,
      operatingSystem.compressionAlgorithm);

    if (cleanUpOnExit) {
      unzipDir.deleteOnExit();
    }

    File sauceConnectBinary = new File(unzipDir, operatingSystem.getExecutable());
    if (!sauceConnectBinary.canExecute() && !sauceConnectBinary.setExecutable(true)) {
      logger.warn("Unable to set the execute permission for SauceConnect binary file located at {}", sauceConnectBinary);
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
            path.getParent().toFile().mkdirs();
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

  protected boolean isConnected() {
    HttpClient client = HttpClient.newHttpClient();

    HttpRequest request = HttpRequest.newBuilder()
      .uri(URI.create(String.format("http://localhost:%d", this.apiPort)))
      .GET()
      .build();

    try {
      HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
      return response.statusCode() == 200;
    } catch (IOException | InterruptedException e) {
      return false;
    }
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

    return null;
  }
}
