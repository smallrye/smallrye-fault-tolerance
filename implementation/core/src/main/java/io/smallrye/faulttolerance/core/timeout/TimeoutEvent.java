package io.smallrye.faulttolerance.core.timeout;

import java.util.function.Supplier;

import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;

import io.smallrye.faulttolerance.core.InvocationContextEvent;

class TimeoutEvent implements InvocationContextEvent {
    private final Supplier<TimeoutException> timeoutException;

    TimeoutEvent(Supplier<TimeoutException> timeoutException) {
        this.timeoutException = timeoutException;
    }

    TimeoutException timeoutException() {
        return timeoutException.get();
    }
}
