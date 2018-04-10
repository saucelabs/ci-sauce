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
public class BuildInformationTest {
    BuildInformation build;
    JSONObject jobs;

    @Before
    public void setUp() throws Exception {
        JSONObject obj = new JSONObject(
            IOUtils.toString(getClass().getResourceAsStream("/build_info.json"), "UTF-8")
        );
        build = new BuildInformation("1234");
        build.populateFromJson(obj);
    }

    @Test
    public void testBuildId() throws Exception {
        assertEquals("1234", build.getBuildId());
    }

    @Test
    public void testStatus() throws Exception {
        assertEquals("failed", build.getStatus());

        build.clearChanges();
        assertFalse(build.hasChanges());
        build.setStatus("gavin");
        assertEquals("gavin", build.getStatus());
        assertTrue(build.hasChange("status"));

        build.clearChanges();
        assertFalse(build.hasChanges());
        build.setStatus("passed");
        assertEquals("passed", build.getStatus());
        build.setStatus("failed");
        assertEquals("failed", build.getStatus());
        build.setStatus("error");
        assertEquals("error", build.getStatus());
        assertTrue(build.hasChange("status"));
    }

    @Test
    public void testPopulateFromJson() throws Exception {
        assertEquals("test-name", build.getName());

        jobs = new JSONObject();
        jobs.put("finished", 7);
        jobs.put("passed", 5);
        jobs.put("failed", 2);
        jobs.put("errored", 0);

        JSONObject json = new JSONObject();
        json.put("name", (String) null);
        json.put("build", (String) null);
        json.put("jobs", jobs);
        json.put("start_time", 1448576067);
        json.put("end_time", 1448576078);

        /* Name */
        json.put("name", (String) null);
        build = new BuildInformation("1234");
        build.populateFromJson(json);
        assertEquals(null, build.getName());

        json.put("name", "Something");
        build = new BuildInformation("1234");
        build.populateFromJson(json);
        assertEquals("Something", build.getName());

        /* Status */
        json.put("status", (String) null);
        build = new BuildInformation("1234");
        build.populateFromJson(json);
        assertEquals(null, build.getStatus());

        json.put("status", "success");
        build = new BuildInformation("1234");
        build.populateFromJson(json);
        assertEquals("success", build.getStatus());

        json.put("status", "failed");
        build = new BuildInformation("1234");
        build.populateFromJson(json);
        assertEquals("failed", build.getStatus());

        json.put("status", "error");
        build = new BuildInformation("1234");
        build.populateFromJson(json);
        assertEquals("error", build.getStatus());

        /* Jobs */
        jobs = new JSONObject();
        jobs.put("finished", 7);
        jobs.put("passed", 5);
        jobs.put("failed", 2);
        jobs.put("errored", 0);

        json.put("jobs", jobs);
        build = new BuildInformation("1234");
        build.populateFromJson(json);
        assertEquals(7, build.getJobsFinished());
        assertEquals(5, build.getJobsPassed());
        assertEquals(2, build.getJobsFailed());
        assertEquals(0, build.getJobsErrored());

        /* Time */
        json.put("start_time", 1448576000);
        json.put("end_time", 1448576100);
        build = new BuildInformation("1234");
        build.populateFromJson(json);
        assertEquals(1448576000, build.getStartTime());
        assertEquals(1448576100, build.getEndTime());
    }

    @Test
    public void testBuildName() throws Exception {
        HashMap<String, Object> updates = new HashMap<String, Object>();
        updates.put("name", "Gavin's first build");

        assertEquals("test-name", build.getName());
        assertFalse(build.hasChanges());
        build.setName(null);
        assertFalse(build.hasBuildName());
        build.setName("");
        assertFalse(build.hasBuildName());
        build.setName("null");
        assertFalse(build.hasBuildName());
        build.setName("Gavin's first build");
        assertTrue(build.hasBuildName());
        assertTrue(build.hasChanges());
        assertEquals(updates, build.getChanges());
    }

    @Test
    public void testGetChange() throws Exception {
        HashMap<String, Object> updates = new HashMap<String, Object>();
        updates.put("name", "name-name-name");

        assertEquals("test-name", build.getName());
        assertFalse(build.hasChanges());
        build.setName("name-name-name");
        assertTrue(build.hasChanges());
        assertEquals(updates, build.getChanges());
    }

    /* TODO - figure out how to test equals */
}
