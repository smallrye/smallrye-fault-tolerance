package io.smallrye.faulttolerance.core.timeout;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.MessageLogger;

@MessageLogger(projectCode = "SRFTL", length = 5)
public interface TimeoutLogger extends BasicLogger {
    TimeoutLogger LOG = Logger.getMessageLogger(TimeoutLogger.class, TimeoutLogger.class.getPackage().getName());
}
