package io.smallrye.faulttolerance.core.metrics;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.MessageLogger;

@MessageLogger(projectCode = "SRFTL", length = 5)
public interface MetricsLogger extends BasicLogger {
    MetricsLogger LOG = Logger.getMessageLogger(MetricsLogger.class, MetricsLogger.class.getPackage().getName());
}
