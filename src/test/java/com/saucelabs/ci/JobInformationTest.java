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
    private JobInformation job;

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
        assertEquals("hmac", job.getHmac());
        job.setHmac("newhmac");
        assertEquals("newhmac", job.getHmac());
    }

    @Test
    public void testJobId() throws Exception {
        assertEquals("1234", job.getJobId());
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
        assertEquals("Passed", job.getStatus());
        job.setStatus(false);
        assertEquals("Failed", job.getStatus());
        assertTrue(job.hasChange("status"));
    }

    @Test
    public void testPopulateFromJson() throws Exception {
        assertFalse(job.hasJobName());
        assertNull(job.getName());

        JSONObject json = new JSONObject();
        json.put("passed", (String) null);
        json.put("name", (String) null);
        json.put("build", (String) null);
        json.put("os", "Windows 2012 R2");
        json.put("browser", "");
        json.put("browser_short_version", "");
        json.put("video_url", "");
        json.put("log_url", "");
        json.put("start_time", 1448576067);
        json.put("end_time", 1448576078);

        /* Name */
        json.put("name", (String) null);
        job = new JobInformation("1234", "hmac");
        job.populateFromJson(json);
        assertEquals(null, job.getName());

        json.put("name", "Something");
        job = new JobInformation("1234", "hmac");
        job.populateFromJson(json);
        assertEquals("Something", job.getName());

        /* Build */
        json.put("build", (String) null);
        job = new JobInformation("1234", "hmac");
        job.populateFromJson(json);
        assertEquals(null, job.getBuild());

        json.put("build", "Something");
        job = new JobInformation("1234", "hmac");
        job.populateFromJson(json);
        assertEquals("Something", job.getBuild());

        /* Passed */
        json.put("passed", (String) null);
        job = new JobInformation("1234", "hmac");
        job.populateFromJson(json);
        assertEquals(null, job.getStatus());

        json.put("passed", true);
        job = new JobInformation("1234", "hmac");
        job.populateFromJson(json);
        assertEquals("Passed", job.getStatus());

        json.put("passed", false);
        job = new JobInformation("1234", "hmac");
        job.populateFromJson(json);
        assertEquals("Failed", job.getStatus());

        json.put("start_time", 1448576000);
        json.put("end_time", 1448576100);
        job = new JobInformation("1234", "hmac");
        job.populateFromJson(json);
        assertEquals(1448576000, job.getStartTime());
        assertEquals(1448576100, job.getEndTime());
        assertEquals(100, job.getDuration());

        JSONObject customData = new JSONObject();
        customData.put("FAILURE_MESSAGE", "test failure");
        json.put("custom-data",customData);
        job = new JobInformation("1234", "hmac");
        job.populateFromJson(json);
        assertEquals("test failure", job.getFailureMessage());
    }

    @Test
    public void testFailureMessage() throws Exception {
        HashMap<String, Object> updates = new HashMap<String, Object>();
        updates.put("failureMessage", "test failure");

        assertNull(job.getFailureMessage());
        assertFalse(job.hasFailureMessage());
        assertFalse(job.hasChanges());
        job.setFailureMessage(null);
        assertFalse(job.hasFailureMessage());
        job.setFailureMessage("");
        assertFalse(job.hasFailureMessage());
        job.setFailureMessage("null");
        assertFalse(job.hasFailureMessage());
        job.setFailureMessage("test failure");
        assertTrue(job.hasFailureMessage());
        assertTrue(job.hasChanges());
        assertEquals(updates, job.getChanges());
    }

    @Test
    public void testJobName() throws Exception {
        HashMap<String, Object> updates = new HashMap<String, Object>();
        updates.put("name", "Gavin's first job");

        assertFalse(job.hasJobName());
        assertNull(job.getName());
        assertFalse(job.hasChanges());
        job.setName(null);
        assertFalse(job.hasJobName());
        job.setName("");
        assertFalse(job.hasJobName());
        job.setName("null");
        assertFalse(job.hasJobName());
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
        assertNull(job.getBuild());
        assertFalse(job.hasChanges());

        job.setBuild(null);
        assertFalse(job.hasBuild());
        job.setBuild("");
        assertFalse(job.hasBuild());
        job.setBuild("null");
        assertFalse(job.hasBuild());

        job.setBuild("build-name");
        assertTrue(job.hasChanges());
        assertTrue(job.hasBuild());

        assertEquals(updates, job.getChanges());
    }

    @Test
    public void testBrowser() throws Exception {
        HashMap<String, Object> updates = new HashMap<String, Object>();
        updates.put("browser", "firefox");

        assertEquals("iexplore", job.getBrowser());
        assertFalse(job.hasChanges());

        job.setBrowser("firefox");
        assertTrue(job.hasChanges());
        assertEquals("firefox", job.getBrowser());
        assertTrue(job.hasChange("browser"));

        assertEquals(updates, job.getChanges());
    }

    @Test
    public void testVersion() throws Exception {
        HashMap<String, Object> updates = new HashMap<String, Object>();
        updates.put("version", "20");

        assertEquals("10", job.getVersion());
        assertFalse(job.hasChanges());

        job.setVersion("20");
        assertTrue(job.hasChanges());
        assertEquals("20", job.getVersion());
        assertTrue(job.hasChange("version"));

        assertEquals(updates, job.getChanges());
    }

    @Test
    public void testOs() throws Exception {
        HashMap<String, Object> updates = new HashMap<String, Object>();
        updates.put("os", "20");

        assertEquals("Windows 7", job.getOs());
        assertFalse(job.hasChanges());

        /* Test random os */
        job.setOs("20");
        assertTrue(job.hasChanges());
        assertEquals("20", job.getOs());
        assertTrue(job.hasChange("os"));

        assertEquals(updates, job.getChanges());

        /* Test mapped os */
        job.setOs("Windows 2012 R2");
        assertTrue(job.hasChanges());
        assertEquals("Windows 8.1", job.getOs());
        assertTrue(job.hasChange("os"));

        updates.put("os", "Windows 2012 R2");
        assertEquals(updates, job.getChanges());
    }

    @Test
    public void testVideoUrl() throws Exception {
        HashMap<String, Object> updates = new HashMap<String, Object>();
        updates.put("videoUrl", "20");

        assertEquals(
            "https://saucelabs.com/jobs/449f8e8f5940483ea6938ce6cdbea117/video.flv",
            job.getVideoUrl()
        );
        assertFalse(job.hasChanges());

        job.setVideoUrl("20");
        assertTrue(job.hasChanges());
        assertEquals("20", job.getVideoUrl());
        assertTrue(job.hasChange("videoUrl"));

        assertEquals(updates, job.getChanges());
    }

    @Test
    public void testLogUrl() throws Exception {
        HashMap<String, Object> updates = new HashMap<String, Object>();
        updates.put("logUrl", "20");

        assertEquals(
            "https://saucelabs.com/jobs/449f8e8f5940483ea6938ce6cdbea117/selenium-server.log",
            job.getLogUrl()
        );
        assertFalse(job.hasChanges());

        job.setLogUrl("20");
        assertTrue(job.hasChanges());
        assertEquals("20", job.getLogUrl());
        assertTrue(job.hasChange("logUrl"));

        assertEquals(updates, job.getChanges());
    }


    @Test
    public void testGetChange() throws Exception {
        HashMap<String, Object> updates = new HashMap<String, Object>();
        updates.put("build", "build-name");
        updates.put("name", "name-name-name");

        assertNull(job.getName());
        assertFalse(job.hasChanges());
        job.setBuild("build-name");
        job.setName("name-name-name");
        assertTrue(job.hasChanges());
        assertEquals(updates, job.getChanges());
    }

    /* TODO - figure out how to test equals */
}
