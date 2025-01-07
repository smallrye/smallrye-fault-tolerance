package io.smallrye.faulttolerance.core.retry;

import java.lang.invoke.MethodHandles;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.MessageLogger;

@MessageLogger(projectCode = "SRFTL", length = 5)
interface RetryLogger extends BasicLogger {
    RetryLogger LOG = Logger.getMessageLogger(MethodHandles.lookup(), RetryLogger.class,
            RetryLogger.class.getPackage().getName());
}
