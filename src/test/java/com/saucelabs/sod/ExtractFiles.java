package com.saucelabs.sod;

import com.saucelabs.ci.sauceconnect.SauceConnectFourManager;
import org.junit.jupiter.api.Test;

import java.io.File;

/**
 * @author Ross Rowe
 */
class ExtractFiles {

  private SauceConnectFourManager manager = new SauceConnectFourManager();

  @Test
  void linux() throws Exception {

    File workingDirectory = new File(System.getProperty("java.io.tmpdir"));
    manager.extractZipFile(workingDirectory, SauceConnectFourManager.OperatingSystem.LINUX);
  }

  @Test
  void windows() throws Exception {
    File workingDirectory = new File(System.getProperty("java.io.tmpdir"));
    manager.extractZipFile(workingDirectory, SauceConnectFourManager.OperatingSystem.WINDOWS);
  }
}
