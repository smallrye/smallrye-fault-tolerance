package io.smallrye.faulttolerance.internal;

import java.lang.invoke.MethodHandles;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.MessageLogger;

@MessageLogger(projectCode = "SRFTL", length = 5)
interface InternalLogger extends BasicLogger {
    InternalLogger LOG = Logger.getMessageLogger(MethodHandles.lookup(), InternalLogger.class,
            InternalLogger.class.getPackage().getName());
}
