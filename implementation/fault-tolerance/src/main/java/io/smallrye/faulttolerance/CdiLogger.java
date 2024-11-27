package io.smallrye.faulttolerance;

import java.util.Set;

import jakarta.enterprise.inject.spi.DefinitionException;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

import io.smallrye.faulttolerance.autoconfig.MethodDescriptor;

@MessageLogger(projectCode = "SRFTL", length = 5)
interface CdiLogger extends BasicLogger {
    CdiLogger LOG = Logger.getMessageLogger(CdiLogger.class, CdiLogger.class.getPackage().getName());

    @Message(id = 1, value = "MicroProfile: Fault Tolerance activated (SmallRye Fault Tolerance version: %s)")
    @LogMessage
    void activated(String version);

    @Message(id = 2, value = "Multiple circuit breakers have the same name '%s': %s")
    DefinitionException multipleCircuitBreakersWithTheSameName(String name, Set<String> useSites);

    @Message(id = 3, value = "Backoff annotation @%s present on '%s', but @Retry is missing")
    DefinitionException backoffAnnotationWithoutRetry(String backoffAnnotation, MethodDescriptor method);

    DefinitionException backoffAnnotationWithoutRetry(String backoffAnnotation, Class<?> clazz);

    @Message(id = 4, value = "Both @Blocking and @NonBlocking present on '%s'")
    DefinitionException bothBlockingNonBlockingPresent(MethodDescriptor method);

    DefinitionException bothBlockingNonBlockingPresent(Class<?> clazz);

    @Message(id = 5, value = "Both @Asynchronous and @AsynchronousNonBlocking present on '%s'")
    DefinitionException bothAsyncAndAsyncNonBlockingPresent(MethodDescriptor method);

    DefinitionException bothAsyncAndAsyncNonBlockingPresent(Class<?> clazz);

    @Message(id = 6, value = "@RetryWhen present on '%s', but @Retry is missing")
    DefinitionException retryWhenAnnotationWithoutRetry(MethodDescriptor method);

    DefinitionException retryWhenAnnotationWithoutRetry(Class<?> clazz);

    @Message(id = 7, value = "@BeforeRetry present on '%s', but @Retry is missing")
    DefinitionException beforeRetryAnnotationWithoutRetry(MethodDescriptor method);

    DefinitionException beforeRetryAnnotationWithoutRetry(Class<?> clazz);

    @Message(id = 8, value = "Multiple Guard/TypedGuard beans have the same identifier '%s': %s")
    DefinitionException multipleGuardsWithTheSameIdentifier(String identifier, Set<String> beans);

    @Message(id = 9, value = "Guard/TypedGuard with identifier '%s' expected, but does not exist")
    DefinitionException expectedGuardDoesNotExist(String identifier);

    @Message(id = 10, value = "Guard/TypedGuard with identifier 'global' is not allowed: %s")
    DefinitionException guardWithIdentifierGlobal(String bean);
}
