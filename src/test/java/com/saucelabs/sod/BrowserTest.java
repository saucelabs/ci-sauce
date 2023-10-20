package com.saucelabs.sod;

import com.saucelabs.ci.Browser;
import com.saucelabs.ci.BrowserFactory;
import com.saucelabs.saucerest.SauceREST;
import com.saucelabs.saucerest.api.PlatformEndpoint;
import com.saucelabs.saucerest.model.platform.Platform;
import com.saucelabs.saucerest.model.platform.SupportedPlatforms;
import org.junit.Test;
import org.junit.Before;

import java.util.ArrayList;
import java.util.List;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Ross Rowe
 */
public class BrowserTest {
  private BrowserFactory browserFactory;

  @Before
  public void setUp() throws Exception {

    // Mock appium response
    List<Platform> appiumPlatforms = new ArrayList<Platform>();

    Platform kindle =
        new Platform(
            "2.3",
            "Amazon Kindle Fire Emulator",
            "android",
            "2.3.7.",
            "2.3.7.",
            "appium",
            "android",
            null,
            null,
            null,
            "Amazon Kindle Fire Emulator");
    Platform kindle2 =
        new Platform(
            "2.4",
            "Amazon Kindle Fire Emulator",
            "android",
            "2.4.7.",
            "2.4.7.",
            "appium",
            "android",
            null,
            null,
            null,
            "Amazon Kindle Fire Emulator");

    appiumPlatforms.add(kindle);

    // Mock webdriver response
    List<Platform> webdriverPlatforms = new ArrayList<Platform>();
    Platform kindleHD =
        new Platform(
            "4.0",
            "Amazon Kindle Fire HD 8.9 Emulator",
            "android",
            "4.0.4.",
            "4.0.4.",
            "appium",
            "android",
            null,
            null,
            null,
            "Amazon Kindle Fire HD 8.9 Emulator");
    Platform linux =
        new Platform(
            "4", "Firefox", "firefox", "4.0.1.", null, null, "Linux", null, null, null, null);
    webdriverPlatforms.add(kindleHD);
    webdriverPlatforms.add(linux);

    SauceREST sauceREST = mock(SauceREST.class);
    PlatformEndpoint mockPlatform = mock(PlatformEndpoint.class);

    when(sauceREST.getPlatformEndpoint()).thenReturn(mockPlatform);

    when(mockPlatform.getSupportedPlatforms("appium"))
        .thenReturn(new SupportedPlatforms(appiumPlatforms));

    when(mockPlatform.getSupportedPlatforms("webdriver"))
        .thenReturn(new SupportedPlatforms(webdriverPlatforms));

    this.browserFactory = new BrowserFactory(sauceREST);
  }

  @Test
  public void testNames() {
    Browser browser = new Browser(null, null, null, null, null, null, "Windows 2008");
    assertEquals("windows 2008 is really windows 7", browser.getName(), "Windows 7");
    browser = new Browser(null, null, null, null, null, null, "Windows 2003");
    assertEquals("windows 2003 is really windows xp", browser.getName(), "Windows XP");
  }

  @Test
  public void browserList() throws Exception {

    List<Platform> appiumPlatforms = new ArrayList<Platform>();

    Platform kindle =
        new Platform(
            "2.3",
            "Amazon Kindle Fire Emulator",
            "android",
            "2.3.7.",
            "2.3.7.",
            "appium",
            "android",
            null,
            null,
            null,
            "Amazon Kindle Fire Emulator");

    appiumPlatforms.add(kindle);

    List<Browser> browsers = browserFactory.getBrowserListFromPlatforms(appiumPlatforms);
    assertFalse("browsers is empty", browsers.isEmpty());
  }

  @Test
  public void browserFromSaucelabs() throws IOException {
    List<Browser> browsers = browserFactory.getWebDriverBrowsers();
    assertFalse("browsers is empty", browsers.isEmpty());
    browsers = browserFactory.getAppiumBrowsers();
    assertFalse("browsers is empty", browsers.isEmpty());
  }

  @Test
  public void copyBrowser() {
    Browser orig =
        new Browser(
            "thisismykey",
            "Windows 2008",
            "Firefox",
            "Mozilla Firefox",
            "47.0.12345",
            "47",
            "firefox");
    Browser browser = new Browser(orig, false);
    Browser latest = new Browser(orig, true);

    assertNotEquals(orig.getKey(), browser.getKey());
    assertNotEquals(orig.getKey(), latest.getKey());
    assertNotEquals(browser.getKey(), latest.getKey());
    assertEquals(latest.getVersion(), "latest");
  }
}
