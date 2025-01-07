package io.smallrye.faulttolerance.core.metrics;

import java.lang.invoke.MethodHandles;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.MessageLogger;

@MessageLogger(projectCode = "SRFTL", length = 5)
interface MetricsLogger extends BasicLogger {
    MetricsLogger LOG = Logger.getMessageLogger(MethodHandles.lookup(), MetricsLogger.class,
            MetricsLogger.class.getPackage().getName());
}
