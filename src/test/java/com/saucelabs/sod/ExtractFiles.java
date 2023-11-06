package com.saucelabs.sod;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.saucelabs.ci.sauceconnect.SauceConnectFourManager;
import com.saucelabs.ci.sauceconnect.SauceConnectFourManager.OperatingSystem;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * @author Ross Rowe
 */
class ExtractFiles {

  private final SauceConnectFourManager manager = new SauceConnectFourManager();

  static Stream<Arguments> operatingSystems() {
    return Stream.of(
      arguments(OperatingSystem.OSX, "sc"),
      arguments(OperatingSystem.LINUX, "sc"),
      arguments(OperatingSystem.LINUX_ARM64, "sc"),
      arguments(OperatingSystem.WINDOWS, "sc.exe")
    );
  }

  @ParameterizedTest
  @MethodSource("operatingSystems")
  void shouldExtractSauceConnectExecutable(OperatingSystem os, String executableFileName, @TempDir Path tempDir)
      throws IOException {
    File dir = manager.extractZipFile(tempDir.toFile(), os);
    File executableFile = dir.toPath().resolve("bin/" + executableFileName).toFile();
    assertTrue(executableFile.exists());
  }
}
