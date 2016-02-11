package com.saucelabs.ci;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.*;

/**
 * Created by gavinmogan on 2016-02-10.
 */
public class JobInformationTest {

    @Test
    public void testPopulateFromJson() throws Exception {

        JSONObject obj = new JSONObject(
            IOUtils.toString(getClass().getResourceAsStream("/job_info.json"), "UTF-8")
        );
        JobInformation job = new JobInformation("1234", "hmac");
        job.populateFromJson(obj);
        assertEquals(job.getName(), null);
    }

    @Test
    public void testGetChanges_jobName() throws Exception {
        HashMap<String, Object> updates = new HashMap<String, Object>();
        updates.put("name", "Gavin's first job");

        JobInformation job = new JobInformation("1234", "hmac");

        assertEquals(job.getName(), null);
        job.setName("Gavin's first job");
        assertEquals(job.getChanges(), updates);
    }


    @Test
    public void testGetChanges_build() throws Exception {
        JobInformation job = new JobInformation("1234", "hmac");
        HashMap<String, Object> updates = new HashMap<String, Object>();
        updates.put("build", "build-name");

        assertEquals(job.getName(), null);
        job.setBuild("build-name");
        assertEquals(job.getChanges(), updates);
    }

    @Test
    public void testGetChanges_combo() throws Exception {
        JobInformation job = new JobInformation("1234", "hmac");
        HashMap<String, Object> updates = new HashMap<String, Object>();
        updates.put("build", "build-name");
        updates.put("name", "name-name-name");


        assertEquals(job.getName(), null);
        job.setBuild("build-name");
        job.setName("name-name-name");
        assertEquals(job.getChanges(), updates);
    }
}
