package com.saucelabs.ci;

import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Stores details about a sauce labs job in builds context
 * These jobs are returned by builds-api v2
 *
 * @author Sauce Labs
 */
public class BuildJobInformation implements Serializable {

    public enum JobStatusFlag {
        COMPLETED,
        ERRORED,
        FAILED,
        FINISHED,
        NEW,
        PASSED,
        PUBLIC,
        QUEUED,
        RUNNING
    };

    private String jobId;

    private long creationTime;
    private long deletionTime;
    private long modificationTime;

    final private EnumSet<JobStatusFlag> status = EnumSet.noneOf(JobStatusFlag.class);

    final private HashMap<String, Object> changes = new HashMap<>();

    /**
     *
     * @param jobId Id of job in question
     */
    public BuildJobInformation(String jobId) {
        this.jobId = jobId;
    }

    public BuildJobInformation(JSONObject o) {
        this.jobId = o.getString("id");
        populateFromJson(o);
    }

    /**
     * Id of the job in question
     *
     * @return jobid
     */
    public String getJobId() {
        return jobId;
    }

    /**
     * set creation time
     * @param creationTime creation time
     */
    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
        changes.put("creationTime", creationTime);
    }

    /**
     * get creation time
     * @return creationTime
     */
    public long getCreationTime() {
        return creationTime;
    }

    /**
     * set modification time
     * @param modificationTime modification time
     */
    public void setModificationTime(long modificationTime) {
        this.modificationTime = modificationTime;
        changes.put("modificationTime", modificationTime);
    }

    /**
     * get modification time
     * @return modificationTime
     */
    public long getModificationTime() {
        return modificationTime;
    }

    /**
     * set deletion time
     * @param deletionTime deletion time
     */
    public void setDeletionTime(long deletionTime) {
        this.deletionTime = deletionTime;
        changes.put("deletionTime", deletionTime);
    }

    /**
     * get deletion time
     * @return deletionTime
     */
    public long getDeletionTime() {
        return deletionTime;
    }

    /**
     * get all status flags of the job
     * @return status set of job status flags
     */
    public EnumSet<JobStatusFlag> getStatus() {
        return this.status.clone();
    }

    private void setStatusFlag(boolean enabled, JobStatusFlag flag) {
        if (enabled)
            this.status.add(flag);
        else
            this.status.remove(flag);
        changes.put("status", this.status);
    }

    /**
     * set status flags of the job
     * @param flag Flag to enble on status
     */
    public void setStatusFlag(JobStatusFlag flag) {
        setStatusFlag(true, flag);
    }

    /**
     * clear status flags of the job
     * @param flag Flag to clear on status
     */
    public void clearStatusFlag(JobStatusFlag flag) {
        setStatusFlag(false, flag);
    }

    /**
     * clear status flags of the job
     * @param completed Boolean to set completed status
     */
    public void setStatusCompleted(boolean completed) {
        setStatusFlag(completed, JobStatusFlag.COMPLETED);
    }

    /**
     * set/clear status ERORED flag status of the job
     * @param errored Boolean to set errored status
     */
    public void setStatusErrored(boolean errored) {
        setStatusFlag(errored, JobStatusFlag.ERRORED);
    }

    /**
     * set/clear status FAILED flag status of the job
     * @param failed Boolean to set failed status
     */
    public void setStatusFailed(boolean failed) {
        setStatusFlag(failed, JobStatusFlag.FAILED);
    }

    /**
     * set/clear status FINISHED flag status of the job
     * @param finished Boolean to set finished status
     */
    public void setStatusFinished(boolean finished) {
        setStatusFlag(finished, JobStatusFlag.FINISHED);
    }

    /**
     * set/clear status NEW flag status of the job
     * @param flagNew Boolean to set new status
     */
    public void setStatusNew(boolean flagNew) {
        setStatusFlag(flagNew, JobStatusFlag.NEW);
    }

    /**
     * set/clear status PASSED flag status of the job
     * @param passed Boolean to set passed status
     */
    public void setStatusPassed(boolean passed) {
        setStatusFlag(passed, JobStatusFlag.PASSED);
    }

    /**
     * set/clear status PUBLIC flag status of the job
     * @param flagPublic Boolean to set public status
     */
    public void setStatusPublic(boolean flagPublic) {
        setStatusFlag(flagPublic, JobStatusFlag.PUBLIC);
    }

    /**
     * set/clear status QUEUED flag status of the job
     * @param queued Boolean to set queued status
     */
    public void setStatusQueued(boolean queued) {
        setStatusFlag(queued, JobStatusFlag.QUEUED);
    }

    /**
     * set/clear status RUNNING flag status of the job
     * @param running Boolean to set running status
     */
    public void setStatusRunning(boolean running) {
        setStatusFlag(running, JobStatusFlag.RUNNING);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BuildJobInformation that = (BuildJobInformation) o;

        if (this.creationTime != that.creationTime) return false;
        if (this.modificationTime != that.modificationTime) return false;
        if (this.deletionTime != that.deletionTime) return false;
        if (this.status != that.status) return false;

        return changes != null ? changes.equals(that.changes) : that.changes == null;
    }

    @Override
    public int hashCode() {
        return jobId != null ? jobId.hashCode() : 0;
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
        setCreationTime(buildData.getLong("creation_time"));
        setModificationTime(buildData.getLong("modification_time"));
        if (buildData.has("deletion_time") && !buildData.isNull("deletion_time")) {
            setDeletionTime(buildData.getLong("deletion_time"));
        }

        JSONObject buildJobs = buildData.getJSONObject("state");
        setStatusCompleted(buildJobs.getBoolean("completed"));
        setStatusErrored(buildJobs.getBoolean("errored"));
        setStatusFailed(buildJobs.getBoolean("failed"));
        setStatusFinished(buildJobs.getBoolean("finished"));
        setStatusNew(buildJobs.getBoolean("new"));
        setStatusPassed(buildJobs.getBoolean("passed"));
        setStatusPublic(buildJobs.getBoolean("public"));
        setStatusQueued(buildJobs.getBoolean("queued"));
        setStatusRunning(buildJobs.getBoolean("running"));

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
