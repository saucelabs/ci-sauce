package com.saucelabs.ci;

import com.sebuilder.interpreter.IO;
import com.sebuilder.interpreter.Script;
import com.sebuilder.interpreter.webdriverfactory.Remote;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
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

    private static final String URL = "https://{0}:{1}@{2}:{3}";

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
        //todo if any variable is null, return false

        config.put("browser", browser);
        config.put("version", version);
        config.put("os", os);
        config.put("name", scriptFile.getName());
        config.put("url", MessageFormat.format(URL, username, accessKey, host, port));

        BufferedReader br = null;
        try {
            Script script = IO.read(br = new BufferedReader(new InputStreamReader(new FileInputStream(scriptFile), "UTF-8")));
            Log log = LogFactory.getFactory().getInstance(SeleniumBuilderManager.class);
            return script.run(log, new Remote(), config);
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

}
