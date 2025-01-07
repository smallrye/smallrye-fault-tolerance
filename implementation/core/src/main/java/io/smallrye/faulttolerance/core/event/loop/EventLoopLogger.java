package io.smallrye.faulttolerance.core.event.loop;

import static org.jboss.logging.annotations.Message.NONE;

import java.lang.invoke.MethodHandles;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.Transform;

@MessageLogger(projectCode = "SRFTL", length = 5)
interface EventLoopLogger extends BasicLogger {
    EventLoopLogger LOG = Logger.getMessageLogger(MethodHandles.lookup(), EventLoopLogger.class,
            EventLoopLogger.class.getPackage().getName());

    @Message(id = NONE, value = "Found event loop integration %s")
    @LogMessage(level = Logger.Level.DEBUG)
    void foundEventLoop(@Transform(Transform.TransformType.GET_CLASS) EventLoop eventLoop);

    @Message(id = NONE, value = "No event loop integration found")
    @LogMessage(level = Logger.Level.DEBUG)
    void noEventLoop();
}
