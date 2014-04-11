package com.saucelabs.ci.sauceconnect;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.plexus.archiver.tar.TarGZipUnArchiver;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;

import java.io.*;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.logging.Level;

/**
 * @author Ross Rowe
 */
public class SauceConnectFourManager extends AbstractSauceTunnelManager implements SauceTunnelManager {

    private static final String SAUCE_CONNECT_4_STARTED = "Sauce Connect is up, you may start your tests";

    public enum OperatingSystem {
        OSX(OSX_DIR, OSX_FILE),
        WINDOWS(WINDOWS_DIR, WINDOWS_FILE, "bin\\sc.exe"),
        LINUX(LINUX_DIR, LINUX_FILE);
        private final String directory;
        private final String fileName;
        private final String executable;

        OperatingSystem(String directory, String fileName, String executable) {
            this.directory = directory;
            this.fileName = fileName;
            this.executable = executable;
        }

        OperatingSystem(String directory, String fileName) {
            this(directory, fileName, "bin/sc");
        }

        public static OperatingSystem getOperatingSystem() {
            String os = System.getProperty("os.name").toLowerCase();
            if (isWindows(os)) {
                return WINDOWS;
            } else if (isMac(os)) {
                return OSX;
            } else if (isUnix(os)) {
                return LINUX;
            }
            return null;
        }

        private static boolean isWindows(String os) {
            return (os.indexOf("win") >= 0);
        }

        private static boolean isMac(String os) {
            return (os.indexOf("mac") >= 0);
        }

        private static boolean isUnix(String os) {
            return (os.indexOf("nux") >= 0);
        }

        private String getDirectory() {
            return directory;
        }

        private String getFileName() {
            return fileName;
        }

        public String getExecutable() {
            return executable;
        }
    }


    public static final String SAUCE_CONNECT_4 = "sc-4.1";
    private static final String OSX_DIR = SAUCE_CONNECT_4 + "-osx";
    private static final String WINDOWS_DIR = SAUCE_CONNECT_4 + "-win";
    private static final String LINUX_DIR = SAUCE_CONNECT_4 + "-linux";
    private static final String BASE_FILE_NAME = SAUCE_CONNECT_4 + "-latest-";
    private static final String LINUX_FILE = BASE_FILE_NAME + "linux.tar.gz";
    private static final String OSX_FILE = BASE_FILE_NAME + "osx.zip";
    private static final String WINDOWS_FILE = BASE_FILE_NAME + "win32.zip";

    public SauceConnectFourManager() {
        this(false);
    }

    public SauceConnectFourManager(boolean quietMode) {
        super(quietMode);
    }

    @Override
    protected ProcessBuilder createProcessBuilder(String username, String apiKey, int port, File sauceConnectJar, String options, String httpsProtocol, PrintStream printStream) throws URISyntaxException, IOException {

        //find zip file to extract
        File workingDirectory = null;
        if (sauceConnectJar != null && sauceConnectJar.exists()) {
            workingDirectory = sauceConnectJar.getParentFile();
        }
        if (workingDirectory == null) {
            workingDirectory = new File(getSauceConnectWorkingDirectory());
        }
        OperatingSystem operatingSystem = OperatingSystem.getOperatingSystem();
        if (operatingSystem == null) {
            //TODO log an error
            return null;
        }
        extractZipFile(workingDirectory, operatingSystem);
        File unzipDirectory = new File(workingDirectory, operatingSystem.getDirectory());
        String[] args = new String[]{operatingSystem.getExecutable()};
        args = generateSauceConnectArgs(args, username, apiKey, port, options);

        ProcessBuilder processBuilder = new ProcessBuilder(args);
        processBuilder.directory(unzipDirectory);
        julLogger.log(Level.INFO, "Launching Sauce Connect " + Arrays.toString(args));

        return processBuilder;
    }

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

    public File extractZipFile(File workingDirectory, OperatingSystem operatingSystem) throws IOException {

        File zipFile = extractFile(workingDirectory, operatingSystem.getFileName());
        if (operatingSystem.equals(OperatingSystem.OSX) | operatingSystem.equals(OperatingSystem.WINDOWS)) {
            unzipFile(zipFile, workingDirectory);
        } else if (operatingSystem.equals(OperatingSystem.LINUX)) {
            untarGzFile(zipFile, workingDirectory);
        }
        return zipFile;
    }

    private void untarGzFile(File zipFile, File destination) {
        final TarGZipUnArchiver unArchiver = new TarGZipUnArchiver();
        unArchiver.enableLogging(new ConsoleLogger(Logger.LEVEL_DEBUG, "Sauce"));
        unArchiver.setSourceFile(zipFile);
        unArchiver.setDestDirectory(destination);
        unArchiver.extract();

    }

    private void unzipFile(File zipFile, File destination) {
        final ZipUnArchiver unArchiver = new ZipUnArchiver();
        unArchiver.enableLogging(new ConsoleLogger(Logger.LEVEL_DEBUG, "Sauce"));
        unArchiver.setSourceFile(zipFile);
        unArchiver.setDestDirectory(destination);
        unArchiver.extract();
    }

    private File extractFile(File workingDirectory, String fileName) throws IOException {

        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName);
        //copy input stream to a file
        File destination = new File(workingDirectory, fileName);
        FileOutputStream outputStream = new FileOutputStream(destination);
        IOUtils.copy(inputStream, outputStream);
        return destination;
    }

    protected String getSauceStartedMessage() {
        return SAUCE_CONNECT_4_STARTED;
    }
}
