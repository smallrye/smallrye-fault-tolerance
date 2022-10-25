package io.smallrye.faulttolerance.core.rate.limit;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.MessageLogger;

@MessageLogger(projectCode = "SRFTL", length = 5)
interface RateLimitLogger extends BasicLogger {
    RateLimitLogger LOG = Logger.getMessageLogger(RateLimitLogger.class,
            RateLimitLogger.class.getPackage().getName());
}
