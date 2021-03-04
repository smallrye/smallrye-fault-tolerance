package io.smallrye.faulttolerance.core.scheduler;

import static org.jboss.logging.annotations.Message.NONE;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.Transform;

@MessageLogger(projectCode = "SRFTL", length = 5)
interface SchedulerLogger extends BasicLogger {
    SchedulerLogger LOG = Logger.getMessageLogger(SchedulerLogger.class, SchedulerLogger.class.getPackage().getName());

    @Message(id = NONE, value = "Timer %s created")
    @LogMessage(level = Logger.Level.TRACE)
    void createdTimer(String name);

    @Message(id = NONE, value = "Timer %s shut down")
    @LogMessage(level = Logger.Level.TRACE)
    void shutdownTimer(String name);

    @Message(id = NONE, value = "Scheduled timer task %s to run in %s millis")
    @LogMessage(level = Logger.Level.TRACE)
    void scheduledTimerTask(@Transform(Transform.TransformType.IDENTITY_HASH_CODE) TimerTask task, long millis);

    @Message(id = NONE, value = "Running timer task %s")
    @LogMessage(level = Logger.Level.TRACE)
    void runningTimerTask(@Transform(Transform.TransformType.IDENTITY_HASH_CODE) TimerTask task);

    @Message(id = NONE, value = "Cancelled timer task %s")
    @LogMessage(level = Logger.Level.TRACE)
    void cancelledTimerTask(@Transform(Transform.TransformType.IDENTITY_HASH_CODE) TimerTask task);

    @Message(id = 11000, value = "Unexpected exception in timer loop, ignoring")
    @LogMessage(level = Logger.Level.WARN)
    void unexpectedExceptionInTimerLoop(@Cause Exception e);

    @Message(id = NONE, value = "Found event loop integration %s")
    @LogMessage(level = Logger.Level.TRACE)
    void foundEventLoop(@Transform(Transform.TransformType.GET_CLASS) EventLoop eventLoop);

    @Message(id = NONE, value = "No event loop integration found")
    @LogMessage(level = Logger.Level.TRACE)
    void noEventLoop();
}
