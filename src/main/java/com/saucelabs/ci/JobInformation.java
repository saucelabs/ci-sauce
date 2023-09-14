package com.saucelabs.ci;

import com.saucelabs.saucerest.model.jobs.Job;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Stores details about a sauce labs job/test
 *
 * @author Sauce Labs
 */
public class JobInformation implements Serializable {

    private String jobId;

    private String hmac;
    private String status;
    private String name;

    private String os;
    private String browser;
    private String version;

    private String videoUrl;
    private String logUrl;
    private String build;

    private long startTime;
    private long endTime;

    private String failureMessage;

    final private HashMap<String, Object> changes = new HashMap<>();

    /**
     *
     * @param jobId JobID of the job/test in question
     * @param hmac Temporary Authentication token
     */
    public JobInformation(String jobId, String hmac) {
        this.jobId = jobId;
        this.hmac = hmac;
    }

    /**
     * Gets Temporary Authentication token
     *
     * @return hmac token provided in the constructor
     */
    public String getHmac() {
        return hmac;
    }

    /**
     * Sets Temporary Authentication token
     *
     * @param hmac hmac token
     */
    public void setHmac(String hmac) {
        this.hmac = hmac;
    }

    /**
     * Job id of the job/test in question
     *
     * @return jobid
     */
    public String getJobId() {
        return jobId;
    }

    /**
     * Job Status
     *
     * @return "Passed", "Failed", null
     */
    @Nullable
    public String getStatus() {
        return status;
    }

    /**
     * Set job status state (boolean)
     * @param status Boolean of true (passed) or false (failed)
     */
    public void setStatus(@Nonnull Boolean status) {
        this.setStatus(status.booleanValue() ? "Passed" : "Failed");
    }

    /**
     * Set job status state (string)
     * @param status null, "passed", "false"
     */
    public void setStatus(@Nullable String status) {
        this.status = status;
        changes.put("status", status);
    }

    /**
     * Is a job name set? Check for all possible false values
     *
     * @return true/false
     */
    public boolean hasJobName() {
        return name != null && !name.equals("") && !name.equals("null");
    }

    /**
     * Get Job Name
     * @return name of job
     */
    @Nullable
    public String getName() {
        return name;
    }

    /**
     * Set job name
     *
     * @param name New Name
     */
    public void setName(@Nullable String name) {
        this.name = name;
        changes.put("name", name);
    }

    /**
     * Is a build name set? Check for all possible false values
     *
     * @return true/false
     */
    public boolean hasBuild() {
        return build != null && !build.equals("") && !build.equals("null");
    }

    /**
     * Get build name
     *
     * @return build name
     */
    @Nullable
    public String getBuild() {
        return build;
    }

    /**
     * Set build name
     *
     * @param build build name
     */
    public void setBuild(@Nullable String build) {
        this.build = build;
        changes.put("build", build);
    }

    /**
     * get browser
     * @return browser
     */
    public String getBrowser() {
        return browser;
    }

    /**
     * set browser
     * @param browser browser name
     */
    public void setBrowser(String browser) {
        this.browser = browser;
        changes.put("browser", browser);

    }

    /**
     * get os (normalized)
     * @return normalized os
     */
    public String getOs() {
        return OperatingSystemDescription.getOperatingSystemName(os);
    }

    /**
     * set os
     * @param os os name
     */
    public void setOs(String os) {
        this.os = os;
        changes.put("os", os);
    }

    /**
     * get version
     * @return version
     */
    public String getVersion() {
        return version;
    }

    /**
     * get version
     * @param version version
     */
    public void setVersion(String version) {
        this.version = version;
        changes.put("version", version);
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
     * get duration
     * @return endTime - startTime
     */
    public long getDuration() {
        return endTime - startTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JobInformation that = (JobInformation) o;

        if (jobId != null ? !jobId.equals(that.jobId) : that.jobId != null) return false;
        if (hmac != null ? !hmac.equals(that.hmac) : that.hmac != null) return false;
        if (status != null ? !status.equals(that.status) : that.status != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (os != null ? !os.equals(that.os) : that.os != null) return false;
        if (browser != null ? !browser.equals(that.browser) : that.browser != null) return false;
        if (version != null ? !version.equals(that.version) : that.version != null) return false;
        if (videoUrl != null ? !videoUrl.equals(that.videoUrl) : that.videoUrl != null) return false;
        if (logUrl != null ? !logUrl.equals(that.logUrl) : that.logUrl != null) return false;
        if (build != null ? !build.equals(that.build) : that.build != null) return false;
        if (failureMessage != null ? !failureMessage.equals(that.failureMessage) : that.failureMessage != null) return false;
        if (startTime != that.startTime) return false;
        if (endTime != that.endTime) return false;
        return changes != null ? changes.equals(that.changes) : that.changes == null;

    }

    @Override
    public int hashCode() {
        return jobId != null ? jobId.hashCode() : 0;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
        changes.put("videoUrl", videoUrl);

    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setLogUrl(String logUrl) {
        this.logUrl = logUrl;
        changes.put("logUrl", logUrl);
    }

    public String getLogUrl() {
        return logUrl;
    }

    public void setFailureMessage(String failureMessage) {
        this.failureMessage = failureMessage;
        changes.put("failureMessage", failureMessage);
    }

    @Nullable
    public String getFailureMessage() {
        return failureMessage;
    }

    public boolean hasFailureMessage() {
        return failureMessage != null && !failureMessage.equals("") && !failureMessage.equals("null");
    }

    /**
     * Takes in a SauceREST Job, and populates the current object with all values
     * Also resets the list of changes
     *
     * @see JobInformation#clearChanges()
     *
     * @param job SauceREST Job
     */
    public void populate(Job job) {
        if (job.passed != null) {
            setStatus(job.passed);
        }
        if (job.name != null) {
            setName(job.name);
        }
        if (job.build != null) {
            setBuild(job.build);
        }
        if (job.customData != null) {
            if (job.customData.containsKey("FAILURE_MESSAGE")) {
                setFailureMessage(job.customData.get("FAILURE_MESSAGE"));
            }
        }
        if (job.endTime != null) {
            setEndTime(job.endTime);
        }
        if (job.startTime != null) {
            setStartTime(job.startTime);
        }
        if (job.os != null) {
            setOs(job.os);
        }
        if (job.browser != null) {
            setBrowser(job.browser);
        }
        if (job.browserShortVersion != null) {
            setVersion(job.browserShortVersion);
        }
        if (job.videoUrl != null) {
            setVideoUrl(job.videoUrl);
        }
        if (job.logUrl != null) {
            setLogUrl(job.logUrl);
        }

        clearChanges();
    }

    /**
     * Takes in a JSONObject, and populates the current object with all values
     * Also resets the list of changes
     *
     * @see JobInformation#clearChanges()
     *
     * @param jobData JSON Data of job/test data
     * @throws JSONException Any processing error of the JSON Object
     */
    public void populateFromJson(JSONObject jobData) throws JSONException {

        if (jobData.has("passed") && !jobData.isNull("passed")) {
            setStatus(jobData.getBoolean("passed"));
        }
        if (jobData.has("name") && !jobData.isNull("name")) {
            setName(jobData.getString("name"));
        }
        if (jobData.has("build") && !jobData.isNull("build")) {
            setBuild(jobData.getString("build"));
        }
        if (jobData.has("custom-data")&& !jobData.isNull("custom-data")) {
            JSONObject customData = jobData.getJSONObject("custom-data");
            if (customData.has("FAILURE_MESSAGE")) {
                String failureMessage = customData.getString("FAILURE_MESSAGE");
                setFailureMessage(failureMessage);
            }
        }
        if (jobData.has("end_time") && !jobData.isNull("end_time")) {
            setEndTime(jobData.getLong("end_time"));
        }
        setStartTime(jobData.getLong("start_time"));
        setOs(jobData.getString("os"));
        setBrowser(jobData.getString("browser"));
        setVersion(jobData.getString("browser_short_version"));
        setVideoUrl(jobData.getString("video_url"));
        setLogUrl(jobData.getString("log_url"));
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
