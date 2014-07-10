package com.saucelabs.ci;

import org.openqa.selenium.Platform;

import java.util.Locale;


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
    private String device;
    private String deviceType;

    public Browser(String key, String os, String browserName, String version, String name) {
        this.key = key;
        this.os = os;
        this.browserName = browserName;
        this.version = version;
        this.name = name;
    }

    public String getKey() {
        return key;
    }

    public String getBrowserName() {
        return browserName;
    }

    public String getName() {
        return name;
    }

    public String getOs() {
        return os;
    }

    public String getVersion() {
        return version;
    }

    public Platform getPlatform() {
        //convert the operating system into the Platform enum
        if (os.toLowerCase(Locale.getDefault()).contains("windows 2008")) {

            return Platform.VISTA;
        } else if (os.toLowerCase(Locale.getDefault()).contains("windows 2012 r2")) {

            return Platform.WIN8_1;
        } else if (os.toLowerCase(Locale.getDefault()).contains("windows 2012")) {

            return Platform.WIN8;
        } else if (os.toLowerCase(Locale.getDefault()).contains("windows 2003")) {

            return Platform.XP;
        }
        //otherwise just return the os
        return Platform.extractFromSysProperty(os);
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
}
