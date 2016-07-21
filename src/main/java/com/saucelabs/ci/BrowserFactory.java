/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.saucelabs.ci;

import com.saucelabs.saucerest.SauceREST;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles invoking the Sauce REST API to retrieve the list of valid Browsers.  The list of browser is cached for
 * an hour.
 *
 * @author Ross Rowe
 */
public class BrowserFactory {

    private static final Logger logger = Logger.getLogger(BrowserFactory.class.getName());

    public static final int ONE_HOUR_IN_MILLIS = 1000 * 60 * 60;

    private SauceREST sauceREST;

    private Map<String, Browser> seleniumLookup = new HashMap<String, Browser>();
    private Map<String, Browser> appiumLookup = new HashMap<String, Browser>();
    private Map<String, Browser> webDriverLookup = new HashMap<String, Browser>();
    protected Timestamp lastLookup = null;
    private static final String IEHTA = "iehta";
    private static final String CHROME = "chrome";
    private static BrowserFactory instance;

    public BrowserFactory() {
        this(null);
    }

    public BrowserFactory(SauceREST sauceREST) {
        if (sauceREST == null) {
            this.sauceREST = new SauceREST(null, null);
        } else {
            this.sauceREST = sauceREST;
        }
        try {
            initializeWebDriverBrowsers();
            initializeAppiumBrowsers();
        } catch (IOException e) {
            //TODO exception could mean we're behind firewall
            logger.log(Level.WARNING, "Error retrieving browsers, attempting to continue", e);
        } catch (JSONException e) {
            logger.log(Level.WARNING, "Error retrieving browsers, attempting to continue", e);
        }
    }

    public List<Browser> getAppiumBrowsers() throws IOException, JSONException {
        List<Browser> browsers;
        if (shouldRetrieveBrowsers()) {
            browsers = initializeAppiumBrowsers();
        } else {
            browsers = new ArrayList<Browser>(appiumLookup.values());
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
        return lastLookup == null || CacheTimeUtil.pastAcceptableDuration(lastLookup, ONE_HOUR_IN_MILLIS);
    }

    private List<Browser> initializeAppiumBrowsers() throws IOException, JSONException {
        List<Browser> browsers = getAppiumBrowsersFromSauceLabs();
        appiumLookup = new HashMap<String, Browser>();
        for (Browser browser : browsers) {
            appiumLookup.put(browser.getKey(), browser);
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

    private List<Browser> getWebDriverBrowsersFromSauceLabs() throws IOException, JSONException {
        String response = sauceREST.getSupportedPlatforms("webdriver");
        if (response.equals("")) {
            response = "[]";
        }
        return getBrowserListFromJson(response);
    }

    private List<Browser> getAppiumBrowsersFromSauceLabs() throws IOException, JSONException {
        String response = sauceREST.getSupportedPlatforms("appium");
        if (response.equals("")) {
            response = "[]";
        }
        return getBrowserListFromJson(response);
    }

    /**
     * Parses the JSON response and constructs a List of {@link Browser} instances.
     *
     * @param browserListJson JSON response with all browsers
     * @return List of browser objects
     * @throws JSONException Invalid JSON
     */
    public List<Browser> getBrowserListFromJson(String browserListJson) throws JSONException {
        List<Browser> browsers = new ArrayList<Browser>();
        HashMap<String, Boolean> latestBrowser = new HashMap();


        JSONArray browserArray = new JSONArray(browserListJson);
        for (int i = 0; i < browserArray.length(); i++) {
            JSONObject browserObject = browserArray.getJSONObject(i);
            String seleniumName = browserObject.getString("api_name");
            if (seleniumName.equals(IEHTA)) {
                //exclude these browsers from the list, as they replicate iexplore and firefox
                continue;
            }
            if (browserObject.has("device")) {
                //appium browser
                String longName = browserObject.getString("long_name");
                String longVersion = browserObject.getString("long_version");
                String osName = browserObject.getString("api_name"); //use api_name instead of os, as os was returning Linux/Mac OS
                String shortVersion = browserObject.getString("short_version");
                //set value used for device to be the long name (ie. if device value is 'Nexus7HD', then actually use 'Google Nexus 7 HD Emulator' ​
                String device = longName;

                String deviceType = null;

                if (browserObject.has("device-type")) {
                    deviceType = browserObject.getString("device-type");
                }
                //iOS devices should include 'Simulator' in the device name (not currently included in the Sauce REST API response.  The platform should also be set to iOS (as per instructions at https://docs.saucelabs.com/reference/platforms-configurator
                if (device.equalsIgnoreCase("ipad") || device.equalsIgnoreCase("iphone")) {
                    device = device + " Simulator";
                    osName = "iOS";
                    //JENKINS-29047 set the browserName to 'Safari'
                    seleniumName = "Safari";
                }
                Browser browser;
                if (latestBrowser.get("device_" + longName + "_" + longVersion) == null) {
                    browser = createDeviceBrowser(seleniumName, longName, "latest", osName, device, deviceType, "latest", "portrait");
                    browsers.add(browser);
                    browser = createDeviceBrowser(seleniumName, longName, "latest", osName, device, deviceType, "latest", "landscape");
                    browsers.add(browser);
                    latestBrowser.put("device_" + longName + "_" + longVersion, Boolean.TRUE);
                }
                browser = createDeviceBrowser(seleniumName, longName, longVersion, osName, device, deviceType, shortVersion, "portrait");
                browsers.add(browser);
                browser = createDeviceBrowser(seleniumName, longName, longVersion, osName, device, deviceType, shortVersion, "landscape");
                browsers.add(browser);


            } else {
                //webdriver/selenium browser
                String longName = browserObject.getString("long_name");
                String longVersion = browserObject.getString("long_version");
                String osName = browserObject.getString("os");
                String shortVersion = browserObject.getString("short_version");

                Browser browser;

                browser = createBrowserBrowser(seleniumName, longName, "latest", osName, "latest");
                if (latestBrowser.get(browser.getKey()) == null) {
                    browsers.add(browser);
                    latestBrowser.put(browser.getKey(), Boolean.TRUE);
                }
                browser = createBrowserBrowser(seleniumName, longName, longVersion, osName, shortVersion);
                browsers.add(browser);
            }
        }

        latestBrowser = null;
        return browsers;
    }

    private Browser createDeviceBrowser(String seleniumName, String longName, String longVersion, String osName, String device, String deviceType, String shortVersion, String orientation) {
        String browserKey = device + orientation + seleniumName + longVersion;
        //replace any spaces with _s
        browserKey = browserKey.replaceAll(" ", "_");
        //replace any . with _
        browserKey = browserKey.replaceAll("\\.", "_");
        StringBuilder label = new StringBuilder();
        label.append(longName).append(' ');
        if (deviceType != null) {
            label.append(deviceType).append(' ');
        }
        label.append(shortVersion);
        label.append(" (").append(orientation).append(')');
        //add browser for both landscape and portrait orientation
        Browser browser = new Browser(browserKey, osName, seleniumName, longName, shortVersion, longVersion, label.toString());
        browser.setDevice(device);
        browser.setDeviceType(deviceType);
        browser.setDeviceOrientation(orientation);
        return browser;
    }

    private Browser createBrowserBrowser(String seleniumName, String longName, String longVersion, String osName, String shortVersion) {
        String browserKey = osName + seleniumName + shortVersion;
        //replace any spaces with _s
        browserKey = browserKey.replaceAll(" ", "_");
        //replace any . with _
        browserKey = browserKey.replaceAll("\\.", "_");
        String label = OperatingSystemDescription.getOperatingSystemName(osName) + " " + longName + " " + shortVersion;
        return new Browser(browserKey, osName, seleniumName, longName, shortVersion, longVersion, label);
    }

    /**
     * Return the selenium rc browser which matches the key.
     *
     * @param key the key
     * @return the selenium rc browser which matches the key.
     */
    public Browser seleniumBrowserForKey(String key) {
        return seleniumLookup.get(key);
    }

    public Browser seleniumBrowserForKey(String key, boolean useLatestVersion) {
        Browser browser = webDriverBrowserForKey(key);
        if (useLatestVersion) {
            return getLatestSeleniumBrowserVersion(browser);
        } else {
            return browser;
        }
    }

    private Browser getLatestSeleniumBrowserVersion(Browser originalBrowser) {
        Browser candidateBrowser = originalBrowser;
        for (Browser browser : seleniumLookup.values()) {
            try {
                if (browser.getBrowserName().equals(originalBrowser.getBrowserName())
                        && browser.getOs().equals(originalBrowser.getOs())
                        && Integer.parseInt(browser.getLongVersion()) > Integer.parseInt(candidateBrowser.getLongVersion())) {
                    candidateBrowser = browser;
                }
            } catch (NumberFormatException e) {
                continue;
            }
        }
        return candidateBrowser;
    }

    /**
     * Return the web driver browser which matches the key.
     *
     * @param key the key
     * @return the web driver browser which matches the key.
     */
    public Browser webDriverBrowserForKey(String key) {
        return webDriverLookup.get(key);
    }

    public Browser webDriverBrowserForKey(String key, boolean useLatestVersion) {
        Browser browser = webDriverBrowserForKey(key);
        if (useLatestVersion) {
            return getLatestWebDriverBrowserVersion(browser);
        } else {
            return browser;
        }
    }

    private Browser getLatestWebDriverBrowserVersion(Browser originalBrowser) {
        Browser candidateBrowser = originalBrowser;
        for (Browser browser : webDriverLookup.values()) {

            try {
                if (browser.getBrowserName().equals(originalBrowser.getBrowserName())
                        && browser.getOs().equals(originalBrowser.getOs())
                        && Integer.parseInt(browser.getLongVersion()) > Integer.parseInt(candidateBrowser.getLongVersion())) {
                    candidateBrowser = browser;
                }
            } catch (NumberFormatException e) {
                continue;
            }
        }
        return candidateBrowser;
    }

    /**
     * Return the appium browser which matches the key.
     *
     * @param key the key
     * @return the appium browser which matches the key.
     */

    public Browser appiumBrowserForKey(String key) {
        return appiumLookup.get(key);
    }

    /**
     * Returns a singleton instance of SauceFactory.  This is required because
     * remote agents don't have the Bamboo component plugin available, so the Spring
     * auto-wiring doesn't work.
     *
     * @return the Browser Factory
     */
    public static BrowserFactory getInstance() {
        return getInstance(null);
    }

    public static BrowserFactory getInstance(SauceREST sauceREST) {
        if (instance == null) {
            instance = new BrowserFactory(sauceREST);
        }
        return instance;
    }

}
