package com.saucelabs.sod;

import com.saucelabs.ci.sauceconnect.SauceConnectFourManager;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

/**
 * @author Ross Rowe
 */
public class ExtractFiles {

    private SauceConnectFourManager manager = new SauceConnectFourManager();

    @Before
    public void setup() throws Exception {

    }

    @Test
    public void linux() throws Exception {
        File workingDirectory = new File(System.getProperty("java.io.tmpdir"));
        manager.extractZipFile(workingDirectory, SauceConnectFourManager.OperatingSystem.LINUX);
    }

    @Test
    public void windows() throws Exception {
        manager.extractZipFile(new File("java.io.tmpdir"), SauceConnectFourManager.OperatingSystem.WINDOWS);
    }
}
