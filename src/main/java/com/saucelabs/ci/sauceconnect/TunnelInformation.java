package com.saucelabs.ci.sauceconnect;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by halkeye on 7/29/16.
 */
class TunnelInformation {
    private final String identifier;
    private Process process;
    private Integer processCount = 0;
    private final Lock lock = new ReentrantLock();
    private String tunnelId;

    public TunnelInformation(String identifier) {
        this.identifier = identifier;
    }

    public Lock getLock() {
        return lock;
    }

    public Process getProcess() {
        return process;
    }

    public void setProcess(Process process) {
        this.process = process;
    }

    public Integer getProcessCount() {
        return processCount;
    }

    public void setProcessCount(Integer processCount) {
        this.processCount = processCount;
    }

    public String getTunnelId() {
        return tunnelId;
    }

    public void setTunnelId(String tunnelId) {
        this.tunnelId = tunnelId;
    }

    @Override
    public String toString() {
        return identifier;
    }
}
