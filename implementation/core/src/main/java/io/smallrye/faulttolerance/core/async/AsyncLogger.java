package io.smallrye.faulttolerance.core.async;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.MessageLogger;

@MessageLogger(projectCode = "SRFTL", length = 5)
interface AsyncLogger extends BasicLogger {
    AsyncLogger LOG = Logger.getMessageLogger(AsyncLogger.class, AsyncLogger.class.getPackage().getName());
}
