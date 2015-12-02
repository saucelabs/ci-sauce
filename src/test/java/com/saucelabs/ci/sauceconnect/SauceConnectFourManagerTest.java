package com.saucelabs.ci.sauceconnect;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.saucelabs.saucerest.SauceREST;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.*;
import java.net.URISyntaxException;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class SauceConnectFourManagerTest {

    private SauceConnectFourManager tunnelManager;

    private Process mockProcess;

    private SauceREST mockSauceRest;

    private final String STRING_JSON_TUNNELS_EMPTY;
    private final String STRING_JSON_TUNNELS_ACTIVE;


    public SauceConnectFourManagerTest() throws URISyntaxException, IOException {
        STRING_JSON_TUNNELS_ACTIVE = Resources.toString(getClass().getResource("/tunnels_active_tunnel.json").toURI().toURL(), Charsets.UTF_8);
        STRING_JSON_TUNNELS_EMPTY = Resources.toString(getClass().getResource("/tunnels_empty.json").toURI().toURL(), Charsets.UTF_8);
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
            null, "", mock(PrintStream.class), false,
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

        File sauceConnectJar = null;
        String options = "";
        PrintStream printStream = mock(PrintStream.class);
        Boolean verboseLogging = true;
        String sauceConnectPath = "";

        when(mockProcess.getErrorStream()).thenReturn(new ByteArrayInputStream("".getBytes("UTF-8")));
        when(mockProcess.getInputStream()).thenReturn(getClass().getResourceAsStream("/started_sc_closes.log"));

        try {
            Process p = this.tunnelManager.openConnection(
                "fakeuser", "fakeapikey", 12345,
                null, "", mock(PrintStream.class), false,
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
        doReturn(STRING_JSON_TUNNELS_ACTIVE).when(mockSauceRest).getTunnelInformation("8949e55fb5e14fd6bf6230b7a609b494");

        when(mockProcess.getErrorStream()).thenReturn(new ByteArrayInputStream("".getBytes("UTF-8")));
        when(mockProcess.getInputStream()).thenReturn(getClass().getResourceAsStream("/started_sc.log"));

        Process p = this.tunnelManager.openConnection(
            "fakeuser", "fakeapikey", 12345,
            null, "", mock(PrintStream.class), false,
            ""
        );

        assertNotNull(p);

        verify(mockSauceRest).getTunnels();
        verify(tunnelManager).createProcess(
            new String[] { anyString(), "-u", "fakeuser", "-k", "fakeapikey", "-P", "12345" },
            new File( anyString() )
        );
    }
}
