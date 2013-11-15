package com.saucelabs.ci;

import com.sebuilder.interpreter.IO;
import com.sebuilder.interpreter.Script;
import com.sebuilder.interpreter.SeInterpreter;
import com.sebuilder.interpreter.SuiteException;
import com.sebuilder.interpreter.webdriverfactory.Firefox;
import com.sebuilder.interpreter.webdriverfactory.Remote;
import com.sebuilder.interpreter.webdriverfactory.WebDriverFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openqa.selenium.Platform;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.io.*;
import java.net.MalformedURLException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides integration with the {@link com.sebuilder.interpreter.SeInterpreter} class for running
 * JSON-based Selenium Builder scripts
 *
 * @author Ross Rowe
 */
public class SeleniumBuilderManager {

    private static final Logger logger = Logger.getLogger(SeleniumBuilderManager.class.getName());

    /**
     * TODO always use wd/hub?
     */
    private static final String URL = "http://{0}:{1}@{2}:{3}/wd/hub";

    /**
     * Creates and invokes a new {@link Script} instance for the given <code>scriptFile</code>.
     *
     * @param scriptFile  The se-builder script file to invoke
     * @param envVars     Contains the available environment variables set by the CI environment.  The Sauce CI
     *                    plugin will set envvars that contain information about the browser/user/url to use.
     * @param printStream Where messages will be logged to
     * @return boolean indicating whether the invocation of the se-builder script was successful
     */
    public boolean executeSeleniumBuilder(File scriptFile, Map envVars, PrintStream printStream) {

        HashMap<String, String> config = new HashMap<String, String>();
        String browser = readPropertyOrEnv("SELENIUM_BROWSER", envVars, null);
        String version = readPropertyOrEnv("SELENIUM_VERSION", envVars, null);
        String os = readPropertyOrEnv("SELENIUM_PLATFORM", envVars, null);
        //TODO hack: Win8 is being returned as Windows 2012
        if (os != null && os.equals("Windows 2012")) {
            os = "windows 8";
        }
        String host = readPropertyOrEnv("SELENIUM_HOST", envVars, null);
        String port = readPropertyOrEnv("SELENIUM_PORT", envVars, null);
        String username = readPropertyOrEnv("SAUCE_USER_NAME", envVars, null);
        String accessKey = readPropertyOrEnv("SAUCE_API_KEY", envVars, null);

        if (browser != null) {
            config.put("browserName", browser);
        }
        if (version != null) {
            config.put("version", version);
        }
        if (os != null) {
            Platform platform = Platform.extractFromSysProperty(os);
            config.put("platform", platform.toString());
        }
        config.put("name", scriptFile.getName());
        if (host != null && accessKey != null && username != null && port != null)
            config.put("url", MessageFormat.format(URL, username, accessKey, host, port));

        Log log = LogFactory.getFactory().getInstance(SeInterpreter.class);
        BufferedReader br = null;
        try {
            Script script = IO.read(br = new BufferedReader(new InputStreamReader(new FileInputStream(scriptFile), "UTF-8")));

            printStream.println("Starting to run selenium builder command");
            script.run(new PrintStreamLogger(log, printStream), createRemoteDriver(config.get("url"), printStream), config);
            return true;
        } catch (IO.SuiteException e) {

            boolean result = true;
            for (String path : e.getPaths()) {
                BufferedReader reader = null;
                try {
                    Script script = IO.read(reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(scriptFile.getParentFile(), path)), "UTF-8")));
                    result = result || script.run(new PrintStreamLogger(log, printStream), createRemoteDriver(config.get("url"), printStream), config);
                } catch (Exception e1) {
                    result = false;
                    printStream.println("Error running selenium builder command");
                    SeleniumBuilderManager.logger.log(Level.WARNING, "Error running selenium builder command", e1);
                } finally {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e1) {
                            //ignore
                        }
                    }
                }

            }
            return result;
        } catch (Exception e) {
            printStream.println("Error running selenium builder command");
            SeleniumBuilderManager.logger.log(Level.WARNING, "Error running selenium builder command", e);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    SeleniumBuilderManager.logger.log(Level.WARNING, "Error closing script file", e);
                }
            }
        }
        return false;
    }

    /**
     * Returns the {@link WebDriverFactory} instance to be used to run the script against.
     *
     * @param url
     * @return
     */
    private WebDriverFactory createRemoteDriver(String url, PrintStream printStream) {
        if (url == null || url.equals("")) {
            //run against firefox
            return new Firefox();
        } else {
            return new SauceRemoteDriver(printStream);
        }
    }

    private String readPropertyOrEnv(String key, Map vars, String defaultValue) {
        String v = (String) vars.get(key);
        if (v == null)
            v = System.getProperty(key);
        if (v == null)
            v = System.getenv(key);
        if (v == null)
            v = defaultValue;
        return v;
    }

    /**
     * {@link Log} implementation which logs messages to the underlying Logger as well as to the PrintStream.
     */
    private class PrintStreamLogger implements Log {

        private Log log;
        private PrintStream printStream;

        public PrintStreamLogger(Log log, PrintStream printStream) {
            this.log = log;
            this.printStream = printStream;
        }

        public boolean isDebugEnabled() {
            return log.isDebugEnabled();
        }

        public boolean isErrorEnabled() {
            return log.isErrorEnabled();
        }

        public boolean isFatalEnabled() {
            return log.isFatalEnabled();
        }

        public boolean isInfoEnabled() {
            return log.isInfoEnabled();
        }

        public boolean isTraceEnabled() {
            return log.isTraceEnabled();
        }

        public boolean isWarnEnabled() {
            return log.isTraceEnabled();
        }

        public void trace(Object o) {
            log.trace(o);
            if (log.isTraceEnabled()) {
                printStream.println(o);
            }
        }

        public void trace(Object o, Throwable throwable) {
            log.trace(o, throwable);
            if (log.isTraceEnabled()) {
                printStream.println(o);
            }
        }

        public void debug(Object o) {
            log.debug(o);
            printStream.println(o);

        }

        public void debug(Object o, Throwable throwable) {
            log.debug(o, throwable);
            printStream.println(o);

        }

        public void info(Object o) {
            log.info(o);
            printStream.println(o);

        }

        public void info(Object o, Throwable throwable) {
            log.info(o, throwable);
            printStream.println(o);

        }

        public void warn(Object o) {
            log.warn(o);
            printStream.println(o);

        }

        public void warn(Object o, Throwable throwable) {
            log.warn(o, throwable);
            printStream.println(o);

        }

        public void error(Object o) {
            log.error(o);
            printStream.println(o);

        }

        public void error(Object o, Throwable throwable) {
            log.error(o, throwable);
            printStream.println(o);

        }

        public void fatal(Object o) {
            log.fatal(o);
            printStream.println(o);

        }

        public void fatal(Object o, Throwable throwable) {
            log.fatal(o, throwable);
            printStream.println(o);

        }
    }

    /**
     * {@link Remote} subclass which prints a log message identifying the Sauce session id, which will be
     * picked up by the Sauce CI plugin.
     */
    private class SauceRemoteDriver extends Remote {

        private PrintStream printStream;

        public SauceRemoteDriver(PrintStream printStream) {
            this.printStream = printStream;
        }

        @Override
        public RemoteWebDriver make(HashMap<String, String> config) throws MalformedURLException {
            RemoteWebDriver driver = super.make(config);
            driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
            printStream.println(MessageFormat.format("SauceOnDemandSessionID={0} job-name={1}", driver.getSessionId(), config.get("name")));
            return driver;
        }
    }
}
