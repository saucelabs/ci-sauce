package com.saucelabs.ci.sauceconnect;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.archiver.AbstractUnArchiver;
import org.codehaus.plexus.archiver.tar.TarGZipUnArchiver;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

import java.net.URL;

/**
 * Handles launching Sauce Connect v4 (binary executable).
 *
 * @author Ross Rowe
 */
public class SauceConnectFourManager extends AbstractSauceTunnelManager implements SauceTunnelManager {
    private boolean useLatestSauceConnect = false;

    /**
     * Remove all created files and directories on exit
     */
    private boolean cleanUpOnExit;

    /**
     * Represents the operating system-specific Sauce Connect binary.
     */
    public enum OperatingSystem {

        OSX("osx", "zip", UNIX_TEMP_DIR),
        WINDOWS("win32", "zip", WINDOWS_TEMP_DIR, "sc.exe"),
        LINUX("linux", "tar.gz", UNIX_TEMP_DIR),
        LINUX_ARM64("linux-arm64", "tar.gz", UNIX_TEMP_DIR);

        private final String directoryEnding;
        private final String archiveExtension;
        private final String executable;
        private final String tempDirectory;

        OperatingSystem(String directoryEnding, String archiveExtension, String tempDirectory, String executable) {
            this.directoryEnding = directoryEnding;
            this.archiveExtension = archiveExtension;
            this.executable = "bin" + File.separatorChar + executable;
            this.tempDirectory = tempDirectory;
        }

        OperatingSystem(String directoryEnding, String archiveExtension, String tempDirectory) {
            this(directoryEnding, archiveExtension, tempDirectory, "sc");
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
    /**
     * Output from Sauce Connect process which indicates that it has been started.
     */
    private static final String SAUCE_CONNECT_4_STARTED = "Sauce Connect is up, you may start your tests";

    public static final String CURRENT_SC_VERSION = "4.9.1";
    public static final String LATEST_SC_VERSION = getLatestSauceConnectVersion();

    private static final String SAUCE_CONNECT = "sc-";
    public static final String SAUCE_CONNECT_4 = SAUCE_CONNECT + CURRENT_SC_VERSION;

    /**
     * Constructs a new instance with quiet mode disabled.
     */
    public SauceConnectFourManager() {
        this(false);
    }

    /**
     * Constructs a new instance.
     *
     * @param quietMode indicates whether Sauce Connect output should be suppressed
     */
    public SauceConnectFourManager(boolean quietMode) {
        super(quietMode);
    }

    /**
     * @param username         name of the user which launched Sauce Connect
     * @param apiKey           api key corresponding to the user
     * @param port             port which Sauce Connect should be launched on
     * @param sauceConnectJar  File which contains the Sauce Connect executables (typically the CI plugin Jar file)
     * @param options          the command line options used to launch Sauce Connect
     * @param printStream      the output stream to send log messages
     * @param sauceConnectPath if defined, Sauce Connect will be launched from the specified path and won't be extracted from the jar file
     * @return new ProcessBuilder instance which will launch Sauce Connect
     * @throws SauceConnectException thrown if an error occurs extracting the Sauce Connect binary from the CI jar file
     */
    @Override
    protected Process prepAndCreateProcess(String username, String apiKey, int port, File sauceConnectJar, String options, PrintStream printStream, String sauceConnectPath) throws SauceConnectException {

        //find zip file to extract
        try {
            File sauceConnectBinary;
            if (sauceConnectPath == null || sauceConnectPath.equals("")) {
                File workingDirectory = null;
                if (sauceConnectJar != null && sauceConnectJar.exists()) {
                    workingDirectory = sauceConnectJar.getParentFile();
                }
                if (workingDirectory == null) {
                    workingDirectory = new File(getSauceConnectWorkingDirectory());
                }
                if (!workingDirectory.canWrite()) {
                    throw new SauceConnectException("Can't write to " + workingDirectory.getAbsolutePath() + ", please check the directory permissions");
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
                    throw new SauceConnectException(sauceConnectPath + " doesn't exist, please check the location");
                }
            }

            //although we are setting the working directory, we need to specify the full path to the exe
            String[] args = { sauceConnectBinary.getPath() };
            args = generateSauceConnectArgs(args, username, apiKey, port, options);
            args = addExtraInfo(args);

            julLogger.log(Level.INFO, "Launching Sauce Connect " + getCurrentVersion() + " " + hideSauceConnectCommandlineSecrets(args));
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

        for (String arg: args) {
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
            URL url = new URL("https://saucelabs.com/versions.json");
            String versionsJson = IOUtils.toString(url, StandardCharsets.UTF_8);
            return new JSONObject(versionsJson).getJSONObject("Sauce Connect").getString("version");
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * @param args     the initial Sauce Connect command line args
     * @param username name of the user which launched Sauce Connect
     * @param apiKey   the access key for the Sauce user
     * @param port     the port that Sauce Connect should be launched on
     * @param options  command line args specified by the user
     * @return String array representing the command line args to be used to launch Sauce Connect
     */
    protected String[] generateSauceConnectArgs(String[] args, String username, String apiKey, int port, String options) {
        String[] result = joinArgs(args, "-u", username.trim(), "-k", apiKey.trim(), "-P", String.valueOf(port));
        result = addElement(result, options);
        return result;
    }

    protected String[] addExtraInfo(String[] args) {
        String[] result;
        OperatingSystem operatingSystem = OperatingSystem.getOperatingSystem();
        if (operatingSystem == OperatingSystem.WINDOWS) {
            result = joinArgs(args, "--extra-info", "{\\\"runner\\\": \\\"jenkins\\\"}");
        } else {
            result = joinArgs(args, "--extra-info", "{\"runner\": \"jenkins\"}");
        }
        return result;
    }

    /**
     * @param workingDirectory the destination directory
     * @param operatingSystem  represents the current operating system
     * @return the directory containing the extracted files
     * @throws IOException thrown if an error occurs extracting the files
     */
    public File extractZipFile(File workingDirectory, OperatingSystem operatingSystem) throws IOException {
        File zipFile = extractFile(workingDirectory, operatingSystem.getFileName(useLatestSauceConnect));
        if (cleanUpOnExit) {
            zipFile.deleteOnExit();
        }
        AbstractUnArchiver unArchiver;
        if (operatingSystem == OperatingSystem.OSX || operatingSystem == OperatingSystem.WINDOWS) {
            unArchiver = new ZipUnArchiver();
        } else if (operatingSystem == OperatingSystem.LINUX || operatingSystem == OperatingSystem.LINUX_ARM64) {
            removeOldTarFile(zipFile);
            unArchiver = new TarGZipUnArchiver();
        } else {
            throw new RuntimeException("Unknown operating system: " + operatingSystem.name());
        }
        unArchiver.setSourceFile(zipFile);
        unArchiver.setDestDirectory(workingDirectory);
        unArchiver.extract();
        File unzipDir = getUnzipDir(workingDirectory, operatingSystem);
        if (cleanUpOnExit) {
            unzipDir.deleteOnExit();
        }
        return unzipDir;
    }

    private File getUnzipDir(File workingDirectory, OperatingSystem operatingSystem) {
        return new File(workingDirectory, operatingSystem.getDirectory(useLatestSauceConnect));
    }

    private void removeOldTarFile(File zipFile) throws SauceConnectException {
        File tarFile = new File(zipFile.getParentFile(), zipFile.getName().replaceAll(".gz", ""));
        removeFileIfExists(tarFile, "Unable to delete old tar");
    }

    /**
     * @param workingDirectory the destination directory
     * @param fileName         the name of the file to extract
     * @return the directory containing the extracted files
     * @throws IOException thrown if an error occurs extracting the files
     */
    private File extractFile(File workingDirectory, String fileName) throws IOException {
        File destination = new File(workingDirectory, fileName);
        removeFileIfExists(destination, "Unable to delete old zip");
        InputStream inputStream = useLatestSauceConnect ? new URL("https://saucelabs.com/downloads/" + fileName)
            .openStream() : getClass().getClassLoader().getResourceAsStream(fileName);
        FileUtils.copyInputStreamToFile(inputStream, destination);
        return destination;
    }

    private static void removeFileIfExists(File file, String exceptionMessage) throws SauceConnectException {
        if (file.exists() && !file.delete()) {
            throw new SauceConnectException(exceptionMessage);
        }
    }

    /**
     * {@inheritDoc}
     */
    protected String getSauceStartedMessage() {
        return SAUCE_CONNECT_4_STARTED;
    }

    @Override
    protected String getCurrentVersion() {
        return getVersion(useLatestSauceConnect);
    }

    private static String getVersion(boolean useLatestSauceConnect) {
        return useLatestSauceConnect && LATEST_SC_VERSION != null ? LATEST_SC_VERSION : CURRENT_SC_VERSION;
    }

    /**
     * Attempts to find the Sauce Connect log file.  If the --logfile argument has been specified, then
     * use that location, otherwise look at the operating system/tunnel identifer to determine the location.
     *
     * @param options the Sauce Connect command line options, can be null
     *
     * @return File representing the Sauce Connect log file, can be null
     */
    @Override
    public File getSauceConnectLogFile(String options) {

        //Has --logfile arg been specified
        String logfile = getLogfile(options);
        if (logfile != null) {

            File sauceConnectLogFile = new File(logfile);
            if (sauceConnectLogFile.exists()) {
                return sauceConnectLogFile;
            } else {
                return null;
            }
        }

        //otherwise, try to work out location
        String fileName = "sc.log";
        File logFileDirectory = new File(OperatingSystem.getOperatingSystem().getDefaultSauceConnectLogDirectory());

        //has --tunnel-name been specified?
        String tunnelName = getTunnelName(options, null);
        if (tunnelName != null) {
            fileName = MessageFormat.format("sc-{0}.log", tunnelName);
        }
        File sauceConnectLogFile = new File(logFileDirectory, fileName);
        if (!sauceConnectLogFile.exists()) {
            //try working directory
            sauceConnectLogFile = new File(getSauceConnectWorkingDirectory(), fileName);
            if (!sauceConnectLogFile.exists()) {
                return null;
            }
        }
        return sauceConnectLogFile;
    }
}
