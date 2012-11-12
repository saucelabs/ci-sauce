package com.saucelabs.ci;

import com.sebuilder.interpreter.SeInterpreter;

import java.util.Arrays;

/**
 * Provides integration with the {@link com.sebuilder.interpreter.SeInterpreter} class for running
 * JSON-based Selenium Builder scripts
 *
 * @author Ross Rowe
 */
public class SeleniumBuilderManager {

    private static final String REMOTE = "remote";
    private static final String FIREFOX = "firefox";

    public void executeSeleniumBuilder(boolean isRemote, String scriptPath) {

        //set driver to either Firefox or Remote
        String driverType = isRemote ? REMOTE : FIREFOX;
        String[] args = new String[]{"--driver", driverType};
        //add environment variables set by plugin to array
        String browser = readPropertyOrEnv("SELENIUM_BROWSER", null);
        String version = readPropertyOrEnv("SELENIUM_VERSION", null);
        String os = readPropertyOrEnv("SELENIUM_PLATFORM", null);
        if (browser != null) {
           args = concat(args, new String[]{"--driver.browser", browser});
        }if (version != null) {
           args = concat(args, new String[]{"--driver.version", version});
        }
        if (os != null) {
            args = concat(args, new String[]{"--driver.os", os});
        }
        //TODO set selenium host (ondemand.saucelabs.com or localhost:4445)

        //TODO set username/access key

        SeInterpreter.main(concat(args, new String[]{scriptPath}));
    }

    private String readPropertyOrEnv(String key, String defaultValue) {
        String v = System.getProperty(key);
        if (v == null)
            v = System.getenv(key);
        if (v == null)
            v = defaultValue;
        return v;
    }

    public <T> T[] concat(T[] first, T[] second) {
        T[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }


}
