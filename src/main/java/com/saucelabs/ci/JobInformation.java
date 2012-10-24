package com.saucelabs.ci;


/**
 * Simple class used to represent Sauce Job Id and corresponding HMAC token that is used
 * for the display of embedded job results.
 *
 * @author Ross Rowe
 */
public class JobInformation {

    private String jobId;

    private String hmac;
    private String status;
    private String name;

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
}
