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

/**
 * @author Ross Rowe
 */
public class BrowserTest  {
    private static final String JOB_DETAILS_URL = "http://saucelabs.com/rest/v1/%1$s/jobs?full=true";
    private SauceREST sauceREST;

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
        String browserText = IOUtils.toString(getClass().getResourceAsStream("/webdriver.json"));
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


}
