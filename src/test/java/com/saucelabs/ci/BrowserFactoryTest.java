package com.saucelabs.ci;

import com.saucelabs.saucerest.SauceREST;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.URL;
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
            sauceREST.getSupportedPlatforms("selenium-rc")
        ).thenReturn(IOUtils.toString(this.getClass().getResourceAsStream("/selenium-rc.json")));
        when(
            sauceREST.getSupportedPlatforms("appium")
        ).thenReturn(IOUtils.toString(this.getClass().getResourceAsStream("/appium.json")));
        when(
            sauceREST.getSupportedPlatforms("webdriver")
        ).thenReturn(IOUtils.toString(this.getClass().getResourceAsStream("/webdriver.json")));
        this.browserFactory = new BrowserFactory(sauceREST);
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testGetSeleniumBrowsers() throws Exception {
        List<Browser> browsers = this.browserFactory.getSeleniumBrowsers();
        assertEquals("No results were returned", 0, browsers.size());
    }

    @Test
    public void testGetAppiumBrowsers() throws Exception {
        List<Browser> browsers = this.browserFactory.getAppiumBrowsers();
        assertEquals(176, browsers.size());

        assertEquals("Amazon_Kindle_Fire_Emulatorlandscapeandroid2_3_7_", browsers.get(0).getKey());
        assertEquals("android", browsers.get(0).getOs());
        assertEquals("android", browsers.get(0).getBrowserName());
        assertEquals("2.3", browsers.get(0).getVersion());
        assertEquals("Amazon Kindle Fire Emulator 2.3 (landscape)", browsers.get(0).getName());
        assertEquals("2.3.7.", browsers.get(0).getLongVersion());
        assertEquals("Amazon Kindle Fire Emulator", browsers.get(0).getLongName());
        assertEquals("Amazon Kindle Fire Emulator", browsers.get(0).getDevice());
        assertEquals(null, browsers.get(0).getDeviceType());
        assertEquals("landscape", browsers.get(0).getDeviceOrientation());

        assertEquals("Amazon_Kindle_Fire_Emulatorportraitandroid2_3_7_", browsers.get(1).getKey());
        assertEquals("android", browsers.get(1).getOs());
        assertEquals("android", browsers.get(1).getBrowserName());
        assertEquals("2.3", browsers.get(1).getVersion());
        assertEquals("Amazon Kindle Fire Emulator 2.3 (portrait)", browsers.get(1).getName());
        assertEquals("2.3.7.", browsers.get(1).getLongVersion());
        assertEquals("Amazon Kindle Fire Emulator", browsers.get(1).getLongName());
        assertEquals("Amazon Kindle Fire Emulator", browsers.get(1).getDevice());
        assertEquals(null, browsers.get(1).getDeviceType());
        assertEquals("portrait", browsers.get(1).getDeviceOrientation());
    }

    @Test
    public void testGetWebDriverBrowsers() throws Exception {
        List<Browser> browsers = this.browserFactory.getWebDriverBrowsers();
        assertEquals(767, browsers.size());

        assertEquals("Amazon_Kindle_Fire_HD_8_9_Emulatorlandscapeandroid4_0_4_", browsers.get(0).getKey());
        assertEquals("android", browsers.get(0).getOs());
        assertEquals("android", browsers.get(0).getBrowserName());
        assertEquals("4.0", browsers.get(0).getVersion());
        assertEquals("Amazon Kindle Fire HD 8.9 Emulator 4.0 (landscape)", browsers.get(0).getName());
        assertEquals("4.0.4.", browsers.get(0).getLongVersion());
        assertEquals("Amazon Kindle Fire HD 8.9 Emulator", browsers.get(0).getLongName());
        assertEquals("Amazon Kindle Fire HD 8.9 Emulator", browsers.get(0).getDevice());
        assertEquals(null, browsers.get(0).getDeviceType());
        assertEquals("landscape", browsers.get(0).getDeviceOrientation());

        assertEquals("Amazon_Kindle_Fire_HD_8_9_Emulatorportraitandroid4_0_4_", browsers.get(1).getKey());
        assertEquals("android", browsers.get(1).getOs());
        assertEquals("android", browsers.get(1).getBrowserName());
        assertEquals("4.0", browsers.get(1).getVersion());
        assertEquals("Amazon Kindle Fire HD 8.9 Emulator 4.0 (portrait)", browsers.get(1).getName());
        assertEquals("4.0.4.", browsers.get(1).getLongVersion());
        assertEquals("Amazon Kindle Fire HD 8.9 Emulator", browsers.get(1).getLongName());
        assertEquals("Amazon Kindle Fire HD 8.9 Emulator", browsers.get(1).getDevice());
        assertEquals(null, browsers.get(1).getDeviceType());
        assertEquals("portrait", browsers.get(1).getDeviceOrientation());

        assertEquals("Linuxfirefox4", browsers.get(126).getKey());
        assertEquals("Linux", browsers.get(126).getOs());
        assertEquals("firefox", browsers.get(126).getBrowserName());
        assertEquals("4", browsers.get(126).getVersion());
        assertEquals("Linux Firefox 4", browsers.get(126).getName());
        assertEquals("4.0.1.", browsers.get(126).getLongVersion());
        assertEquals("Firefox", browsers.get(126).getLongName());
        assertEquals(null, browsers.get(126).getDevice());
        assertEquals(null, browsers.get(126).getDeviceType());
        assertEquals(null, browsers.get(126).getDeviceOrientation());
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
