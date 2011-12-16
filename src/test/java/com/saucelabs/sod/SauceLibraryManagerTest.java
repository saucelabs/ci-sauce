package com.saucelabs.bamboo.sod;

import com.saucelabs.bamboo.sod.util.BambooSauceLibraryManager;
import com.saucelabs.ci.SauceLibraryManager;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Ross Rowe
 */
public class SauceLibraryManagerTest {
    
    private SauceLibraryManager sauceLibraryManager;

    @Before
    public void setUp() throws Exception {
        this.sauceLibraryManager = new BambooSauceLibraryManager();
    }

    @Test
    public void versionsFromFile() throws Exception {
        String sampleJson = IOUtils.toString(getClass().getResourceAsStream("/versions.json"));
        int version = sauceLibraryManager.extractVersionFromResponse(sampleJson);
        assertEquals("Version not equal to 17", version, 17);
    }
}
