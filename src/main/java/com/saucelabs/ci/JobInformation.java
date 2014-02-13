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

    private transient boolean hasBuildNumber = true;
    private transient boolean hasJobName = false;

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
}
