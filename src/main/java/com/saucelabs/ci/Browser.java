package com.saucelabs.ci;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/**
 * Represents a Sauce Browser instance.
 *
 * @author <a href="http://www.sysbliss.com">Jonathan Doklovic</a>
 * @author Ross Rowe
 */
public class Browser implements Comparable<Browser> {

    private final String key;
    private final String os;
    private final String browserName;
    private final String version;
    private final String name;
    private final String longVersion;
    private final String longName;
    private String device;
    private String deviceType;
    private String deviceOrientation;

    private static final Map<String, String> oses;
    static {
        Map<String, String> aMap = new HashMap<String, String>();
        aMap.put("windows 10", "Windows 10");
        aMap.put("windows 2012 r2", "Windows 8.1");
        aMap.put("windows 2012", "Windows 8");
        aMap.put("windows 2008", "Windows 7");
        aMap.put("windows 2003", "Windows XP");
        aMap.put("linux", "Linux");
        aMap.put("mac 10.12", "macOS Sierra");
        aMap.put("mac 10.11", "OS X El Capitan");
        aMap.put("mac 10.10", "OS X Yosemite");
        aMap.put("mac 10.9", "OS X Mavericks");
        aMap.put("mac 10.8", "OS X Mountain Lion");

        oses = Collections.unmodifiableMap(aMap);
    }

    /**
     * Create a new browser object
     *
     * @param key unique key for the object (eg Windows_2003internet_explorer7)
     * @param os Operating System (eg Windows 2003)
     * @param browserName Browser Name (eg internet explorer)
     * @param longName Full Pretty browser name (eg Internet Explorer)
     * @param version Short versioon number (eg 7)
     * @param longVersion Full version number (eg 7.0.5730.13.)
     * @param name Full Pretty name (ie Windows eg Internet Explorer 7)
     */
    public Browser(String key, String os, String browserName, String longName, String version, String longVersion, String name) {
        this.key = key;
        this.os = os;
        this.browserName = browserName;
        this.longName = longName;
        this.version = version;
        this.longVersion = longVersion;
        this.name = name;
    }

    /**
     * Copy constructor
     * @see #Browser(String, String, String, String, String, String, String)
     *
     * @param original Original Browser Object
     * @param useLatest Replace version strings with "latest" to grab the latest of that browser
     */
    public Browser(Browser original, boolean useLatest) {
        this.key = null;
        this.os = original.os;
        this.browserName = original.browserName;
        this.name = original.name;
        if (useLatest) {
            this.version = "latest";
            this.longVersion = "latest";
        } else {
            this.version = original.version;
            this.longVersion = original.longVersion;
        }
        this.longName = original.longName;
        this.device = original.device;
        this.deviceType = original.deviceType;
        this.deviceOrientation = original.deviceOrientation;
    }

    public String getKey() {
        if (key == null) {
            /* New behavior, see BrowserFactory for the weird versions */
            String browserKey = os + "_" + device + "_" + deviceOrientation + "_" + name + "_" + longVersion;
            //replace any spaces with _s
            browserKey = browserKey.replaceAll(" ", "_");
            //replace any . with _
            browserKey = browserKey.replaceAll("\\.", "_");
            return browserKey;
        }
        return key;
    }

    public String getBrowserName() {
        return browserName;
    }

    public String getName() {
        String newName = name.toLowerCase();
        if (oses.containsKey(newName)) {
            return oses.get(newName);
        }
        return name;
    }

    public String getOs() {
        return os;
    }

    public String getVersion() {
        return version;
    }

    private static boolean isBetterMatch(String previous, String matcher) {
        return previous == null || matcher.length() >= previous.length();
    }

    public String getPlatform() {
        return os;
    }

    public boolean equals(Object object) {
        if (!(object instanceof Browser)) {
            return false;
        }
        Browser browser = (Browser) object;
        return (key == null ? browser.key == null : key.equals(browser.key)) &&
                (browserName == null ? browser.browserName == null : browserName.equals(browser.browserName)) &&
                (name == null ? browser.name == null : name.equals(browser.name)) &&
                (os == null ? browser.os == null : os.equals(browser.os)) &&
                (deviceType == null ? browser.deviceType == null : deviceType.equals(browser.deviceType)) &&
                (version == null ? browser.version == null : version.equals(browser.version));
    }

    public int hashCode() {
        int result = 17;
        if (key != null) {
            result = 31 * result + key.hashCode();
        }
        if (browserName != null) {
            result = 31 * result + browserName.hashCode();
        }
        if (name != null) {
            result = 31 * result + name.hashCode();
        }
        if (os != null) {
            result = 31 * result + os.hashCode();
        }
        if (version != null) {
            result = 31 * result + version.hashCode();
        }
        if (deviceType != null) {
            result = 31 * result + deviceType.hashCode();
        }
        return result;
    }

    public int compareTo(Browser browser) {
        return String.CASE_INSENSITIVE_ORDER.compare(name, browser.name);
    }

    public String toString() {
        if (name == null) {
            return super.toString();
        } else {
            return name;
        }
    }

    public String getUri() {
        return getUri(null, null);
    }

    public String getUri(String username, String accessKey) {
        StringBuilder builder = new StringBuilder();
        builder.append("sauce-ondemand:?os=").append(os).
                append("&browser=").append(browserName).
                append("&browser-version=").append(version);
        if (username != null) {
            builder.append("&username=").append(username);
        }
        if (accessKey != null) {
            builder.append("&access-key=").append(accessKey);
        }
        return builder.toString();
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public String getDevice() {
        return device;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceOrientation(String deviceOrientation) {
        this.deviceOrientation = deviceOrientation;
    }

    public String getDeviceOrientation() {
        return deviceOrientation;
    }

    public String getLongVersion() {
        return longVersion;
    }

    public String getLongName() {
        return longName;
    }


}
