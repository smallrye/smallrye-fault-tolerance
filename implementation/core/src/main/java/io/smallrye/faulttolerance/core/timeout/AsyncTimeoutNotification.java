package io.smallrye.faulttolerance.core.timeout;

import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;

@FunctionalInterface
interface AsyncTimeoutNotification {
    void accept(TimeoutException timeoutException);
}
