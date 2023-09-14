package com.saucelabs.ci;

import com.saucelabs.saucerest.SauceREST;
import com.saucelabs.saucerest.model.platform.Platform;
import com.saucelabs.saucerest.model.platform.SupportedPlatforms;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BrowserFactoryTest {

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private BrowserFactory browserFactory;

    @Before
    public void setUp() throws Exception {

        // Mock appium response
        List<Platform> appiumPlatforms = new ArrayList<Platform>();

        Platform kindle = new Platform("2.3", "Amazon Kindle Fire Emulator", "android", "2.3.7.", "2.3.7.", "appium", "android", null, null, null, "Amazon Kindle Fire Emulator");
        Platform kindle2 = new Platform("2.4", "Amazon Kindle Fire Emulator", "android", "2.4.7.", "2.4.7.", "appium", "android", null, null, null, "Amazon Kindle Fire Emulator");

        appiumPlatforms.add(kindle);

        // Mock webdriver response
        List<Platform> webdriverPlatforms = new ArrayList<Platform>();
        Platform kindleHD = new Platform("4.0", "Amazon Kindle Fire HD 8.9 Emulator", "android", "4.0.4.", "4.0.4.", "appium", "android", null, null, null, "Amazon Kindle Fire HD 8.9 Emulator");
        Platform linux = new Platform("4", "Firefox", "firefox", "4.0.1.", null, null, "Linux", null, null, null, null);
        webdriverPlatforms.add(kindleHD);
        webdriverPlatforms.add(linux);

        SauceREST sauceREST = mock(SauceREST.class);
        com.saucelabs.saucerest.api.PlatformEndpoint mockPlatform = mock(com.saucelabs.saucerest.api.PlatformEndpoint.class);

        when(
            sauceREST.getPlatformEndpoint()
        ).thenReturn(mockPlatform);

        when(
            mockPlatform.getSupportedPlatforms("appium")
        ).thenReturn(new SupportedPlatforms(appiumPlatforms));

        when(
            mockPlatform.getSupportedPlatforms("webdriver")
        ).thenReturn(new SupportedPlatforms(webdriverPlatforms));

        this.browserFactory = new BrowserFactory(sauceREST);
    }

    @Test
    public void testGetAppiumBrowsers() throws IOException {
        List<Browser> browsers = this.browserFactory.getAppiumBrowsers();
        assertEquals(2, browsers.size());

        int elm = 0;
        assertEquals("Amazon_Kindle_Fire_Emulatorlandscapeandroid2_3_7_", browsers.get(elm).getKey());
        assertEquals("android", browsers.get(elm).getOs());
        assertEquals("android", browsers.get(elm).getBrowserName());
        assertEquals("2.3", browsers.get(elm).getVersion());
        assertEquals("Amazon Kindle Fire Emulator 2.3 (landscape)", browsers.get(elm).getName());
        assertEquals("2.3.7.", browsers.get(elm).getLongVersion());
        assertEquals("Amazon Kindle Fire Emulator", browsers.get(elm).getLongName());
        assertEquals("Amazon Kindle Fire Emulator", browsers.get(elm).getDevice());
        assertEquals(null, browsers.get(elm).getDeviceType());
        assertEquals("landscape", browsers.get(elm).getDeviceOrientation());

        elm = 1;
        assertEquals("Amazon_Kindle_Fire_Emulatorportraitandroid2_3_7_", browsers.get(elm).getKey());
        assertEquals("android", browsers.get(elm).getOs());
        assertEquals("android", browsers.get(elm).getBrowserName());
        assertEquals("2.3", browsers.get(elm).getVersion());
        assertEquals("Amazon Kindle Fire Emulator 2.3 (portrait)", browsers.get(elm).getName());
        assertEquals("2.3.7.", browsers.get(elm).getLongVersion());
        assertEquals("Amazon Kindle Fire Emulator", browsers.get(elm).getLongName());
        assertEquals("Amazon Kindle Fire Emulator", browsers.get(elm).getDevice());
        assertEquals(null, browsers.get(elm).getDeviceType());
        assertEquals("portrait", browsers.get(elm).getDeviceOrientation());
    }

    @Test
    public void testGetWebDriverBrowsers() throws IOException {
        List<Browser> browsers = this.browserFactory.getWebDriverBrowsers();
        assertEquals(4, browsers.size());

        int elm = 0;
        assertEquals("Amazon_Kindle_Fire_HD_8_9_Emulatorlandscapeandroid4_0_4_", browsers.get(elm).getKey());
        assertEquals("android", browsers.get(elm).getOs());
        assertEquals("android", browsers.get(elm).getBrowserName());
        assertEquals("4.0", browsers.get(elm).getVersion());
        assertEquals("Amazon Kindle Fire HD 8.9 Emulator 4.0 (landscape)", browsers.get(elm).getName());
        assertEquals("4.0.4.", browsers.get(elm).getLongVersion());
        assertEquals("Amazon Kindle Fire HD 8.9 Emulator", browsers.get(elm).getLongName());
        assertEquals("Amazon Kindle Fire HD 8.9 Emulator", browsers.get(elm).getDevice());
        assertEquals(null, browsers.get(elm).getDeviceType());
        assertEquals("landscape", browsers.get(elm).getDeviceOrientation());

        elm = 1;
        assertEquals("Amazon_Kindle_Fire_HD_8_9_Emulatorportraitandroid4_0_4_", browsers.get(elm).getKey());
        assertEquals("android", browsers.get(elm).getOs());
        assertEquals("android", browsers.get(elm).getBrowserName());
        assertEquals("4.0", browsers.get(elm).getVersion());
        assertEquals("Amazon Kindle Fire HD 8.9 Emulator 4.0 (portrait)", browsers.get(elm).getName());
        assertEquals("4.0.4.", browsers.get(elm).getLongVersion());
        assertEquals("Amazon Kindle Fire HD 8.9 Emulator", browsers.get(elm).getLongName());
        assertEquals("Amazon Kindle Fire HD 8.9 Emulator", browsers.get(elm).getDevice());
        assertEquals(null, browsers.get(elm).getDeviceType());
        assertEquals("portrait", browsers.get(elm).getDeviceOrientation());

        elm = 2;
        assertEquals("Linuxfirefox4", browsers.get(elm).getKey());
        assertEquals("Linux", browsers.get(elm).getOs());
        assertEquals("firefox", browsers.get(elm).getBrowserName());
        assertEquals("4", browsers.get(elm).getVersion());
        assertEquals("Linux Firefox 4", browsers.get(elm).getName());
        assertEquals("4.0.1.", browsers.get(elm).getLongVersion());
        assertEquals("Firefox", browsers.get(elm).getLongName());
        assertEquals(null, browsers.get(elm).getDevice());
        assertEquals(null, browsers.get(elm).getDeviceType());
        assertEquals(null, browsers.get(elm).getDeviceOrientation());

        elm = 3;
        assertEquals("Linuxfirefoxlatest", browsers.get(elm).getKey());
        assertEquals("Linux", browsers.get(elm).getOs());
        assertEquals("firefox", browsers.get(elm).getBrowserName());
        assertEquals("latest", browsers.get(elm).getVersion());
        assertEquals("Linux Firefox latest", browsers.get(elm).getName());
        assertEquals("latest", browsers.get(elm).getLongVersion());
        assertEquals("Firefox", browsers.get(elm).getLongName());
        assertEquals(null, browsers.get(elm).getDevice());
        assertEquals(null, browsers.get(elm).getDeviceType());
        assertEquals(null, browsers.get(elm).getDeviceOrientation());
    }

    @Test
    public void testDuplicateMobileDevice() {
        String obj = "{\"deprecated_backend_versions\": [\"1.4.13\"], \"short_version\": \"4.4\", \"long_name\": \"Amazon Kindle Fire HD 8.9 GoogleAPI Emulator\", \"recommended_backend_version\": \"1.5.3\", \"long_version\": \"4.4.\", \"api_name\": \"android\", \"supported_backend_versions\": [\"1.4.16\", \"1.5.1\", \"1.5.2\", \"1.5.3\"], \"device\": \"KindleFireHDGoogleAPI\", \"latest_stable_version\": \"\", \"automation_backend\": \"appium\", \"os\": \"Linux\"}";

        List<Browser> browsers = this.browserFactory.getBrowserListFromJson(
            "[" + obj + "," + obj + "]"
        );
        Collections.sort(browsers);

        assertEquals(2, browsers.size());

        int elm = 0;
        assertEquals("Amazon_Kindle_Fire_HD_8_9_GoogleAPI_Emulatorlandscapeandroid4_4_", browsers.get(elm).getKey());
        assertEquals("android", browsers.get(elm).getOs());
        assertEquals("android", browsers.get(elm).getBrowserName());
        assertEquals("4.4", browsers.get(elm).getVersion());
        assertEquals("Amazon Kindle Fire HD 8.9 GoogleAPI Emulator 4.4 (landscape)", browsers.get(elm).getName());
        assertEquals("4.4.", browsers.get(elm).getLongVersion());
        assertEquals("Amazon Kindle Fire HD 8.9 GoogleAPI Emulator", browsers.get(elm).getLongName());
        assertEquals("Amazon Kindle Fire HD 8.9 GoogleAPI Emulator", browsers.get(elm).getDevice());
        assertEquals(null, browsers.get(elm).getDeviceType());
        assertEquals("landscape", browsers.get(elm).getDeviceOrientation());

        elm = 1;
        assertEquals("Amazon_Kindle_Fire_HD_8_9_GoogleAPI_Emulatorportraitandroid4_4_", browsers.get(elm).getKey());
        assertEquals("android", browsers.get(elm).getOs());
        assertEquals("android", browsers.get(elm).getBrowserName());
        assertEquals("4.4", browsers.get(elm).getVersion());
        assertEquals("Amazon Kindle Fire HD 8.9 GoogleAPI Emulator 4.4 (portrait)", browsers.get(elm).getName());
        assertEquals("4.4.", browsers.get(elm).getLongVersion());
        assertEquals("Amazon Kindle Fire HD 8.9 GoogleAPI Emulator", browsers.get(elm).getLongName());
        assertEquals("Amazon Kindle Fire HD 8.9 GoogleAPI Emulator", browsers.get(elm).getDevice());
        assertEquals(null, browsers.get(elm).getDeviceType());
        assertEquals("portrait", browsers.get(elm).getDeviceOrientation());
    }

    @Test
    public void testDuplicateWebDevice() {
        String obj = "{\"short_version\": \"4\", \"long_name\": \"Firefox\", \"api_name\": \"firefox\", \"long_version\": \"4.0.1.\", \"latest_stable_version\": \"\", \"automation_backend\": \"webdriver\", \"os\": \"Linux\"}";
        List<Browser> browsers = this.browserFactory.getBrowserListFromJson(
            "[" + obj + "," + obj + "]"
        );
        Collections.sort(browsers);

        assertEquals(2, browsers.size());

        int elm = 0;
        assertEquals("Linuxfirefox4", browsers.get(elm).getKey());
        assertEquals("Linux", browsers.get(elm).getOs());
        assertEquals("firefox", browsers.get(elm).getBrowserName());
        assertEquals("4", browsers.get(elm).getVersion());
        assertEquals("Linux Firefox 4", browsers.get(elm).getName());
        assertEquals("4.0.1.", browsers.get(elm).getLongVersion());
        assertEquals("Firefox", browsers.get(elm).getLongName());
        assertNull(browsers.get(elm).getDevice());
        assertNull(browsers.get(elm).getDeviceType());
        assertNull(browsers.get(elm).getDeviceOrientation());

        elm = 1;
        assertEquals("Linuxfirefoxlatest", browsers.get(elm).getKey());
        assertEquals("Linux", browsers.get(elm).getOs());
        assertEquals("firefox", browsers.get(elm).getBrowserName());
        assertEquals("latest", browsers.get(elm).getVersion());
        assertEquals("Linux Firefox latest", browsers.get(elm).getName());
        assertEquals("latest", browsers.get(elm).getLongVersion());
        assertEquals("Firefox", browsers.get(elm).getLongName());
        assertNull(browsers.get(elm).getDevice());
        assertNull(browsers.get(elm).getDeviceType());
        assertNull(browsers.get(elm).getDeviceOrientation());
    }
}
