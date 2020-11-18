package io.smallrye.faulttolerance.core.retry;

import io.smallrye.faulttolerance.core.InvocationContextEvent;

public class RetryEvents {
    public enum Result {
        VALUE_RETURNED,
        EXCEPTION_NOT_RETRYABLE,
        MAX_RETRIES_REACHED,
        MAX_DURATION_REACHED,
    }

    public enum Retried implements InvocationContextEvent {
        INSTANCE
    }

    public enum Finished implements InvocationContextEvent {
        VALUE_RETURNED(Result.VALUE_RETURNED),
        EXCEPTION_NOT_RETRYABLE(Result.EXCEPTION_NOT_RETRYABLE),
        MAX_RETRIES_REACHED(Result.MAX_RETRIES_REACHED),
        MAX_DURATION_REACHED(Result.MAX_DURATION_REACHED),
        ;

        public final Result result;

        Finished(Result result) {
            this.result = result;
        }
    }
}
