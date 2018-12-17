package com.saucelabs.ci.sauceconnect;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.plexus.archiver.AbstractUnArchiver;
import org.codehaus.plexus.archiver.tar.TarGZipUnArchiver;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Arrays;
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
     * Represents the operating system-specific Sauce Connect binary.
     */
    public enum OperatingSystem {

        OSX("osx", "zip", UNIX_TEMP_DIR),
        WINDOWS("win32", "zip", WINDOWS_TEMP_DIR, "sc.exe"),
        LINUX("linux", "tar.gz", UNIX_TEMP_DIR),
        LINUX32("linux32", "tar.gz", UNIX_TEMP_DIR);

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
                //check to see if we are on 64 bit
                return is64BitLinux() ? LINUX : LINUX32;
            }
            throw new IllegalStateException("Unsupported OS: " + os);
        }

        /**
         * Executes 'uname -a', if the result of the command contains '64', then return true
         *
         * @return boolean indicating whether OS is 64-bit
         */
        private static boolean is64BitLinux() {
            try {
                Runtime runtime = Runtime.getRuntime();
                Process process = runtime.exec("uname -a");
                process.waitFor();
                return IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8).contains("64");
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
            return false;
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

        public String getDirectory(boolean useLatestSauceConnect) {
            return SAUCE_CONNECT + getVersion(useLatestSauceConnect) + '-' + directoryEnding;
        }

        public String getFileName(boolean useLatestSauceConnect) {
            return getDirectory(useLatestSauceConnect) + '.' + archiveExtension;
        }

        private String getVersion(boolean useLatestSauceConnect) {
            return useLatestSauceConnect ? LATEST_SC_VERSION : CURRENT_SC_VERSION;
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

    public static final String CURRENT_SC_VERSION = "4.5.2";
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
                    extractZipFile(workingDirectory, operatingSystem);
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

            julLogger.log(Level.INFO, "Launching Sauce Connect " + getCurrentVersion() + " " + Arrays.toString(args).replaceAll("\\w+-\\w+-\\w+-\\w+-\\w+", "****"));
            return createProcess(args, sauceConnectBinary.getParentFile());
        } catch (IOException e) {
            throw new SauceConnectException(e);
        }
    }

    public void setUseLatestSauceConnect(boolean useLatestSauceConnect) {
        this.useLatestSauceConnect = useLatestSauceConnect;
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
        args = addElement(args, "-u");
        args = addElement(args, username);
        args = addElement(args, "-k");
        args = addElement(args, apiKey);
        args = addElement(args, "-P");
        args = addElement(args, String.valueOf(port));
        if (StringUtils.isNotBlank(options)) {
            args = addElement(args, options);
        }
        return args;
    }

    /**
     * @param workingDirectory the destination directory
     * @param operatingSystem  represents the current operating system
     * @return the directory containing the extracted files
     * @throws IOException thrown if an error occurs extracting the files
     */
    public File extractZipFile(File workingDirectory, OperatingSystem operatingSystem) throws IOException {
        File zipFile = extractFile(workingDirectory, operatingSystem.getFileName(useLatestSauceConnect));
        AbstractUnArchiver unArchiver;
        if (operatingSystem == OperatingSystem.OSX || operatingSystem == OperatingSystem.WINDOWS) {
            unArchiver = new ZipUnArchiver();
        } else if (operatingSystem == OperatingSystem.LINUX || operatingSystem == OperatingSystem.LINUX32) {
            removeOldTarFile(zipFile);
            unArchiver = new TarGZipUnArchiver();
        } else {
            throw new RuntimeException("Unknown operating system: " + operatingSystem.name());
        }
        extractArchive(unArchiver, zipFile, workingDirectory);
        return getUnzipDir(workingDirectory, operatingSystem);
    }

    private File getUnzipDir(File workingDirectory, OperatingSystem operatingSystem) {
        return new File(workingDirectory, operatingSystem.getDirectory(useLatestSauceConnect));
    }

    private void removeOldTarFile(File zipFile) throws SauceConnectException {
        File tarFile = new File(zipFile.getParentFile(), zipFile.getName().replaceAll(".gz", ""));
        removeFileIfExists(tarFile, "Unable to delete old tar");
    }

    /**
     * @param unArchiver  the unarchiver
     * @param archive     the compressed file to extract
     * @param destination the destination directory
     */
    private void extractArchive(AbstractUnArchiver unArchiver, File archive, File destination) {
        unArchiver.enableLogging(new ConsoleLogger(Logger.LEVEL_DEBUG, "Sauce"));
        unArchiver.setSourceFile(archive);
        unArchiver.setDestDirectory(destination);
        unArchiver.extract();
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
        return useLatestSauceConnect ? LATEST_SC_VERSION : CURRENT_SC_VERSION;
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

        //has --tunnel-identifer been specified?
        String tunnelIdentifier = getTunnelIdentifier(options, null);
        if (tunnelIdentifier != null) {
            fileName = MessageFormat.format("sc-{0}.log", tunnelIdentifier);
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
