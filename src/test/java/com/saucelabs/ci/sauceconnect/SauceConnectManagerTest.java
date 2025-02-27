package com.saucelabs.ci.sauceconnect;

import com.saucelabs.ci.sauceconnect.SauceConnectManager.OperatingSystem;
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
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.Semaphore;
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
class SauceConnectManagerTest {

  private static final String STARTED_SC_LOG = "/started_sc.log";
  private static final String STARTED_TUNNEL_ID = "0bf5b8e2090d4212ad2cc7c241382489";

  @Mock private Process mockProcess;
  @Mock private SauceREST mockSauceRest;
  @Mock private SauceConnectEndpoint mockSCEndpoint;
  @Mock private HttpClient mockHttpClient;
  @Spy private final SauceConnectManager tunnelManager = new SauceConnectManager();

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
    tunnelManager.setCleanUpOnExit(cleanUpOnExit);

    SCMonitor scMonitor = mock(SCMonitor.class);

    doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
      	      Semaphore sem = (Semaphore) invocation.getArgument(0);
      	      sem.release();
      	      return null;
            }
    }).when(scMonitor).setSemaphore(any(Semaphore.class));

    tunnelManager.setSCMonitor(scMonitor);

    Process process = testOpenConnection(STARTED_SC_LOG);
    assertEquals(mockProcess, process);
  }

  @Test
  void openConnectionTest_closes() throws IOException, InterruptedException {
    when(mockSCEndpoint.getTunnelsInformationForAUser()).thenReturn(List.of());
    when(mockProcess.waitFor(30, TimeUnit.SECONDS)).thenReturn(true);

    SCMonitor scMonitor = mock(SCMonitor.class);

    doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
      	      Semaphore sem = (Semaphore) invocation.getArgument(0);
      	      sem.release();
      	      return null;
            }
    }).when(scMonitor).setSemaphore(any(Semaphore.class));

    when(scMonitor.isFailed()).thenReturn(true);

    tunnelManager.setSCMonitor(scMonitor);
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

    SCMonitor scMonitor = mock(SCMonitor.class);

    doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
      	      Semaphore sem = (Semaphore) invocation.getArgument(0);
      	      sem.release();
      	      return null;
            }
    }).when(scMonitor).setSemaphore(any(Semaphore.class));

    tunnelManager.setSCMonitor(scMonitor);

    testOpenConnection(STARTED_SC_LOG, " username-with-spaces-around ");
  }

  private Process testOpenConnection(String logFile) throws IOException {
    return testOpenConnection(logFile, "fakeuser");
  }

  private Process testOpenConnection(String logFile, String username) throws IOException {
    final String apiKey = "fakeapikey";
    final DataCenter dataCenter = DataCenter.US_WEST;

    try (InputStream resourceAsStream = getResourceAsStream(logFile)) {
      doReturn(mockProcess).when(tunnelManager).createProcess(any(String[].class), any(File.class));
      return tunnelManager.openConnection(
          username, apiKey, dataCenter, null, "  ", ps, false, "");
    } finally {
      verify(mockSCEndpoint).getTunnelsInformationForAUser();
      ArgumentCaptor<String[]> argsCaptor = ArgumentCaptor.forClass(String[].class);
      verify(tunnelManager).createProcess(argsCaptor.capture(), any(File.class));
      String[] actualArgs = argsCaptor.getValue();
      assertEquals(10, actualArgs.length);
      assertEquals("run", actualArgs[1]);
      assertEquals("--username", actualArgs[2]);
      assertEquals(username.trim(), actualArgs[3]);
      assertEquals("--access-key", actualArgs[4]);
      assertEquals(apiKey, actualArgs[5]);
      assertEquals("--api-address", actualArgs[6]);
      assertThat(Integer.parseInt(actualArgs[7].substring(1)), allOf(greaterThan(0), lessThan(65536)));
      assertEquals("--metadata", actualArgs[8]);
      assertEquals("runner=jenkins", actualArgs[9]);
    }
  }

  @Test
  void openConnectionTest_existing_tunnel() throws IOException {
    TunnelInformation started = new TunnelInformation();
    started.tunnelIdentifier = "8949e55fb5e14fd6bf6230b7a609b494";
    started.status = "running";
    started.isReady = true;

    when(mockSCEndpoint.getTunnelsInformationForAUser()).thenReturn(List.of(started));

    SCMonitor scMonitor = mock(SCMonitor.class);

    doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
      	      Semaphore sem = (Semaphore) invocation.getArgument(0);
      	      sem.release();
      	      return null;
            }
    }).when(scMonitor).setSemaphore(any(Semaphore.class));

    tunnelManager.setSCMonitor(scMonitor);

    Process process = testOpenConnection(STARTED_SC_LOG);
    assertEquals(mockProcess, process);

    verify(mockSCEndpoint).getTunnelsInformationForAUser();
  }

  @ParameterizedTest
  @CsvSource({
      "true,  LINUX_AMD64",
      "true,  LINUX_ARM64",
      "true,  WINDOWS_AMD64",
      "true,  WINDOWS_ARM64",
      "true,  OSX",
      "false, LINUX_AMD64",
      "false, LINUX_ARM64",
      "false, WINDOWS_AMD64",
      "false, WINDOWS_ARM64",
      "false, OSX"
  })
  void testExtractZipFile(boolean cleanUpOnExit, OperatingSystem operatingSystem,
      @TempDir Path folder) throws IOException {
    String osName = operatingSystem.name().toLowerCase(Locale.ROOT);
    File destination = folder.resolve("sauceconnect_" + osName).toFile();

    SauceConnectManager manager = new SauceConnectManager();
    manager.setCleanUpOnExit(cleanUpOnExit);
    manager.extractZipFile(destination, operatingSystem);

    File expectedBinaryPath = new File(destination, operatingSystem.getDirectory(false));
    File expectedBinaryFile = new File(expectedBinaryPath, operatingSystem.getExecutable());
    assertTrue(expectedBinaryFile.exists(), () -> osName + " binary exists at " + expectedBinaryFile);
    assertTrue(expectedBinaryFile.canExecute(), () -> osName + " binary " + expectedBinaryFile + " is executable");
  }

  @Test
  void testSauceConnectSecretsCoveredWithStars() {
    SauceConnectManager manager = new SauceConnectManager();
    String[] args = {"/sauce/connect/binary/path/"};
    args =
        manager.generateSauceConnectArgs(
            args,
            "username",
            "apikey",
            "--access-key apiKey");
    String result = manager.hideSauceConnectCommandlineSecrets(args);

    assertEquals(
        "[/sauce/connect/binary/path/, run, --username, username, --access-key, ****, --api-address, :9000, --access-key, ****]",
        result);

  }

  @Test
  void testSauceConnectLegacySecretsCoveredWithStars() {
    // FIXME: Remove when SC4 legacy support is removed
    SauceConnectManager manager = new SauceConnectManager();
    String[] args = {"/sauce/connect/binary/path/"};
    args =
        manager.generateSauceConnectArgs(
            args,
            "username",
            "apikey",
            "--api-key apiKey");
    String result = manager.hideSauceConnectCommandlineSecrets(args);

    assertEquals(
        "[/sauce/connect/binary/path/, run, --username, username, --access-key, ****, --api-address, :9000, --api-key, ****]",
        result);

  }

  @Test
  void testSauceConnectAuthSecretsCoveredWithStars() {
    SauceConnectManager manager = new SauceConnectManager();

    String[] args = {"/sauce/connect/binary/path/"};
    args =
        manager.generateSauceConnectArgs(
            args,
            "username",
            "apikey",
            "--auth foo:bar@host:8080");
    String result = manager.hideSauceConnectCommandlineSecrets(args);

    assertEquals(
        "[/sauce/connect/binary/path/, run, --username, username, --access-key, ****, --api-address, :9000, --auth, ****]",
        result);

    args = new String[] {"/sauce/connect/binary/path/"};
    args =
        manager.generateSauceConnectArgs(
            args,
            "username",
            "apikey",
            "-a foo:bar@host:8080");
    result = manager.hideSauceConnectCommandlineSecrets(args);

    assertEquals(
        "[/sauce/connect/binary/path/, run, --username, username, --access-key, ****, --api-address, :9000, -a, ****]",
        result);

    args = new String[] {"/sauce/connect/binary/path/"};
    args =
        manager.generateSauceConnectArgs(
            args,
            "username",
            "apikey",
            "-a foo:bar@host:8080 -a user:pwd@host1:1234 --auth root:pass@host2:9999 --auth uucp:pass@host3:8080");
    result = manager.hideSauceConnectCommandlineSecrets(args);

    assertEquals(
        "[/sauce/connect/binary/path/, run, --username, username, --access-key, ****, --api-address, :9000, -a, ****, -a, ****, --auth, ****, --auth, ****]",
        result);

  }

  @Test
  void testSauceConnectProxySecretsCoveredWithStars() {
    SauceConnectManager manager = new SauceConnectManager();

    String[] args = {"/sauce/connect/binary/path/"};
    args =
        manager.generateSauceConnectArgs(
            args,
            "username",
            "apikey",
            "--proxy user:pwd@host:8080");
    String result = manager.hideSauceConnectCommandlineSecrets(args);

    assertEquals(
        "[/sauce/connect/binary/path/, run, --username, username, --access-key, ****, --api-address, :9000, --proxy, user:****@host:8080]",
        result);

    args = new String[] {"/sauce/connect/binary/path/"};
    args =
        manager.generateSauceConnectArgs(
            args,
            "username",
            "apikey",
            "--proxy user@host:8080");
    result = manager.hideSauceConnectCommandlineSecrets(args);

    assertEquals(
        "[/sauce/connect/binary/path/, run, --username, username, --access-key, ****, --api-address, :9000, --proxy, user@host:8080]",
        result);

    args = new String[] {"/sauce/connect/binary/path/"};
    args =
        manager.generateSauceConnectArgs(
            args,
            "username",
            "apikey",
            "-x user@host:8080");
    result = manager.hideSauceConnectCommandlineSecrets(args);

    assertEquals(
        "[/sauce/connect/binary/path/, run, --username, username, --access-key, ****, --api-address, :9000, -x, user@host:8080]",
        result);

    args = new String[] {"/sauce/connect/binary/path/"};
    args =
        manager.generateSauceConnectArgs(
            args,
            "username",
            "apikey",
            "--proxy-sauce user@host:8080");
    result = manager.hideSauceConnectCommandlineSecrets(args);

    assertEquals(
        "[/sauce/connect/binary/path/, run, --username, username, --access-key, ****, --api-address, :9000, --proxy-sauce, user@host:8080]",
        result);

  }

  @Test
  void testSauceConnectAPIBasicAuthSecretsCoveredWithStars() {
    SauceConnectManager manager = new SauceConnectManager();

    String[] args = {"/sauce/connect/binary/path/"};
    args =
        manager.generateSauceConnectArgs(
            args,
            "username",
            "apiKey",
            "--api-basic-auth user:pwd");
    String result = manager.hideSauceConnectCommandlineSecrets(args);

    assertEquals(
        "[/sauce/connect/binary/path/, run, --username, username, --access-key, ****, --api-address, :9000, --api-basic-auth, user:****]",
        result);

    args = new String[] {"/sauce/connect/binary/path/"};
    args =
        manager.generateSauceConnectArgs(
            args,
            "username",
            "apiKey",
            "--api-basic-auth user");
    result = manager.hideSauceConnectCommandlineSecrets(args);

    assertEquals(
        "[/sauce/connect/binary/path/, run, --username, username, --access-key, ****, --api-address, :9000, --api-basic-auth, user]",
        result);
  }

  @Test
  void shouldInitLatestVersionLazilyAndOnce() throws IOException, InterruptedException {
    try (MockedStatic<HttpClient> httpClientStaticMock = mockStatic(HttpClient.class)) {
      HttpClient httpClient = mock();
      HttpResponse<String> httpResponse = mock();
      String version = "5.99.99";
      when(httpResponse.body()).thenReturn("{\"download\": {\"version\": \"" + version + "\"}}");
      when(httpClient.send(any(), argThat((ArgumentMatcher<BodyHandler<String>>) argument -> true))).thenReturn(
          httpResponse);
      httpClientStaticMock.when(HttpClient::newHttpClient).thenReturn(httpClient);

      SauceConnectManager sauceConnectManager = new SauceConnectManager();
      sauceConnectManager.setUseLatestSauceConnect(true);

      String currentVersion = sauceConnectManager.getCurrentVersion();
      assertEquals(version, currentVersion);
      httpClientStaticMock.verify(HttpClient::newHttpClient);

      currentVersion = sauceConnectManager.getCurrentVersion();
      assertEquals(version, currentVersion);
      httpClientStaticMock.verifyNoMoreInteractions();
    }
  }
}
