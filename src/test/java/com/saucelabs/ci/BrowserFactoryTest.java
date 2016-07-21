package com.saucelabs.ci;

import com.saucelabs.saucerest.SauceREST;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BrowserFactoryTest {

    private BrowserFactory browserFactory;

    @Before
    public void setUp() throws Exception {
        SauceREST sauceREST = mock(SauceREST.class);
        when(
            sauceREST.getSupportedPlatforms("appium")
        ).thenReturn(IOUtils.toString(this.getClass().getResourceAsStream("/appium.json"), StandardCharsets.UTF_8));
        when(
            sauceREST.getSupportedPlatforms("webdriver")
        ).thenReturn(IOUtils.toString(this.getClass().getResourceAsStream("/webdriver.json"), StandardCharsets.UTF_8));
        this.browserFactory = new BrowserFactory(sauceREST);
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testGetAppiumBrowsers() throws Exception {
        List<Browser> browsers = this.browserFactory.getAppiumBrowsers();
        assertEquals(242, browsers.size());

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

        elm = 2;
        assertEquals("Amazon_Kindle_Fire_Emulatorlandscapeandroidlatest", browsers.get(elm).getKey());
        assertEquals("android", browsers.get(elm).getOs());
        assertEquals("android", browsers.get(elm).getBrowserName());
        assertEquals("latest", browsers.get(elm).getVersion());
        assertEquals("Amazon Kindle Fire Emulator latest (landscape)", browsers.get(elm).getName());
        assertEquals("latest", browsers.get(elm).getLongVersion());
        assertEquals("Amazon Kindle Fire Emulator", browsers.get(elm).getLongName());
        assertEquals("Amazon Kindle Fire Emulator", browsers.get(elm).getDevice());
        assertEquals(null, browsers.get(elm).getDeviceType());
        assertEquals("landscape", browsers.get(elm).getDeviceOrientation());

        elm = 3;
        assertEquals("Amazon_Kindle_Fire_Emulatorportraitandroidlatest", browsers.get(elm).getKey());
        assertEquals("android", browsers.get(elm).getOs());
        assertEquals("android", browsers.get(elm).getBrowserName());
        assertEquals("latest", browsers.get(elm).getVersion());
        assertEquals("Amazon Kindle Fire Emulator latest (portrait)", browsers.get(elm).getName());
        assertEquals("latest", browsers.get(elm).getLongVersion());
        assertEquals("Amazon Kindle Fire Emulator", browsers.get(elm).getLongName());
        assertEquals("Amazon Kindle Fire Emulator", browsers.get(elm).getDevice());
        assertEquals(null, browsers.get(elm).getDeviceType());
        assertEquals("portrait", browsers.get(elm).getDeviceOrientation());
    }

    @Test
    public void testGetWebDriverBrowsers() throws Exception {
        List<Browser> browsers = this.browserFactory.getWebDriverBrowsers();
        assertEquals(843, browsers.size());

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

        elm = 146;
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

        elm = 157;
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
    public void testShouldRetrieveBrowsers() throws Exception {

    }

    @Test
    public void testGetSauceAPIFactory() throws Exception {

    }

    @Test
    public void testGetBrowserListFromJson() throws Exception {
        // Where the meat is
    }

    @Test
    public void testSeleniumBrowserForKey() throws Exception {

    }

    @Test
    public void testSeleniumBrowserForKey1() throws Exception {

    }

    @Test
    public void testWebDriverBrowserForKey() throws Exception {

    }

    @Test
    public void testWebDriverBrowserForKey1() throws Exception {

    }

    @Test
    public void testAppiumBrowserForKey() throws Exception {

    }

    @Test
    public void testGetInstance() throws Exception {

    }

    @Test
    public void testGetInstance1() throws Exception {

    }
}
