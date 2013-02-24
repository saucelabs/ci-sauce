package com.saucelabs.ci;

import com.sebuilder.interpreter.IO;
import com.sebuilder.interpreter.Script;
import com.sebuilder.interpreter.SeInterpreter;
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
import java.util.Map;
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
     *
     * @param scriptFile
     * @param envVars
     * @param printStream
     * @return
     */
    public boolean executeSeleniumBuilder(File scriptFile, Map envVars, PrintStream printStream) {

        //add environment variables set by plugin to array
        HashMap<String, String> config = new HashMap<String, String>();
        String browser = readPropertyOrEnv("SELENIUM_BROWSER", envVars, null);
        String version = readPropertyOrEnv("SELENIUM_VERSION", envVars, null);
        String os = readPropertyOrEnv("SELENIUM_PLATFORM", envVars, null);
        String host = readPropertyOrEnv("SELENIUM_HOST", envVars, null);
        String port = readPropertyOrEnv("SELENIUM_PORT", envVars, null);
        String username = readPropertyOrEnv("SAUCE_USER_NAME", envVars, null);
        String accessKey = readPropertyOrEnv("SAUCE_API_KEY", envVars, null);
        //todo if any variable is null, run against Firefox

        Platform platform = Platform.extractFromSysProperty(os);
        config.put("browserName", browser);
        config.put("version", version);
        config.put("platform", platform.toString());
        config.put("name", scriptFile.getName());
        config.put("url", MessageFormat.format(URL, username, accessKey, host, port));

        BufferedReader br = null;
        try {
            Script script = IO.read(br = new BufferedReader(new InputStreamReader(new FileInputStream(scriptFile), "UTF-8")));
            Log log = LogFactory.getFactory().getInstance(SeInterpreter.class);
            printStream.println("Starting to run selenium builder command");
            return script.run(new PrintStreamLogger(log, printStream), createRemoteDriver(config.get("url"), log), config);
        } catch (Exception e) {
            printStream.println("Error running selenium builder command");
            SeleniumBuilderManager.logger.log(Level.WARNING, "Error running selenium builder command", e);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    private WebDriverFactory createRemoteDriver(String url, Log log) {
        if (url.equals("")) {
            //run against firefox
            return new Firefox();
        } else {
            return new SauceRemoteDriver(log);
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
            //if (log.isDebugEnabled()) {
                printStream.println(o);
            //}
        }

        public void debug(Object o, Throwable throwable) {
            log.debug(o, throwable);
            //if (log.isDebugEnabled()) {
                printStream.println(o);
            //}
        }

        public void info(Object o) {
            log.info(o);
            if (log.isInfoEnabled()) {
                printStream.println(o);
            }
        }

        public void info(Object o, Throwable throwable) {
            log.info(o, throwable);
            if (log.isInfoEnabled()) {
                printStream.println(o);
            }
        }

        public void warn(Object o) {
            log.warn(o);
            if (log.isWarnEnabled()) {
                printStream.println(o);
            }
        }

        public void warn(Object o, Throwable throwable) {
            log.warn(o, throwable);
            if (log.isWarnEnabled()) {
                printStream.println(o);
            }
        }

        public void error(Object o) {
            log.error(o);
            if (log.isErrorEnabled()) {
                printStream.println(o);
            }
        }

        public void error(Object o, Throwable throwable) {
            log.error(o, throwable);
            if (log.isErrorEnabled()) {
                printStream.println(o);
            }
        }

        public void fatal(Object o) {
            log.fatal(o);
            if (log.isFatalEnabled()) {
                printStream.println(o);
            }
        }

        public void fatal(Object o, Throwable throwable) {
            log.fatal(o, throwable);
            if (log.isFatalEnabled()) {
                printStream.println(o);
            }
        }
    }

    /**
     * {@link Remote} subclass which prints a log message identifying the Sauce session id, which will be
     * picked up by the Sauce CI plugin.
     */
    private class SauceRemoteDriver extends Remote {

        private Log log;

        public SauceRemoteDriver(Log log) {
            this.log = log;
        }

        @Override
        public RemoteWebDriver make(HashMap<String, String> config) throws MalformedURLException {
            RemoteWebDriver driver = super.make(config);
            log.info(MessageFormat.format("SauceOnDemandSessionID={0} job-name={1}", driver.getSessionId(), config.get("name")));
            return driver;
        }
    }
}
