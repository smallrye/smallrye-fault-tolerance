package io.smallrye.faulttolerance.core.timeout;

import java.util.function.Supplier;

import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;

import io.smallrye.faulttolerance.core.InvocationContextEvent;

public class TimeoutEvents {
    public enum Started implements InvocationContextEvent {
        INSTANCE
    }

    public enum Finished implements InvocationContextEvent {
        NORMALLY(false),
        TIMED_OUT(true),
        ;

        public final boolean timedOut;

        Finished(boolean timedOut) {
            this.timedOut = timedOut;
        }
    }

    public static class AsyncTimedOut implements InvocationContextEvent {
        private final Supplier<TimeoutException> timeoutException;

        AsyncTimedOut(Supplier<TimeoutException> timeoutException) {
            this.timeoutException = timeoutException;
        }

        TimeoutException timeoutException() {
            return timeoutException.get();
        }
    }
}
