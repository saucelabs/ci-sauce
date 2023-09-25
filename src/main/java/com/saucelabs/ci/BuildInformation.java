package com.saucelabs.ci;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Stores details about a sauce labs build/test
 * Does not contain everything
 *
 * @author Sauce Labs
 */
public class BuildInformation implements Serializable {

    private String buildId;

    private String status;
    private String name;

    private long startTime;
    private long endTime;

    private int jobsPassed;
    private int jobsFinished;
    private int jobsFailed;
    private int jobsErrored;

    final private HashMap<String, Object> changes = new HashMap<>();

    /**
     *
     * @param buildId BuildID of the build/test in question
     */
    public BuildInformation(String buildId) {
        this.buildId = buildId;
    }

    /**
     * Build id of the build/test in question
     *
     * @return buildid
     */
    public String getBuildId() {
        return buildId;
    }

    /**
     * Build Status
     *
     * @return "success", "failed", "error", null
     */
    @Nullable
    public String getStatus() {
        return status;
    }

    /**
     * Set build status state (string)
     * @param status null, "success", "failed", "error", null
     */
    public void setStatus(@Nullable String status) {
        this.status = status;
        changes.put("status", status);
    }

    /**
     * Is a build name set? Check for all possible false values
     *
     * @return true/false
     */
    public boolean hasBuildName() {
        return name != null && !name.equals("") && !name.equals("null");
    }

    /**
     * set start time
     * @param startTime start time
     */
    public void setStartTime(long startTime) {
        this.startTime = startTime;
        changes.put("startTime", startTime);
    }

    /**
     * get start time
     * @return startTime
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * set end time
     * @param endTime end time
     */
    public void setEndTime(long endTime) {
        this.endTime = endTime;
        changes.put("endTime", endTime);
    }

    /**
     * get end time
     * @return endTime
     */
    public long getEndTime() {
        return endTime;
    }

    /**
     * Get Build Name
     * @return name of build
     */
    @Nullable
    public String getName() {
        return name;
    }

    /**
     * Set build name
     *
     * @param name New Name
     */
    public void setName(@Nullable String name) {
        this.name = name;
        changes.put("name", name);
    }

    /**
     * get jobsFinished
     * @return jobsFinished
     */
    public int getJobsFinished() {
        return jobsFinished;
    }

    /**
     * set jobsFinished
     * @param jobsFinished jobs Finished
     */
    public void setJobsFinished(int jobsFinished) {
        this.jobsFinished = jobsFinished;
        changes.put("jobsFinished", jobsFinished);
    }

    /**
     * get jobsPassed
     * @return jobsPassed
     */
    public int getJobsPassed() {
        return jobsPassed;
    }

    /**
     * set jobsPassed
     * @param jobsPassed jobs Finished
     */
    public void setJobsPassed(int jobsPassed) {
        this.jobsPassed = jobsPassed;
        changes.put("jobsPassed", jobsPassed);
    }

    /**
     * get jobsFailed
     * @return jobsFailed
     */
    public int getJobsFailed() {
        return jobsFailed;
    }

    /**
     * set jobsFailed
     * @param jobsFailed jobs Failed
     */
    public void setJobsFailed(int jobsFailed) {
        this.jobsFailed = jobsFailed;
        changes.put("jobsFailed", jobsFailed);
    }

    /**
     * get jobsErrored
     * @return jobsErrored
     */
    public int getJobsErrored() {
        return jobsErrored;
    }

    /**
     * set jobsErrored
     * @param jobsErrored jobs Errored
     */
    public void setJobsErrored(int jobsErrored) {
        this.jobsErrored = jobsErrored;
        changes.put("jobsErrored", jobsErrored);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BuildInformation that = (BuildInformation) o;

        if (endTime != that.endTime) return false;
        if (jobsFinished != that.jobsFinished) return false;
        if (jobsPassed != that.jobsPassed) return false;
        if (jobsFailed != that.jobsFailed) return false;
        if (jobsErrored != that.jobsErrored) return false;

        if (buildId != null ? !buildId.equals(that.buildId) : that.buildId != null) return false;
        if (status != null ? !status.equals(that.status) : that.status != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (startTime != that.startTime) return false;

        return changes != null ? changes.equals(that.changes) : that.changes == null;
    }

    @Override
    public int hashCode() {
        return buildId != null ? buildId.hashCode() : 0;
    }

    /**
     * Takes in a JSONObject, and populates the current object with all values
     * Also resets the list of changes
     *
     * @see BuildInformation#clearChanges()
     *
     * @param buildData JSON Data of build/test data
     * @throws JSONException Any processing error of the JSON Object
     */
    public void populateFromJson(JSONObject buildData) throws JSONException {
        if (buildData.has("status") && !buildData.isNull("status")) {
            setStatus(buildData.getString("status"));
        }
        if (buildData.has("name") && !buildData.isNull("name")) {
            setName(buildData.getString("name"));
        }
        if (buildData.has("end_time") && !buildData.isNull("end_time")) {
            setEndTime(buildData.getLong("end_time"));
        }

        setStartTime(buildData.getLong("start_time"));
        JSONObject buildJobs = buildData.getJSONObject("jobs");
        setJobsFinished(buildJobs.getInt("finished"));
        setJobsPassed(buildJobs.getInt("passed"));
        setJobsFailed(buildJobs.getInt("failed"));
        setJobsErrored(buildJobs.getInt("errored"));

        clearChanges();
    }

    public boolean hasChange(String field) {
        return changes.containsKey(field);
    }

    public boolean hasChanges() {
        return !changes.isEmpty();
    }

    /**
     * Resets the internal dirty data state
     */
    public void clearChanges() {
        changes.clear();
    }

    /**
     * Get a map of all the internal changes/dirty data
     *
     * @return map of all the changes
     */
    public Map<String, Object> getChanges() {
        return new HashMap<>(changes);
    }
}
