package com.saucelabs.ci;

import java.sql.Timestamp;
import java.util.Date;

/**
 * Contains several utility methods to perform current time calculations.
 *
 * @author <a href="http://www.sysbliss.com">Jonathan Doklovic</a>
 * @author Ross Rowe
 */
public final class CacheTimeUtil {
    /**
     * Class can't be constructed.
     */
    private CacheTimeUtil() {
    }

    private static Timestamp getMaxTimestampForDuration(Timestamp startTime, long duration) {
        return new Timestamp(startTime.getTime() + duration);
    }

    public static Timestamp getCurrentTimestamp() {
        Date now = new Date();
        return new Timestamp(now.getTime());
    }

    public static boolean pastAcceptableDuration(Timestamp startTime, long duration) {

        // Get the maximum end time
        Timestamp maxTimestamp = getMaxTimestampForDuration(startTime, duration);

        // Get the time elapsed since creation
        Timestamp timeElapsed = getCurrentTimestamp();

        // If we're after the max we return true
        return timeElapsed.after(maxTimestamp);
    }
}
