package com.saucelabs.ci;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.EnumSet;
import java.util.HashMap;

import static org.junit.Assert.*;

public class BuildJobInformationTest {
    private BuildJobInformation job;

    @Before
    public void setUp() throws Exception {
        JSONObject obj = new JSONObject(
            IOUtils.toString(getClass().getResourceAsStream("/build_job.json"), "UTF-8")
        );
        job = new BuildJobInformation(obj);
    }

    @Test
    public void testBuildJobInformation_ConstructorFromJSON() throws Exception {
        JSONObject obj = new JSONObject(
            IOUtils.toString(getClass().getResourceAsStream("/build_job.json"), "UTF-8")
        );
        job = new BuildJobInformation(obj);

        assertEquals(1641976754, job.getCreationTime());
        assertEquals(1643620921, job.getModificationTime());
        assertEquals(0, job.getDeletionTime());
        assertEquals(
            EnumSet.of(BuildJobInformation.JobStatusFlag.FINISHED, BuildJobInformation.JobStatusFlag.PASSED),
            job.getStatus()
        );
    }

    @Test
    public void testBuildJobInformation_ConstructorFromId() {
        job = new BuildJobInformation("1234");

        assertEquals("1234", job.getJobId());
        assertEquals(0, job.getCreationTime());
        assertEquals(0, job.getModificationTime());
        assertEquals(0, job.getDeletionTime());
        assertEquals(EnumSet.noneOf(BuildJobInformation.JobStatusFlag.class), job.getStatus());
    }

    @Test
    public void testJobId() {
        assertEquals("1375ba7db500432a97fbd0d99ce2bff2", job.getJobId());
    }

    @Test
    public void testSetGetCreationTime() {
        job.setCreationTime(25);

        assertEquals(25, job.getCreationTime());
    }

    @Test
    public void testSetGetModificationTime() {
        job.setModificationTime(26);

        assertEquals(26, job.getModificationTime());
    }

    @Test
    public void testSetGetDeletionTime() {
        job.setDeletionTime(26);

        assertEquals(26, job.getDeletionTime());
    }

    @Test
    public void testSetStatusFlag() {
        assertFalse(job.getStatus().contains(BuildJobInformation.JobStatusFlag.PUBLIC));
        assertFalse(job.hasChanges());

        job.setStatusFlag(BuildJobInformation.JobStatusFlag.PUBLIC);

        assertTrue(job.getStatus().contains(BuildJobInformation.JobStatusFlag.PUBLIC));
        assertTrue(job.getChanges().containsKey("status"));
    }

    @Test
    public void testClearStatusFlag() {
        job.setStatusFlag(BuildJobInformation.JobStatusFlag.PUBLIC);
        job.clearChanges();
        assertTrue(job.getStatus().contains(BuildJobInformation.JobStatusFlag.PUBLIC));
        assertFalse(job.hasChanges());

        job.clearStatusFlag(BuildJobInformation.JobStatusFlag.PUBLIC);

        assertFalse(job.getStatus().contains(BuildJobInformation.JobStatusFlag.PUBLIC));
        assertTrue(job.getChanges().containsKey("status"));
    }

    @Test
    public void testPopulateFromJson() {
        JSONObject states = new JSONObject();
        states.put("completed", true);
        states.put("errored", true);
        states.put("failed", true);
        states.put("finished", true);
        states.put("new", true);
        states.put("passed", true);
        states.put("public", true);
        states.put("queued", true);
        states.put("running", true);

        JSONObject json = new JSONObject();
        json.put("id", "1234");
        json.put("creation_time", 10);
        json.put("modification_time", 11);
        json.put("deletion_time", 12);
        json.put("state", states);

        BuildJobInformation job = new BuildJobInformation("1234");
        job.populateFromJson(json);

        assertEquals(10, job.getCreationTime());
        assertEquals(11, job.getModificationTime());
        assertEquals(12, job.getDeletionTime());
        assertEquals(EnumSet.allOf(BuildJobInformation.JobStatusFlag.class), job.getStatus());
    }

    @Test
    public void testGetChange() {
        HashMap<String, Object> updates = new HashMap<>();
        updates.put("creationTime", (long) 1);

        assertFalse(job.hasChanges());
        job.setCreationTime(1);
        assertTrue(job.hasChanges());
        assertEquals(updates, job.getChanges());
    }
}
