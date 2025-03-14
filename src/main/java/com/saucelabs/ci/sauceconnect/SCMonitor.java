package com.saucelabs.ci.sauceconnect;

import java.util.concurrent.Semaphore;

public interface SCMonitor extends Runnable {
    void setSemaphore(Semaphore semaphore);
    String getTunnelId();
    Exception getLastHealtcheckException();
    void markAsFailed();
    boolean isFailed();
}
