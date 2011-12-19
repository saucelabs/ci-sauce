package com.saucelabs.sod;

import com.saucelabs.ci.SauceLibraryManager;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;

/**
 * @author Ross Rowe
 */
public class SauceLibraryManagerTest {
    
    private SauceLibraryManager sauceLibraryManager;

    @Before
    public void setUp() throws Exception {
        this.sauceLibraryManager = new SauceLibraryManager() {

            @Override
            public void updatePluginJar(File jarFile) throws IOException, URISyntaxException {
                //To change body of implemented methods use File | Settings | File Templates.
            }
        };
    }

    @Test
    public void versionsFromFile() throws Exception {
        String sampleJson = IOUtils.toString(getClass().getResourceAsStream("/versions.json"));
        int version = sauceLibraryManager.extractVersionFromResponse(sampleJson);
        assertEquals("Version not equal to 17", version, 17);
    }
}
