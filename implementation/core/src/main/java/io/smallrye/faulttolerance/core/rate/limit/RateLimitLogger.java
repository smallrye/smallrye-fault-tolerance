package io.smallrye.faulttolerance.core.rate.limit;

import java.lang.invoke.MethodHandles;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.MessageLogger;

@MessageLogger(projectCode = "SRFTL", length = 5)
interface RateLimitLogger extends BasicLogger {
    RateLimitLogger LOG = Logger.getMessageLogger(MethodHandles.lookup(), RateLimitLogger.class,
            RateLimitLogger.class.getPackage().getName());
}
