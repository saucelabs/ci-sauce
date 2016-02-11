package com.saucelabs.ci;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.*;

/**
 * Created by gavinmogan on 2016-02-10.
 */
public class JobInformationTest {
    JobInformation job;

    @Before
    public void setUp() throws Exception {
        JSONObject obj = new JSONObject(
            IOUtils.toString(getClass().getResourceAsStream("/job_info.json"), "UTF-8")
        );
        job = new JobInformation("1234", "hmac");
        job.populateFromJson(obj);
    }

    @Test
    public void testHMAC() throws Exception {
        assertEquals(job.getHmac(), "hmac");
        job.setHmac("newhmac");
        assertEquals(job.getHmac(), "newhmac");
    }

    @Test
    public void testJobId() throws Exception {
        assertEquals(job.getJobId(), "1234");
    }

    @Test
    public void testStatus() throws Exception {
        assertEquals("Passed", job.getStatus());

        job.clearChanges();
        assertFalse(job.hasChanges());
        job.setStatus("gavin");
        assertEquals("gavin", job.getStatus());
        assertTrue(job.hasChange("status"));

        job.clearChanges();
        assertFalse(job.hasChanges());
        job.setStatus(true);
        assertEquals("passed", job.getStatus());
        assertTrue(job.hasChange("status"));
    }

    @Test
    public void testPopulateFromJson() throws Exception {
        assertFalse(job.hasJobName());
        assertEquals(job.getName(), null);
    }

    @Test
    public void testJobName() throws Exception {
        HashMap<String, Object> updates = new HashMap<String, Object>();
        updates.put("name", "Gavin's first job");

        assertFalse(job.hasJobName());
        assertEquals(job.getName(), null);
        assertFalse(job.hasChanges());
        job.setName("Gavin's first job");
        assertTrue(job.hasJobName());
        assertTrue(job.hasChanges());
        assertEquals(updates, job.getChanges());
    }

    @Test
    public void testBuildName() throws Exception {
        HashMap<String, Object> updates = new HashMap<String, Object>();
        updates.put("build", "build-name");

        assertFalse(job.hasBuild());
        assertEquals(job.getBuild(), null);
        assertFalse(job.hasChanges());

        job.setBuild("build-name");
        assertTrue(job.hasChanges());
        assertTrue(job.hasBuild());

        assertEquals(updates, job.getChanges());
    }

    @Test
    public void testBrowser() throws Exception {
        HashMap<String, Object> updates = new HashMap<String, Object>();
        updates.put("browser", "firefox");

        assertEquals(job.getBrowser(), "iexplore");
        assertFalse(job.hasChanges());

        job.setBrowser("firefox");
        assertTrue(job.hasChanges());
        assertEquals(job.getBrowser(), "firefox");
        assertTrue(job.hasChange("browser"));

        assertEquals(updates, job.getChanges());
    }

    @Test
    public void testVersion() throws Exception {
        HashMap<String, Object> updates = new HashMap<String, Object>();
        updates.put("version", "20");

        assertEquals(job.getVersion(), "10");
        assertFalse(job.hasChanges());

        job.setVersion("20");
        assertTrue(job.hasChanges());
        assertEquals(job.getVersion(), "20");
        assertTrue(job.hasChange("version"));

        assertEquals(updates, job.getChanges());
    }

    @Test
    public void testVideoUrl() throws Exception {
        HashMap<String, Object> updates = new HashMap<String, Object>();
        updates.put("videoUrl", "20");

        assertEquals(job.getVideoUrl(), "https://saucelabs.com/jobs/449f8e8f5940483ea6938ce6cdbea117/video.flv");
        assertFalse(job.hasChanges());

        job.setVideoUrl("20");
        assertTrue(job.hasChanges());
        assertEquals(job.getVideoUrl(), "20");
        assertTrue(job.hasChange("videoUrl"));

        assertEquals(updates, job.getChanges());
    }

    @Test
    public void testLogUrl() throws Exception {
        HashMap<String, Object> updates = new HashMap<String, Object>();
        updates.put("logUrl", "20");

        assertEquals(job.getLogUrl(), "https://saucelabs.com/jobs/449f8e8f5940483ea6938ce6cdbea117/selenium-server.log");
        assertFalse(job.hasChanges());

        job.setLogUrl("20");
        assertTrue(job.hasChanges());
        assertEquals(job.getLogUrl(), "20");
        assertTrue(job.hasChange("logUrl"));

        assertEquals(updates, job.getChanges());
    }

    @Test
    public void getChange() throws Exception {
        HashMap<String, Object> updates = new HashMap<String, Object>();
        updates.put("build", "build-name");
        updates.put("name", "name-name-name");

        assertEquals(job.getName(), null);
        assertFalse(job.hasChanges());
        job.setBuild("build-name");
        job.setName("name-name-name");
        assertTrue(job.hasChanges());
        assertEquals(updates, job.getChanges());
    }

    /* TODO - figure out how to test equals */
}
