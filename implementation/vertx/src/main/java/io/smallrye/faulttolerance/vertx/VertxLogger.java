package io.smallrye.faulttolerance.vertx;

import static org.jboss.logging.annotations.Message.NONE;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.Transform;

import io.smallrye.faulttolerance.core.scheduler.SchedulerTask;

@MessageLogger(projectCode = "SRFTL", length = 5)
interface VertxLogger extends BasicLogger {
    VertxLogger LOG = Logger.getMessageLogger(VertxLogger.class, VertxLogger.class.getPackage().getName());

    @Message(id = NONE, value = "Scheduled event loop task %s to run in %s millis")
    @LogMessage(level = Logger.Level.TRACE)
    void scheduledEventLoopTask(@Transform(Transform.TransformType.IDENTITY_HASH_CODE) SchedulerTask task, long millis);

    @Message(id = NONE, value = "Running event loop task %s")
    @LogMessage(level = Logger.Level.TRACE)
    void runningEventLoopTask(@Transform(Transform.TransformType.IDENTITY_HASH_CODE) SchedulerTask task);

    @Message(id = NONE, value = "Cancelled event loop task %s")
    @LogMessage(level = Logger.Level.TRACE)
    void cancelledEventLoopTask(@Transform(Transform.TransformType.IDENTITY_HASH_CODE) SchedulerTask task);
}
