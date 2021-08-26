package io.smallrye.faulttolerance.core.before.retry;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.MessageLogger;

@MessageLogger(projectCode = "SRFTL", length = 5)
interface BeforeRetryLogger extends BasicLogger {
    BeforeRetryLogger LOG = Logger.getMessageLogger(BeforeRetryLogger.class, BeforeRetryLogger.class.getPackage().getName());
}