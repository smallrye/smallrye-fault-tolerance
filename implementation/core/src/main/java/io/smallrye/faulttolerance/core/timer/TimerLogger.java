package io.smallrye.faulttolerance.core.timer;

import static org.jboss.logging.annotations.Message.NONE;

import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.Transform;

@MessageLogger(projectCode = "SRFTL", length = 5)
public interface TimerLogger {
    TimerLogger LOG = Logger.getMessageLogger(TimerLogger.class, TimerLogger.class.getPackage().getName());

    @Message(id = NONE, value = "Timer %s created")
    @LogMessage(level = Logger.Level.TRACE)
    void created(String name);

    @Message(id = NONE, value = "Timer %s shut down")
    @LogMessage(level = Logger.Level.TRACE)
    void shutdown(String name);

    @Message(id = NONE, value = "Scheduled task %s to run in %s millis")
    @LogMessage(level = Logger.Level.TRACE)
    void scheduledTask(@Transform(Transform.TransformType.IDENTITY_HASH_CODE) TimerTask task, long millis);

    @Message(id = NONE, value = "Running task %s")
    @LogMessage(level = Logger.Level.TRACE)
    void runningTask(@Transform(Transform.TransformType.IDENTITY_HASH_CODE) TimerTask task);

    @Message(id = NONE, value = "Cancelled task %s")
    @LogMessage(level = Logger.Level.TRACE)
    void cancelledTask(@Transform(Transform.TransformType.IDENTITY_HASH_CODE) TimerTask task);

    @Message(id = 11000, value = "Unexpected exception in timer loop, ignoring")
    @LogMessage(level = Logger.Level.WARN)
    void unexpectedExceptionInTimerLoop(@Cause Exception e);
}
