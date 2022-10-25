package io.smallrye.faulttolerance.core.bulkhead;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.MessageLogger;

@MessageLogger(projectCode = "SRFTL", length = 5)
interface BulkheadLogger extends BasicLogger {
    BulkheadLogger LOG = Logger.getMessageLogger(BulkheadLogger.class, BulkheadLogger.class.getPackage().getName());

    default void debugOrTrace(String debugMessage, String traceAmendment) {
        if (isTraceEnabled()) {
            debugf("%s: %s", traceAmendment, debugMessage);
        } else {
            debug(debugMessage);
        }
    }
}
