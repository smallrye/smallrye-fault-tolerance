package io.smallrye.faulttolerance.core.async;

import java.lang.invoke.MethodHandles;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.MessageLogger;

@MessageLogger(projectCode = "SRFTL", length = 5)
interface AsyncLogger extends BasicLogger {
    AsyncLogger LOG = Logger.getMessageLogger(MethodHandles.lookup(), AsyncLogger.class,
            AsyncLogger.class.getPackage().getName());
}
