package com.saucelabs.sod;

import com.saucelabs.ci.Browser;
import com.saucelabs.ci.BrowserFactory;
import com.saucelabs.saucerest.SauceREST;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.openqa.selenium.Platform;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

/**
 * @author Ross Rowe
 */
public class BrowserTest  {
    private SauceREST sauceREST = new SauceREST(null, null);

    @Test
    public void osNames() throws Exception {
        Browser browser = new Browser(null, "Windows 2008", null, null, null, null, null);
        assertEquals("Platform is not Windows", browser.getPlatform(), Platform.VISTA);
        browser = new Browser(null, "Windows 2003", null, null, null, null, null);
        assertEquals("Platform is not Windows", browser.getPlatform(), Platform.XP);
        browser = new Browser(null, "Linux", null, null, null, null, null);
        assertEquals("Platform is not Linux", browser.getPlatform(), Platform.LINUX);
    }

    @Test
    public void browserList() throws Exception {

        BrowserFactory factory = new BrowserFactory(sauceREST);
        String browserText = IOUtils.toString(getClass().getResourceAsStream("/appium_browsers.json"));
        List<Browser> browsers = factory.getBrowserListFromJson(browserText);
        assertFalse("browsers is empty", browsers.isEmpty());

    }

    @Test
    public void browserFromSaucelabs() throws Exception {
        BrowserFactory factory = new BrowserFactory(sauceREST);
        List<Browser> browsers = factory.getWebDriverBrowsers();
        assertFalse("browsers is empty", browsers.isEmpty());
        browsers = factory.getAppiumBrowsers();
        assertFalse("browsers is empty", browsers.isEmpty());
    }


    @Test
    public void copyBrowser() throws Exception {
        Browser orig = new Browser("thisismykey", "Windows 2008", "Firefox", "Mozilla Firefox", "47.0.12345", "47", "firefox");
        Browser browser = new Browser(orig, false);
        Browser latest = new Browser(orig, true);

        assertNotEquals(orig.getKey(), browser.getKey());
        assertNotEquals(orig.getKey(), latest.getKey());
        assertNotEquals(browser.getKey(), latest.getKey());
        assertEquals(latest.getVersion(), "latest");
    }

}
