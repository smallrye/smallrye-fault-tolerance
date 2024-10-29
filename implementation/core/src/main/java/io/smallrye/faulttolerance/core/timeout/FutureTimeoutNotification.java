package io.smallrye.faulttolerance.core.timeout;

import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;

@FunctionalInterface
interface FutureTimeoutNotification {
    void accept(TimeoutException timeoutException);
}
