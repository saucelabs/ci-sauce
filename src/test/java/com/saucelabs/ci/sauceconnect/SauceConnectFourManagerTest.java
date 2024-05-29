package com.saucelabs.ci.sauceconnect;

import com.saucelabs.ci.sauceconnect.SauceConnectFourManager.OperatingSystem;
import com.saucelabs.saucerest.DataCenter;
import com.saucelabs.saucerest.SauceREST;
import com.saucelabs.saucerest.api.SauceConnectEndpoint;
import com.saucelabs.saucerest.model.sauceconnect.TunnelInformation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SauceConnectFourManagerTest {

  @Mock private Process mockProcess;
  @Mock private SauceREST mockSauceRest;
  @Mock private SauceConnectEndpoint mockSCEndpoint;
  @Spy private final SauceConnectFourManager tunnelManager = new SauceConnectFourManager(false);

  private final PrintStream ps = System.out;

  @BeforeEach
  void beforeEach() {
    when(mockSauceRest.getSauceConnectEndpoint()).thenReturn(mockSCEndpoint);
    tunnelManager.setSauceRest(mockSauceRest);
  }

  private InputStream getResourceAsStream(String resourceName) {
    return getClass().getResourceAsStream(resourceName);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void testOpenConnectionSuccessfully(boolean cleanUpOnExit) throws IOException {
    when(mockSCEndpoint.getTunnelsInformationForAUser()).thenReturn(List.of());
    tunnelManager.setCleanUpOnExit(cleanUpOnExit);
    Process process = testOpenConnection("/started_sc.log");
    assertEquals(mockProcess, process);
  }

  @Test
  void openConnectionTest_closes() throws IOException, InterruptedException {
    when(mockSCEndpoint.getTunnelsInformationForAUser()).thenReturn(List.of());
    when(mockProcess.waitFor(30, TimeUnit.SECONDS)).thenReturn(true);
    assertThrows(AbstractSauceTunnelManager.SauceConnectDidNotStartException.class, () -> testOpenConnection(
            "/started_sc_closes.log"));
    verify(mockProcess).destroy();
  }

  @Test
  void testOpenConnectionWithExtraSpacesInArgs() throws IOException {
    when(mockSCEndpoint.getTunnelsInformationForAUser()).thenReturn(List.of());
    testOpenConnection("/started_sc.log", " username-with-spaces-around ");
  }

  private Process testOpenConnection(String logFile) throws IOException {
    return testOpenConnection(logFile, "fakeuser");
  }

  private Process testOpenConnection(String logFile, String username) throws IOException {
    final String apiKey = "fakeapikey";
    final int port = 12345;
    final DataCenter dataCenter = DataCenter.US_WEST;

    try (InputStream resourceAsStream = getResourceAsStream(logFile)) {
      when(mockProcess.getErrorStream())
          .thenReturn(new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8)));
      when(mockProcess.getInputStream()).thenReturn(resourceAsStream);
      doReturn(mockProcess).when(tunnelManager).createProcess(any(String[].class), any(File.class));
      return tunnelManager.openConnection(
          username, apiKey, dataCenter, port, null, "  ", ps, false, "");
    } finally {
      verify(mockSCEndpoint).getTunnelsInformationForAUser();
      ArgumentCaptor<String[]> argsCaptor = ArgumentCaptor.forClass(String[].class);
      verify(tunnelManager).createProcess(argsCaptor.capture(), any(File.class));
      String[] actualArgs = argsCaptor.getValue();
      assertEquals(9, actualArgs.length);
      assertEquals("-u", actualArgs[1]);
      assertEquals(username.trim(), actualArgs[2]);
      assertEquals("-k", actualArgs[3]);
      assertEquals(apiKey, actualArgs[4]);
      assertEquals("-P", actualArgs[5]);
      assertEquals(Integer.toString(port), actualArgs[6]);
      assertEquals("--extra-info", actualArgs[7]);
      OperatingSystem operatingSystem = OperatingSystem.getOperatingSystem();
      if (operatingSystem == OperatingSystem.WINDOWS) {
        assertEquals("{\\\"runner\\\": \\\"jenkins\\\"}", actualArgs[8]);
      } else {
        assertEquals("{\"runner\": \"jenkins\"}", actualArgs[8]);
      }
    }
  }

  @Test
  void openConnectionTest_existing_tunnel() throws IOException {
    TunnelInformation started = new TunnelInformation();
    started.tunnelIdentifier = "8949e55fb5e14fd6bf6230b7a609b494";
    started.status = "running";

    when(mockSCEndpoint.getTunnelsInformationForAUser()).thenReturn(List.of(started));

    Process process = testOpenConnection("/started_sc.log");
    assertEquals(mockProcess, process);

    verify(mockSCEndpoint).getTunnelsInformationForAUser();
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void testExtractZipFile(boolean cleanUpOnExit, @TempDir Path folder) throws IOException {
    File linux_destination = folder.resolve("sauceconnect_linux").toFile();
    File windows_destination = folder.resolve("sauceconnect_windows").toFile();
    File osx_destination = folder.resolve("sauceconnect_osx").toFile();

    SauceConnectFourManager manager = new SauceConnectFourManager();
    manager.setCleanUpOnExit(cleanUpOnExit);

    manager.extractZipFile(linux_destination, OperatingSystem.LINUX);
    assertSauceConnectFileExists(
        "Linux executable exists", linux_destination, OperatingSystem.LINUX);

    manager.extractZipFile(windows_destination, OperatingSystem.WINDOWS);
    assertSauceConnectFileExists(
        "windows executable exists", windows_destination, OperatingSystem.WINDOWS);

    manager.extractZipFile(osx_destination, OperatingSystem.OSX);
    assertSauceConnectFileExists("osx executable exists", osx_destination, OperatingSystem.OSX);
  }

  private void assertSauceConnectFileExists(String message, File destination, OperatingSystem os) {
    assertTrue(
        new File(new File(destination, os.getDirectory(false)), os.getExecutable()).exists(),
        message
    );
  }

  @Test
  void testSauceConnectSecretsCoveredWithStars() {
    SauceConnectFourManager manager = new SauceConnectFourManager();
    String[] args = {"/sauce/connect/binary/path/"};
    args =
        manager.generateSauceConnectArgs(
            args,
            "username",
            "apiKey-apiKey-apiKey-apiKey-apiKey",
            1234,
            "--api-key apiKey-apiKey-apiKey-apiKey-apiKey -w user:pwd --proxy-userpwd user:pwd -a host:8080:user:pwd --auth host:8080:user:pwd -p host:8080 --proxy host:8080 -o pwd --other pwd");
    String result = manager.hideSauceConnectCommandlineSecrets(args);

    assertEquals(
        "[/sauce/connect/binary/path/, -u, username, -k, ****, -P, 1234, --api-key, ****, -w, user:****, --proxy-userpwd, user:****, -a, host:8080:user:****, --auth, host:8080:user:****, -p, host:8080, --proxy, host:8080, -o, pwd, --other, pwd]",
        result);
  }

  @Test
  void testSauceConnectSecretsWithSpecialCharactersCoveredWithStars() {
    SauceConnectFourManager manager = new SauceConnectFourManager();
    String[] args = {"-a", "web-proxy.domain.com:8080:user:pwd"};
    assertEquals(
        "[-a, web-proxy.domain.com:8080:user:****]",
        manager.hideSauceConnectCommandlineSecrets(args));

    args = new String[] {"-a", "host:8080:user:passwd%#123"};
    assertEquals("[-a, host:8080:user:****]", manager.hideSauceConnectCommandlineSecrets(args));

    args = new String[] {"-a", "host:8080:super-user:passwd"};
    assertEquals(
        "[-a, host:8080:super-user:****]", manager.hideSauceConnectCommandlineSecrets(args));

    args = new String[] {"-w", "user:passwd%#123"};
    assertEquals("[-w, user:****]", manager.hideSauceConnectCommandlineSecrets(args));

    args = new String[] {"-w", "super-user:passwd"};
    assertEquals("[-w, super-user:****]", manager.hideSauceConnectCommandlineSecrets(args));
  }
}
