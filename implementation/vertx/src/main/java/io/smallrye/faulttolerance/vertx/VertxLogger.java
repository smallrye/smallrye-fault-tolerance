package io.smallrye.faulttolerance.vertx;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.MessageLogger;

@MessageLogger(projectCode = "SRFTL", length = 5)
interface VertxLogger extends BasicLogger {
    VertxLogger LOG = Logger.getMessageLogger(VertxLogger.class, VertxLogger.class.getPackage().getName());
}
