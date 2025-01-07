package io.smallrye.faulttolerance.core.timeout;

import static org.jboss.logging.annotations.Message.NONE;

import java.lang.invoke.MethodHandles;

import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.Transform;

@MessageLogger(projectCode = "SRFTL", length = 5)
interface TimeoutLogger extends BasicLogger {
    TimeoutLogger LOG = Logger.getMessageLogger(MethodHandles.lookup(), TimeoutLogger.class,
            TimeoutLogger.class.getPackage().getName());

    @Message(id = NONE, value = "AsyncTimeoutTask %s created")
    @LogMessage(level = Logger.Level.TRACE)
    void asyncTimeoutTaskCreated(@Transform(Transform.TransformType.IDENTITY_HASH_CODE) Object task);

    @Message(id = NONE, value = "AsyncTimeoutTask %s completing with %s")
    @LogMessage(level = Logger.Level.TRACE)
    void asyncTimeoutTaskCompleting(@Transform(Transform.TransformType.IDENTITY_HASH_CODE) Object task, TimeoutException e);

    @Message(id = NONE, value = "AsyncTimeout rethrowing %s")
    @LogMessage(level = Logger.Level.TRACE)
    void asyncTimeoutRethrowing(Throwable e);
}
