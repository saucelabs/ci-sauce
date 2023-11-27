package com.saucelabs.ci;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/** Created by gavinmogan on 2016-02-10. */
class BuildInformationTest {
  private BuildInformation build;

    @BeforeEach
  void beforeEach() throws Exception {
    JSONObject obj =
        new JSONObject(
            new String(getClass().getResourceAsStream("/build_info.json").readAllBytes(), StandardCharsets.UTF_8));
    build = new BuildInformation("1234");
    build.populateFromJson(obj);
  }

  @Test
  void testBuildId() {
    assertEquals("1234", build.getBuildId());
  }

  @Test
  void testStatus() {
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
  void testPopulateFromJson() {
    assertEquals("test-name", build.getName());

    JSONObject jobs = new JSONObject();
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
    assertNull(build.getName());

    json.put("name", "Something");
    build = new BuildInformation("1234");
    build.populateFromJson(json);
    assertEquals("Something", build.getName());

    /* Status */
    json.put("status", (String) null);
    build = new BuildInformation("1234");
    build.populateFromJson(json);
    assertNull(build.getStatus());

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
    json.put("start_time", 1523657508);
    json.put("end_time", 1523657534);
    build = new BuildInformation("1234");
    build.populateFromJson(json);
    assertEquals(1523657508L, build.getStartTime());
    assertEquals(1523657534L, build.getEndTime());
    json.put("end_time", (String) null);
    build = new BuildInformation("1234");
    build.populateFromJson(json);
    assertEquals(0, build.getEndTime());
  }

  @Test
  void testBuildName() {
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
    assertEquals(Map.of("name", "Gavin's first build"), build.getChanges());
  }

  @Test
  void testGetChange() {
    assertEquals("test-name", build.getName());
    assertFalse(build.hasChanges());
    build.setName("name-name-name");
    assertTrue(build.hasChanges());
    assertEquals(Map.of("name", "name-name-name"), build.getChanges());
  }

  /* TODO - figure out how to test equals */
}
