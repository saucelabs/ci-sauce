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
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SauceConnectFourManagerTest {

  private static final String STARTED_SC_LOG = "/started_sc.log";
  private static final String STARTED_TUNNEL_ID = "a3ccd3985ed04e7ba0fefc7fa401e9c8";

  @Mock private Process mockProcess;
  @Mock private SauceREST mockSauceRest;
  @Mock private SauceConnectEndpoint mockSCEndpoint;
  @Spy private final SauceConnectFourManager tunnelManager = new SauceConnectFourManager();

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
    TunnelInformation readyTunnel = new TunnelInformation();
    readyTunnel.isReady = true;
    when(mockSCEndpoint.getTunnelInformation(STARTED_TUNNEL_ID)).thenReturn(readyTunnel);
    tunnelManager.setCleanUpOnExit(cleanUpOnExit);
    Process process = testOpenConnection(STARTED_SC_LOG);
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
    TunnelInformation notReadyTunnel = new TunnelInformation();
    notReadyTunnel.isReady = false;
    TunnelInformation readyTunnel = new TunnelInformation();
    readyTunnel.isReady = true;
    when(mockSCEndpoint.getTunnelInformation(STARTED_TUNNEL_ID)).thenReturn(notReadyTunnel,
          readyTunnel);
    testOpenConnection(STARTED_SC_LOG, " username-with-spaces-around ");
  }

  private Process testOpenConnection(String logFile) throws IOException {
    return testOpenConnection(logFile, "fakeuser");
  }

  private Process testOpenConnection(String logFile, String username) throws IOException {
    final String apiKey = "fakeapikey";
    final DataCenter dataCenter = DataCenter.US_WEST;

    try (InputStream resourceAsStream = getResourceAsStream(logFile)) {
      when(mockProcess.getErrorStream())
          .thenReturn(new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8)));
      when(mockProcess.getInputStream()).thenReturn(resourceAsStream);
      doReturn(mockProcess).when(tunnelManager).createProcess(any(String[].class), any(File.class));
      return tunnelManager.openConnection(
          username, apiKey, dataCenter, null, "  ", ps, false, "");
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
      assertThat(Integer.parseInt(actualArgs[6]), allOf(greaterThan(0), lessThan(65536)));
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
    started.isReady = true;

    when(mockSCEndpoint.getTunnelsInformationForAUser()).thenReturn(List.of(started));
    when(mockSCEndpoint.getTunnelInformation(STARTED_TUNNEL_ID)).thenReturn(started);

    Process process = testOpenConnection(STARTED_SC_LOG);
    assertEquals(mockProcess, process);

    verify(mockSCEndpoint).getTunnelsInformationForAUser();
  }

  @ParameterizedTest
  @CsvSource({
      "true,  LINUX",
      "true,  WINDOWS",
      "true,  OSX",
      "false, LINUX",
      "false, WINDOWS",
      "false, OSX"
  })
  void testExtractZipFile(boolean cleanUpOnExit, OperatingSystem operatingSystem,
      @TempDir Path folder) throws IOException {
    String osName = operatingSystem.name().toLowerCase(Locale.ROOT);
    File destination = folder.resolve("sauceconnect_" + osName).toFile();

    SauceConnectFourManager manager = new SauceConnectFourManager();
    manager.setCleanUpOnExit(cleanUpOnExit);
    manager.extractZipFile(destination, operatingSystem);

    File expectedBinaryPath = new File(destination, operatingSystem.getDirectory(false));
    File expectedBinaryFile = new File(expectedBinaryPath, operatingSystem.getExecutable());
    assertTrue(expectedBinaryFile.exists(), () -> osName + " binary exists at " + expectedBinaryFile);
    assertTrue(expectedBinaryFile.canExecute(), () -> osName + " binary " + expectedBinaryFile + " is executable");
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

  @Test
  void shouldInitLatestVersionLazilyAndOnce() throws IOException, InterruptedException {
    try (MockedStatic<HttpClient> httpClientStaticMock = mockStatic(HttpClient.class)) {
      HttpClient httpClient = mock();
      HttpResponse<String> httpResponse = mock();
      String version = "4.99.99";
      when(httpResponse.body()).thenReturn("{\"Sauce Connect\": {\"version\": \"" + version + "\"}}");
      when(httpClient.send(any(), argThat((ArgumentMatcher<BodyHandler<String>>) argument -> true))).thenReturn(
          httpResponse);
      httpClientStaticMock.when(HttpClient::newHttpClient).thenReturn(httpClient);

      SauceConnectFourManager sauceConnectFourManager = new SauceConnectFourManager();
      sauceConnectFourManager.setUseLatestSauceConnect(true);

      String currentVersion = sauceConnectFourManager.getCurrentVersion();
      assertEquals(version, currentVersion);
      httpClientStaticMock.verify(HttpClient::newHttpClient);

      currentVersion = sauceConnectFourManager.getCurrentVersion();
      assertEquals(version, currentVersion);
      httpClientStaticMock.verifyNoMoreInteractions();
    }
  }
}
