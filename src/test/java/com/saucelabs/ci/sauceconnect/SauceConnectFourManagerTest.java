package com.saucelabs.ci.sauceconnect;

import com.saucelabs.ci.sauceconnect.SauceConnectFourManager.OperatingSystem;
import com.saucelabs.saucerest.SauceREST;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class SauceConnectFourManagerTest {

    private SauceConnectFourManager tunnelManager;

    private Process mockProcess;

    private SauceREST mockSauceRest;

    private PrintStream ps = System.out;

    private final String STRING_JSON_TUNNELS_EMPTY;
    private final String STRING_JSON_TUNNELS_ACTIVE;
    private final String STRING_JSON_GET_TUNNEL;

    public SauceConnectFourManagerTest() throws IOException {
        STRING_JSON_TUNNELS_ACTIVE = IOUtils.toString(getClass().getResourceAsStream("/tunnels_active_tunnel.json"), "UTF-8");
        STRING_JSON_TUNNELS_EMPTY = IOUtils.toString(getClass().getResourceAsStream("/tunnels_empty.json"), "UTF-8");
        STRING_JSON_GET_TUNNEL = IOUtils.toString(getClass().getResourceAsStream("/single_tunnel.json"), "UTF-8");
    }


    @Before
    public void setup() throws Exception {
        mockProcess = mock(Process.class);
        tunnelManager = spy(new SauceConnectFourManager(false));
        doReturn(mockProcess).when(tunnelManager).createProcess(any(String[].class), any(File.class));
        mockSauceRest = mock(SauceREST.class);
        tunnelManager.setSauceRest(mockSauceRest);
        doReturn(STRING_JSON_TUNNELS_EMPTY).when(mockSauceRest).getTunnels();
    }

    @After
    public void teardown() {
        verifyNoMoreInteractions(mockSauceRest);
        verify(tunnelManager, times(1)).setSauceRest(mockSauceRest);
    }

    @Test
    public void testOpenConnectionSuccessfullyWithoutCleanUpOnExit() throws Exception {
        testOpenConnectionSuccessfully(false);
    }

    @Test
    public void testOpenConnectionSuccessfullyWithCleanUpOnExit() throws Exception {
        testOpenConnectionSuccessfully(true);
    }

    private void testOpenConnectionSuccessfully(boolean cleanUpOnExit) throws IOException {
        when(mockProcess.getErrorStream()).thenReturn(new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8)));
        when(mockProcess.getInputStream()).thenReturn(getClass().getResourceAsStream("/started_sc.log"));

        String username = "fakeuser";
        String apiKey = "fakeapikey";

        tunnelManager.setCleanUpOnExit(cleanUpOnExit);
        Process p = tunnelManager.openConnection(username, apiKey, 12345,
            null, "", ps, false,
            ""
        );

        assertNotNull(p);

        verify(mockSauceRest).getTunnels();
        verify(tunnelManager).createProcess(
            new String[] { anyString(), "-u", username, "-k", apiKey, "-P", "12345" },
            new File(anyString())
        );
    }

    @Test(expected=AbstractSauceTunnelManager.SauceConnectDidNotStartException.class)
    public void openConnectionTest_closes() throws Exception {
        when(mockProcess.getErrorStream()).thenReturn(new ByteArrayInputStream("".getBytes("UTF-8")));
        when(mockProcess.getInputStream()).thenReturn(getClass().getResourceAsStream("/started_sc_closes.log"));

        try {
            this.tunnelManager.openConnection(
                "fakeuser", "fakeapikey", 12345,
                null, "", ps, false,
                ""
            );
        } finally {
            verify(mockSauceRest).getTunnels();
            verify(tunnelManager).createProcess(
                new String[]{anyString(), "-u", "fakeuser", "-k", "fakeapikey", "-P", "12345"},
                new File(anyString())
            );
        }
    }

    @Test
    public void openConnectionTest_existing_tunnel() throws Exception {
        doReturn(STRING_JSON_TUNNELS_ACTIVE).when(mockSauceRest).getTunnels();
        doReturn(STRING_JSON_GET_TUNNEL).when(mockSauceRest).getTunnelInformation("8949e55fb5e14fd6bf6230b7a609b494");

        when(mockProcess.getErrorStream()).thenReturn(new ByteArrayInputStream("".getBytes("UTF-8")));
        when(mockProcess.getInputStream()).thenReturn(getClass().getResourceAsStream("/started_sc.log"));

        Process p = this.tunnelManager.openConnection(
            "fakeuser", "fakeapikey", 12345,
            null, "", ps, false,
            ""
        );

        assertNotNull(p);

        verify(mockSauceRest).getTunnelInformation("8949e55fb5e14fd6bf6230b7a609b494");
        verify(mockSauceRest).getTunnels();
        verify(tunnelManager).createProcess(
            new String[] { anyString(), "-u", "fakeuser", "-k", "fakeapikey", "-P", "12345" },
            new File( anyString() )
        );
    }

    @Rule
    public TemporaryFolder folder= new TemporaryFolder();

    @Test
    public void testExtractZipFileWithoutCleanUpOnExit() throws Exception {
        testExtractZipFile(false);
    }

    @Test
    public void testExtractZipFileWithCleanUpOnExit() throws Exception {
        testExtractZipFile(true);
    }

    private void testExtractZipFile(boolean cleanUpOnExit) throws IOException
    {
        File linux_destination = folder.newFolder("sauceconnect_linux");
        File linux32_destination = folder.newFolder("sauceconnect_linux32");
        File windows_destination = folder.newFolder("sauceconnect_windows");
        File osx_destination = folder.newFolder("sauceconnect_osx");

        SauceConnectFourManager manager = new SauceConnectFourManager();
        manager.setCleanUpOnExit(cleanUpOnExit);

        manager.extractZipFile(linux_destination, OperatingSystem.LINUX);
        assertSauceConnectFileExists("Linux executable exists", linux_destination, OperatingSystem.LINUX);

        manager.extractZipFile(linux32_destination, OperatingSystem.LINUX32);
        assertSauceConnectFileExists("Linux32 executable exists", linux32_destination, OperatingSystem.LINUX32);

        manager.extractZipFile(windows_destination, OperatingSystem.WINDOWS);
        assertSauceConnectFileExists("windows executable exists", windows_destination, OperatingSystem.WINDOWS);

        manager.extractZipFile(osx_destination, OperatingSystem.OSX);
        assertSauceConnectFileExists("osx executable exists", osx_destination, OperatingSystem.OSX);
    }

    private void assertSauceConnectFileExists(String message, File destination, OperatingSystem os) {
        assertTrue(message, new File(new File(destination, os.getDirectory(false)), os.getExecutable()).exists());
    }

    @Test
    public void testSauceConnectSecretsCoveredWithStars() throws Exception {
        SauceConnectFourManager manager = new SauceConnectFourManager();
        String[] args = { "/sauce/connect/binary/path/" };
        args = manager.generateSauceConnectArgs(
            args,
            "username",
            "apiKey-apiKey-apiKey-apiKey-apiKey",
            1234,
            "--api-key apiKey-apiKey-apiKey-apiKey-apiKey -w user:pwd --proxy-userpwd user:pwd -a host:8080:user:pwd --auth host:8080:user:pwd -p host:8080 --proxy host:8080 -o pwd --other pwd"
        );
        String result = manager.hideSauceConnectCommandlineSecrets(Arrays.toString(args));

        assertEquals("[/sauce/connect/binary/path/, -u, username, -k, ****, -P, 1234, --api-key, ****, -w, user:****, --proxy-userpwd, user:****, -a, host:8080:user:****, --auth, host:8080:user:****, -p, host:8080, --proxy, host:8080, -o, pwd, --other, pwd]", result);
    }

    @Test
    public void testSauceConnectSecretsWithSpecialCharactersCoveredWithStars() throws Exception {
        SauceConnectFourManager manager = new SauceConnectFourManager();
        assertEquals("[-a, web-proxy.domain.com:8080:user:****]", manager.hideSauceConnectCommandlineSecrets("[-a, web-proxy.domain.com:8080:user:pwd]"));
    }
}
