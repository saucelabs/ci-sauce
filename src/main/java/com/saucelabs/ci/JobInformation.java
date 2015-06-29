package com.saucelabs.ci;


import java.io.Serializable;

/**
 * Simple class used to represent Sauce Job Id and corresponding HMAC token that is used
 * for the display of embedded job results.
 *
 * @author Ross Rowe
 */
public class JobInformation implements Serializable {

    private String jobId;

    private String hmac;
    private String status;
    private String name;

    private String os;
    private String browser;
    private String version;


    private transient boolean hasBuildNumber = true;
    private transient boolean hasJobName = false;
    private String videoUrl;
    private String logUrl;

    public JobInformation(String jobId, String hmac) {
        this.jobId = jobId;
        this.hmac = hmac;
    }

    public String getHmac() {
        return hmac;
    }

    public void setHmac(String hmac) {
        this.hmac = hmac;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isHasBuildNumber() {
        return hasBuildNumber;
    }

    public void setHasBuildNumber(boolean hasBuildNumber) {
        this.hasBuildNumber = hasBuildNumber;
    }

    public boolean isHasJobName() {
        return hasJobName;
    }

    public void setHasJobName(boolean hasJobName) {
        this.hasJobName = hasJobName;
    }

    public String getBrowser() {
        return browser;
    }

    public void setBrowser(String browser) {
        this.browser = browser;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JobInformation)) return false;

        JobInformation that = (JobInformation) o;

        if (jobId != null ? !jobId.equals(that.jobId) : that.jobId != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return jobId != null ? jobId.hashCode() : 0;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setLogUrl(String logUrl) {
        this.logUrl = logUrl;
    }

    public String getLogUrl() {
        return logUrl;
    }
}
