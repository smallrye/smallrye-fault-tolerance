package io.smallrye.faulttolerance.core.async.types;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.MessageLogger;

@MessageLogger(projectCode = "SRFTL", length = 5)
interface AsyncTypesLogger extends BasicLogger {
    AsyncTypesLogger LOG = Logger.getMessageLogger(AsyncTypesLogger.class, AsyncTypesLogger.class.getPackage().getName());
}
