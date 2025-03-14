package com.saucelabs.ci.sauceconnect;

import org.slf4j.Logger;

public interface SCMonitorFactory {
    SCMonitor create(int port, Logger logger);
}
