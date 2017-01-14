package com.saucelabs.sod;

import com.saucelabs.ci.Browser;
import com.saucelabs.ci.BrowserFactory;
import com.saucelabs.saucerest.SauceREST;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

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
    public void testNames() throws Exception {
        Browser browser = new Browser(null, null, null, null, null, null, "Windows 2008");
        assertEquals("windows 2008 is really windows 7", browser.getName(), "Windows 7");
        browser = new Browser(null, null, null, null, null, null, "Windows 2003");
        assertEquals("windows 2003 is really windows xp", browser.getName(), "Windows XP");
    }

    @Test
    public void browserList() throws Exception {

        BrowserFactory factory = new BrowserFactory(sauceREST);
        String browserText = IOUtils.toString(getClass().getResourceAsStream("/appium_browsers.json"), "UTF-8");
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
