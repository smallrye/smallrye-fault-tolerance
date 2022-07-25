package io.smallrye.faulttolerance.core.circuit.breaker;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.MessageLogger;

@MessageLogger(projectCode = "SRFTL", length = 5)
interface CircuitBreakerLogger extends BasicLogger {
    CircuitBreakerLogger LOG = Logger.getMessageLogger(CircuitBreakerLogger.class,
            CircuitBreakerLogger.class.getPackage().getName());

    default void debugOrTrace(String debugMessage, String traceAmendment) {
        if (isTraceEnabled()) {
            debugf("%s: %s", traceAmendment, debugMessage);
        } else {
            debug(debugMessage);
        }
    }
}
