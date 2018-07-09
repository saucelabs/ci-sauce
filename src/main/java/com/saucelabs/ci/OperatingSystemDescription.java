package com.saucelabs.ci;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by gavinmogan on 2015-12-17.
 */
enum OperatingSystemDescription {
    WINDOWS_10("Windows 2015", "Windows 10"),
    WINDOWS_8_1("Windows 2012 R2", "Windows 8.1"),
    WINDOWS_8("Windows 2012", "Windows 8"),
    WINDOWS_7("Windows 2008", "Windows 7"),
    WINDOWS_XP("Windows 2003", "Windows XP"),
    OSX_EL_CAPITAN("Mac 10.11", "OS X El Capitan"),
    OSX_YOSEMITE("Mac 10.10", "OS X Yosemite"),
    OSX_MAVERICKS("Mac 10.9", "OS X Mavericks"),
    OSX_MOUNTAIN_LION("Mac 10.8", "OS X Mountain Lion"),;
    private final String key;
    private final String description;

    private static final Map<String, String> descriptionMap = new HashMap<>();

    static {
        for (OperatingSystemDescription operatingSystemDescription : OperatingSystemDescription.values()) {
            descriptionMap.put(operatingSystemDescription.key, operatingSystemDescription.description);
        }
    }

    OperatingSystemDescription(String key, String description) {
        this.key = key;
        this.description = description;
    }


    public static String getDescription(String osName) {
        return descriptionMap.get(osName);
    }

    /**
     * The Sauce REST API returns the server operating system name (eg. Windows 2003) rather than the public name
     * (eg. Windows XP), so if we've defined a mapping for the description, we use that here.
     * @param osName
     * @return
     */
    public static String getOperatingSystemName(String osName) {
        String description = OperatingSystemDescription.getDescription(osName);
        if (description != null)
        {
            return description;
        }
        return osName;
    }
}
