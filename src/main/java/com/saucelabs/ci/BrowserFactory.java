/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.saucelabs.ci;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles invoking the Sauce REST API to retrieve the list of valid Browsers.  The list of browser is cached for
 * an hour.
 *
 * @author Ross Rowe
 */
public class BrowserFactory {

    private static final Logger logger = Logger.getLogger(BrowserFactory.class);

    public static final String BROWSER_URL = "http://saucelabs.com/rest/v1/info/browsers";

    private Map<String, Browser> seleniumLookup = new HashMap<String, Browser>();
    private Map<String, Browser> webDriverLookup = new HashMap<String, Browser>();
    protected Timestamp lastLookup = null;
    private static final String IEHTA = "iehta";
    private static final String CHROME = "chrome";
    private static BrowserFactory instance;

    public BrowserFactory() {
        try {
            initializeSeleniumBrowsers();
            initializeWebDriverBrowsers();
        } catch (IOException e) {
            //TODO exception could mean we're behind firewall
            logger.error("Error retrieving browsers, attempting to continue", e);
        } catch (JSONException e) {
            logger.error("Error retrieving browsers, attempting to continue", e);
        }
    }

    public List<Browser> getSeleniumBrowsers() throws IOException, JSONException {
        List<Browser> browsers;
        if (shouldRetrieveBrowsers()) {
            browsers = initializeSeleniumBrowsers();
        } else {
            browsers = new ArrayList<Browser>(seleniumLookup.values());
        }
        Collections.sort(browsers);

        return browsers;
    }

    public List<Browser> getWebDriverBrowsers() throws IOException, JSONException {
        List<Browser> browsers;
        if (shouldRetrieveBrowsers()) {
            browsers = initializeWebDriverBrowsers();
        } else {
            browsers = new ArrayList<Browser>(webDriverLookup.values());
        }
        Collections.sort(browsers);

        return browsers;
    }

    public boolean shouldRetrieveBrowsers() {
        return lastLookup == null;
    }

    private List<Browser> initializeSeleniumBrowsers() throws IOException, JSONException {
        List<Browser> browsers = getSeleniumBrowsersFromSauceLabs();
        seleniumLookup = new HashMap<String, Browser>();
        for (Browser browser : browsers) {
            seleniumLookup.put(browser.getKey(), browser);
        }
        lastLookup = new Timestamp(new Date().getTime());
        return browsers;
    }

    private List<Browser> initializeWebDriverBrowsers() throws IOException, JSONException {
        List<Browser> browsers = getWebDriverBrowsersFromSauceLabs();
        webDriverLookup = new HashMap<String, Browser>();
        for (Browser browser : browsers) {
            webDriverLookup.put(browser.getKey(), browser);
        }
        lastLookup = new Timestamp(new Date().getTime());
        return browsers;
    }

    private List<Browser> getSeleniumBrowsersFromSauceLabs() throws IOException, JSONException {
        String response = getSauceAPIFactory().doREST(BROWSER_URL + "/selenium-rc");
        List<Browser> browsers = getBrowserListFromJson(response);
        List<Browser> toRemove = new ArrayList<Browser>();
        for (Browser browser : browsers) {
            if (browser.getBrowserName().equals(CHROME)) {
                toRemove.add(browser);
            }
        }
        for (Browser browser : toRemove) {
            browsers.remove(browser);
        }
        return browsers;
    }

    private List<Browser> getWebDriverBrowsersFromSauceLabs() throws IOException, JSONException {
        String response = getSauceAPIFactory().doREST(BROWSER_URL + "/webdriver");
        return getBrowserListFromJson(response);
    }

    public SauceFactory getSauceAPIFactory() {
        return new SauceFactory();
    }

    /**
     * Parses the JSON response and constructs a List of {@link Browser} instances.
     *
     * @param browserListJson
     * @return
     * @throws JSONException
     */
    public List<Browser> getBrowserListFromJson(String browserListJson) throws JSONException {
        List<Browser> browsers = new ArrayList<Browser>();

        JSONArray browserArray = new JSONArray(browserListJson);
        for (int i = 0; i < browserArray.length(); i++) {
            JSONObject browserObject = browserArray.getJSONObject(i);
            String seleniumName = browserObject.getString("api_name");
            if (seleniumName.equals(IEHTA)) {
                //exclude these browsers from the list, as they replicate iexplore and firefox
                continue;
            }
            String longName = browserObject.getString("long_name");
            String longVersion = browserObject.getString("long_version");
            String osName = browserObject.getString("os");
            String shortVersion = browserObject.getString("short_version");
            String browserKey = osName + seleniumName + shortVersion;
            String label = osName + " " + longName + " " + longVersion;
            browsers.add(new Browser(browserKey, osName, seleniumName, shortVersion, label));
        }
        return browsers;
    }

    public Browser seleniumBrowserForKey(String key) {
        return seleniumLookup.get(key);
    }

    public Browser webDriverBrowserForKey(String key) {
        return webDriverLookup.get(key);
    }

    /**
     * Returns a singleton instance of SauceFactory.  This is required because
     * remote agents don't have the Bamboo component plugin available, so the Spring
     * auto-wiring doesn't work.
     *
     * @return
     */
    public static BrowserFactory getInstance() {
        if (instance == null) {
            instance = new BrowserFactory();
        }
        return instance;
    }

}
