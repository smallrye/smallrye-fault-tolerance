package io.smallrye.faulttolerance;

import java.util.Set;

import javax.enterprise.inject.spi.DefinitionException;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

@MessageLogger(projectCode = "SRFTL", length = 5)
interface CdiLogger extends BasicLogger {
    CdiLogger LOG = Logger.getMessageLogger(CdiLogger.class, CdiLogger.class.getPackage().getName());

    @Message(id = 1, value = "MicroProfile: Fault Tolerance activated (SmallRye Fault Tolerance version: %s)")
    @LogMessage
    void activated(String version);

    @Message(id = 2, value = "Multiple circuit breakers have the same name '%s': %s")
    DefinitionException multipleCircuitBreakersWithTheSameName(String name, Set<String> useSites);
}
