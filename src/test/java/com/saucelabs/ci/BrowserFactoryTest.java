package com.saucelabs.ci;

import com.saucelabs.saucerest.SauceREST;
import com.saucelabs.saucerest.api.PlatformEndpoint;
import com.saucelabs.saucerest.model.platform.Platform;
import com.saucelabs.saucerest.model.platform.SupportedPlatforms;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BrowserFactoryTest {

  private BrowserFactory browserFactory;

  @BeforeEach
  void beforeEach() throws Exception {

    // Mock appium response
    List<Platform> appiumPlatforms = new ArrayList<>();

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
  void testGetAppiumBrowsers() throws IOException {
    List<Browser> browsers = this.browserFactory.getAppiumBrowsers();
    assertEquals(2, browsers.size());

    Browser browser1 = browsers.get(0);
    assertEquals("Amazon_Kindle_Fire_Emulatorlandscapeandroid2_3_7_", browser1.getKey());
    assertEquals("android", browser1.getOs());
    assertEquals("android", browser1.getBrowserName());
    assertEquals("2.3", browser1.getVersion());
    assertEquals("Amazon Kindle Fire Emulator 2.3 (landscape)", browser1.getName());
    assertEquals("2.3.7.", browser1.getLongVersion());
    assertEquals("Amazon Kindle Fire Emulator", browser1.getLongName());
    assertEquals("Amazon Kindle Fire Emulator", browser1.getDevice());
    assertNull(browser1.getDeviceType());
    assertEquals("landscape", browser1.getDeviceOrientation());

    Browser browser2 = browsers.get(1);
    assertEquals("Amazon_Kindle_Fire_Emulatorportraitandroid2_3_7_", browser2.getKey());
    assertEquals("android", browser2.getOs());
    assertEquals("android", browser2.getBrowserName());
    assertEquals("2.3", browser2.getVersion());
    assertEquals("Amazon Kindle Fire Emulator 2.3 (portrait)", browser2.getName());
    assertEquals("2.3.7.", browser2.getLongVersion());
    assertEquals("Amazon Kindle Fire Emulator", browser2.getLongName());
    assertEquals("Amazon Kindle Fire Emulator", browser2.getDevice());
    assertNull(browser2.getDeviceType());
    assertEquals("portrait", browser2.getDeviceOrientation());
  }

  @Test
  void testGetWebDriverBrowsers() throws IOException {
    List<Browser> browsers = this.browserFactory.getWebDriverBrowsers();
    assertEquals(4, browsers.size());

    Browser browser1 = browsers.get(0);
    assertEquals("Amazon_Kindle_Fire_HD_8_9_Emulatorlandscapeandroid4_0_4_", browser1.getKey());
    assertEquals("android", browser1.getOs());
    assertEquals("android", browser1.getBrowserName());
    assertEquals("4.0", browser1.getVersion());
    assertEquals("Amazon Kindle Fire HD 8.9 Emulator 4.0 (landscape)", browser1.getName());
    assertEquals("4.0.4.", browser1.getLongVersion());
    assertEquals("Amazon Kindle Fire HD 8.9 Emulator", browser1.getLongName());
    assertEquals("Amazon Kindle Fire HD 8.9 Emulator", browser1.getDevice());
    assertNull(browser1.getDeviceType());
    assertEquals("landscape", browser1.getDeviceOrientation());

    Browser browser2 = browsers.get(1);
    assertEquals("Amazon_Kindle_Fire_HD_8_9_Emulatorportraitandroid4_0_4_", browser2.getKey());
    assertEquals("android", browser2.getOs());
    assertEquals("android", browser2.getBrowserName());
    assertEquals("4.0", browser2.getVersion());
    assertEquals("Amazon Kindle Fire HD 8.9 Emulator 4.0 (portrait)", browser2.getName());
    assertEquals("4.0.4.", browser2.getLongVersion());
    assertEquals("Amazon Kindle Fire HD 8.9 Emulator", browser2.getLongName());
    assertEquals("Amazon Kindle Fire HD 8.9 Emulator", browser2.getDevice());
    assertNull(browser2.getDeviceType());
    assertEquals("portrait", browser2.getDeviceOrientation());

    Browser browser3 = browsers.get(2);
    assertEquals("Linuxfirefox4", browser3.getKey());
    assertEquals("Linux", browser3.getOs());
    assertEquals("firefox", browser3.getBrowserName());
    assertEquals("4", browser3.getVersion());
    assertEquals("Linux Firefox 4", browser3.getName());
    assertEquals("4.0.1.", browser3.getLongVersion());
    assertEquals("Firefox", browser3.getLongName());
    assertNull(browser3.getDevice());
    assertNull(browser3.getDeviceType());
    assertNull(browser3.getDeviceOrientation());

    Browser browser4 = browsers.get(3);
    assertEquals("Linuxfirefoxlatest", browser4.getKey());
    assertEquals("Linux", browser4.getOs());
    assertEquals("firefox", browser4.getBrowserName());
    assertEquals("latest", browser4.getVersion());
    assertEquals("Linux Firefox latest", browser4.getName());
    assertEquals("latest", browser4.getLongVersion());
    assertEquals("Firefox", browser4.getLongName());
    assertNull(browser4.getDevice());
    assertNull(browser4.getDeviceType());
    assertNull(browser4.getDeviceOrientation());
  }

  @Test
  void testDuplicateMobileDevice() {
    String obj =
        "{\"deprecated_backend_versions\": [\"1.4.13\"], \"short_version\": \"4.4\", \"long_name\": \"Amazon Kindle Fire HD 8.9 GoogleAPI Emulator\", \"recommended_backend_version\": \"1.5.3\", \"long_version\": \"4.4.\", \"api_name\": \"android\", \"supported_backend_versions\": [\"1.4.16\", \"1.5.1\", \"1.5.2\", \"1.5.3\"], \"device\": \"KindleFireHDGoogleAPI\", \"latest_stable_version\": \"\", \"automation_backend\": \"appium\", \"os\": \"Linux\"}";

    List<Browser> browsers =
        this.browserFactory.getBrowserListFromJson("[" + obj + "," + obj + "]");
    Collections.sort(browsers);

    assertEquals(2, browsers.size());

    Browser browser1 = browsers.get(0);
    assertEquals("Amazon_Kindle_Fire_HD_8_9_GoogleAPI_Emulatorlandscapeandroid4_4_", browser1.getKey());
    assertEquals("android", browser1.getOs());
    assertEquals("android", browser1.getBrowserName());
    assertEquals("4.4", browser1.getVersion());
    assertEquals("Amazon Kindle Fire HD 8.9 GoogleAPI Emulator 4.4 (landscape)", browser1.getName());
    assertEquals("4.4.", browser1.getLongVersion());
    assertEquals("Amazon Kindle Fire HD 8.9 GoogleAPI Emulator", browser1.getLongName());
    assertEquals("Amazon Kindle Fire HD 8.9 GoogleAPI Emulator", browser1.getDevice());
    assertNull(browser1.getDeviceType());
    assertEquals("landscape", browser1.getDeviceOrientation());

    Browser browser2 = browsers.get(1);
    assertEquals("Amazon_Kindle_Fire_HD_8_9_GoogleAPI_Emulatorportraitandroid4_4_", browser2.getKey());
    assertEquals("android", browser2.getOs());
    assertEquals("android", browser2.getBrowserName());
    assertEquals("4.4", browser2.getVersion());
    assertEquals("Amazon Kindle Fire HD 8.9 GoogleAPI Emulator 4.4 (portrait)", browser2.getName());
    assertEquals("4.4.", browser2.getLongVersion());
    assertEquals("Amazon Kindle Fire HD 8.9 GoogleAPI Emulator", browser2.getLongName());
    assertEquals("Amazon Kindle Fire HD 8.9 GoogleAPI Emulator", browser2.getDevice());
    assertNull(browser2.getDeviceType());
    assertEquals("portrait", browser2.getDeviceOrientation());
  }

  @Test
  void testDuplicateWebDevice() {
    String obj =
        "{\"short_version\": \"4\", \"long_name\": \"Firefox\", \"api_name\": \"firefox\", \"long_version\": \"4.0.1.\", \"latest_stable_version\": \"\", \"automation_backend\": \"webdriver\", \"os\": \"Linux\"}";
    List<Browser> browsers =
        this.browserFactory.getBrowserListFromJson("[" + obj + "," + obj + "]");
    Collections.sort(browsers);

    assertEquals(2, browsers.size());

    Browser browser1 = browsers.get(0);
    assertEquals("Linuxfirefox4", browser1.getKey());
    assertEquals("Linux", browser1.getOs());
    assertEquals("firefox", browser1.getBrowserName());
    assertEquals("4", browser1.getVersion());
    assertEquals("Linux Firefox 4", browser1.getName());
    assertEquals("4.0.1.", browser1.getLongVersion());
    assertEquals("Firefox", browser1.getLongName());
    assertNull(browser1.getDevice());
    assertNull(browser1.getDeviceType());
    assertNull(browser1.getDeviceOrientation());

    Browser browser2 = browsers.get(1);
    assertEquals("Linuxfirefoxlatest", browser2.getKey());
    assertEquals("Linux", browser2.getOs());
    assertEquals("firefox", browser2.getBrowserName());
    assertEquals("latest", browser2.getVersion());
    assertEquals("Linux Firefox latest", browser2.getName());
    assertEquals("latest", browser2.getLongVersion());
    assertEquals("Firefox", browser2.getLongName());
    assertNull(browser2.getDevice());
    assertNull(browser2.getDeviceType());
    assertNull(browser2.getDeviceOrientation());
  }
}
