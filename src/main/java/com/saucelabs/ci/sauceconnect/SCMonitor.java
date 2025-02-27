package com.saucelabs.ci.sauceconnect;

import java.util.concurrent.Semaphore;

public interface SCMonitor extends Runnable {
    void setSemaphore(Semaphore semaphore);
    String getTunnelId();
    boolean isFailed();
}
