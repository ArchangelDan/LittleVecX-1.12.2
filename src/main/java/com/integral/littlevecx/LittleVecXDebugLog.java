package com.integral.littlevecx;

import org.apache.logging.log4j.Logger;

/** Central opt-in channel for verbose diagnostics that must stay silent in normal play. */
public final class LittleVecXDebugLog {

    private LittleVecXDebugLog() {}

    public static void debug(Logger logger, String message, Object... parameters) {
        if (LittleVecXConfig.enableVerboseLogging)
            logger.info(message, parameters);
    }
}
