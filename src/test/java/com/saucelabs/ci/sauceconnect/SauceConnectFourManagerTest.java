package com.saucelabs.ci.sauceconnect;

import com.saucelabs.saucerest.SauceREST;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.*;
import java.net.URISyntaxException;

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

    public SauceConnectFourManagerTest() throws URISyntaxException, IOException {
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
    public void teardown() throws Exception {
        verifyNoMoreInteractions(mockSauceRest);
        verify(tunnelManager, times(1)).setSauceRest(mockSauceRest);
    }

    @Test
    public void openConnectionTest_success() throws Exception {
        when(mockProcess.getErrorStream()).thenReturn(new ByteArrayInputStream("".getBytes("UTF-8")));
        when(mockProcess.getInputStream()).thenReturn(getClass().getResourceAsStream("/started_sc.log"));

        Process p = this.tunnelManager.openConnection(
            "fakeuser", "fakeapikey", 12345,
            null, "", ps, false,
            ""
        );

        assertNotNull(p);

        verify(mockSauceRest).getTunnels();
        verify(tunnelManager).createProcess(
            new String[] { anyString(), "-u", "fakeuser", "-k", "fakeapikey", "-P", "12345" },
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
        } catch (Exception e) {
            throw e;
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
    public void testExtractZipFile() throws Exception {
        File linux_destination = folder.newFolder("sauceconnect_linux");
        File linux32_destination = folder.newFolder("sauceconnect_linux32");
        File windows_destination = folder.newFolder("sauceconnect_windows");
        File osx_destination = folder.newFolder("sauceconnect_osx");

        SauceConnectFourManager manager = new SauceConnectFourManager();
        manager.extractZipFile(linux_destination, SauceConnectFourManager.OperatingSystem.LINUX);
        assertTrue("Linux executable exists", new File(
            new File(linux_destination, SauceConnectFourManager.OperatingSystem.LINUX.getDirectory()),
            SauceConnectFourManager.OperatingSystem.LINUX.getExecutable()
        ).exists());

        manager.extractZipFile(linux32_destination, SauceConnectFourManager.OperatingSystem.LINUX32);
        assertTrue("Linux32 executable exists", new File(
            new File(linux32_destination, SauceConnectFourManager.OperatingSystem.LINUX32.getDirectory()),
            SauceConnectFourManager.OperatingSystem.LINUX32.getExecutable()
        ).exists());

        manager.extractZipFile(windows_destination, SauceConnectFourManager.OperatingSystem.WINDOWS);
        assertTrue("windows executable exists", new File(
            new File(windows_destination, SauceConnectFourManager.OperatingSystem.WINDOWS.getDirectory()),
            SauceConnectFourManager.OperatingSystem.WINDOWS.getExecutable()
        ).exists());

        manager.extractZipFile(osx_destination, SauceConnectFourManager.OperatingSystem.OSX);
        assertTrue("osx executable exists", new File(
            new File(osx_destination, SauceConnectFourManager.OperatingSystem.OSX.getDirectory()),
            SauceConnectFourManager.OperatingSystem.OSX.getExecutable()
        ).exists());
    }
}
