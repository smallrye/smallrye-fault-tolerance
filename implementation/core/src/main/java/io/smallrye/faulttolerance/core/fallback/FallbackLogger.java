package io.smallrye.faulttolerance.core.fallback;

import java.lang.invoke.MethodHandles;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.MessageLogger;

@MessageLogger(projectCode = "SRFTL", length = 5)
interface FallbackLogger extends BasicLogger {
    FallbackLogger LOG = Logger.getMessageLogger(MethodHandles.lookup(), FallbackLogger.class,
            FallbackLogger.class.getPackage().getName());
}
