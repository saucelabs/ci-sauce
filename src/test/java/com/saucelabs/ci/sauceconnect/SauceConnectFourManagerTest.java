package com.saucelabs.ci.sauceconnect;

import com.saucelabs.ci.sauceconnect.SauceConnectFourManager.OperatingSystem;
import com.saucelabs.saucerest.SauceREST;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class SauceConnectFourManagerTest {

    @Rule public TemporaryFolder folder= new TemporaryFolder();

    @Mock private Process mockProcess;
    @Mock private SauceREST mockSauceRest;
    @Spy private final SauceConnectFourManager tunnelManager = new SauceConnectFourManager(false);

    private final PrintStream ps = System.out;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        tunnelManager.setSauceRest(mockSauceRest);
    }

    @After
    public void teardown() {
        verifyNoMoreInteractions(mockSauceRest);
    }

    private String readResource(String resourceName) throws IOException {
        try (InputStream resourceAsStream = getResourceAsStream(resourceName)) {
            return IOUtils.toString(resourceAsStream, StandardCharsets.UTF_8);
        }
    }

    private InputStream getResourceAsStream(String resourceName) {
        return getClass().getResourceAsStream(resourceName);
    }

    @Test
    public void testOpenConnectionSuccessfullyWithoutCleanUpOnExit() throws IOException {
        testOpenConnectionSuccessfully(false);
    }

    @Test
    public void testOpenConnectionSuccessfullyWithCleanUpOnExit() throws IOException {
        testOpenConnectionSuccessfully(true);
    }

    private void testOpenConnectionSuccessfully(boolean cleanUpOnExit) throws IOException {
        when(mockSauceRest.getTunnels()).thenReturn(readResource("/tunnels_empty.json"));
        tunnelManager.setCleanUpOnExit(cleanUpOnExit);
        Process process = testOpenConnection("/started_sc.log");
        assertEquals(mockProcess, process);
    }

    @Test(expected=AbstractSauceTunnelManager.SauceConnectDidNotStartException.class)
    public void openConnectionTest_closes() throws IOException {
        when(mockSauceRest.getTunnels()).thenReturn(readResource("/tunnels_empty.json"));
        testOpenConnection("/started_sc_closes.log");
    }

    @Test
    public void testOpenConnectionWithExtraSpacesInArgs() throws IOException {
        when(mockSauceRest.getTunnels()).thenReturn(readResource("/tunnels_empty.json"));
        testOpenConnection("/started_sc.log", " username-with-spaces-around ");
    }

    private Process testOpenConnection(String logFile) throws IOException {
        return testOpenConnection(logFile, "fakeuser");
    }

    private Process testDefaultOpenConnection(String logFile, String username) throws IOException {
        final String apiKey = "fakeapikey";
        final int port = 12345;

        try (InputStream resourceAsStream = getResourceAsStream(logFile)) {
            when(mockProcess.getErrorStream()).thenReturn(new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8)));
            when(mockProcess.getInputStream()).thenReturn(resourceAsStream);
            doReturn(mockProcess).when(tunnelManager).createProcess(any(String[].class), any(File.class));
            return tunnelManager.openConnection(username, apiKey, port, null, "  ", ps, false, "");
        } finally {
            verify(mockSauceRest).getTunnels();
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

    private Process testOpenConnection(String logFile, String username) throws IOException {
        final String apiKey = "fakeapikey";
        final int port = 12345;
        final String dataCenter = "US";

        try (InputStream resourceAsStream = getResourceAsStream(logFile)) {
            when(mockProcess.getErrorStream()).thenReturn(new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8)));
            when(mockProcess.getInputStream()).thenReturn(resourceAsStream);
            doReturn(mockProcess).when(tunnelManager).createProcess(any(String[].class), any(File.class));
            return tunnelManager.openConnection(username, apiKey, dataCenter, port, null, "  ", ps, false, "");
        } finally {
            verify(mockSauceRest).getTunnels();
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
    public void openConnectionTest_existing_tunnel() throws IOException {
        when(mockSauceRest.getTunnels()).thenReturn(readResource("/tunnels_active_tunnel.json"));
        when(mockSauceRest.getTunnelInformation("8949e55fb5e14fd6bf6230b7a609b494")).thenReturn(
            readResource("/single_tunnel.json"));

        Process process = testOpenConnection("/started_sc.log");
        assertEquals(mockProcess, process);

        verify(mockSauceRest).getTunnelInformation("8949e55fb5e14fd6bf6230b7a609b494");
        verify(mockSauceRest).getTunnels();
    }

    @Test
    public void testExtractZipFileWithoutCleanUpOnExit() throws IOException {
        testExtractZipFile(false);
    }

    @Test
    public void testExtractZipFileWithCleanUpOnExit() throws IOException {
        testExtractZipFile(true);
    }

    private void testExtractZipFile(boolean cleanUpOnExit) throws IOException
    {
        File linux_destination = folder.newFolder("sauceconnect_linux");
        File windows_destination = folder.newFolder("sauceconnect_windows");
        File osx_destination = folder.newFolder("sauceconnect_osx");

        SauceConnectFourManager manager = new SauceConnectFourManager();
        manager.setCleanUpOnExit(cleanUpOnExit);

        manager.extractZipFile(linux_destination, OperatingSystem.LINUX);
        assertSauceConnectFileExists("Linux executable exists", linux_destination, OperatingSystem.LINUX);

        manager.extractZipFile(windows_destination, OperatingSystem.WINDOWS);
        assertSauceConnectFileExists("windows executable exists", windows_destination, OperatingSystem.WINDOWS);

        manager.extractZipFile(osx_destination, OperatingSystem.OSX);
        assertSauceConnectFileExists("osx executable exists", osx_destination, OperatingSystem.OSX);
    }

    private void assertSauceConnectFileExists(String message, File destination, OperatingSystem os) {
        assertTrue(message, new File(new File(destination, os.getDirectory(false)), os.getExecutable()).exists());
    }

    @Test
    public void testSauceConnectSecretsCoveredWithStars() {
        SauceConnectFourManager manager = new SauceConnectFourManager();
        String[] args = { "/sauce/connect/binary/path/" };
        args = manager.generateSauceConnectArgs(
            args,
            "username",
            "apiKey-apiKey-apiKey-apiKey-apiKey",
            1234,
            "--api-key apiKey-apiKey-apiKey-apiKey-apiKey -w user:pwd --proxy-userpwd user:pwd -a host:8080:user:pwd --auth host:8080:user:pwd -p host:8080 --proxy host:8080 -o pwd --other pwd"
        );
        String result = manager.hideSauceConnectCommandlineSecrets(args);

        assertEquals("[/sauce/connect/binary/path/, -u, username, -k, ****, -P, 1234, --api-key, ****, -w, user:****, --proxy-userpwd, user:****, -a, host:8080:user:****, --auth, host:8080:user:****, -p, host:8080, --proxy, host:8080, -o, pwd, --other, pwd]", result);
    }

    @Test
    public void testSauceConnectSecretsWithSpecialCharactersCoveredWithStars() {
        SauceConnectFourManager manager = new SauceConnectFourManager();
        String[] args = {"-a", "web-proxy.domain.com:8080:user:pwd"};
        assertEquals("[-a, web-proxy.domain.com:8080:user:****]", manager.hideSauceConnectCommandlineSecrets(args));

        args = new String[]{"-a", "host:8080:user:passwd%#123"};
        assertEquals("[-a, host:8080:user:****]", manager.hideSauceConnectCommandlineSecrets(args));

        args = new String[]{"-a", "host:8080:super-user:passwd"};
        assertEquals("[-a, host:8080:super-user:****]", manager.hideSauceConnectCommandlineSecrets(args));

        args = new String[]{"-w", "user:passwd%#123"};
        assertEquals("[-w, user:****]", manager.hideSauceConnectCommandlineSecrets(args));

        args = new String[]{"-w", "super-user:passwd"};
        assertEquals("[-w, super-user:****]", manager.hideSauceConnectCommandlineSecrets(args));
    }
}
