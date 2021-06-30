package io.smallrye.faulttolerance.api;

import io.smallrye.common.annotation.Experimental;

/**
 * For each invocation of a method annotated {@code @Retry} and {@code @CustomBackoff},
 * an instance of custom backoff strategy is created and used for computing delays
 * between individual retry attempts. Therefore, implementations of this interface
 * must have a public zero-parameter constructor and should generally be cheap
 * to construct and not use a lot of memory. Additionally, they may be used from
 * multiple threads, so they must be thread safe.
 *
 * @see #init(long)
 * @see #nextDelayInMillis(Throwable)
 * @see CustomBackoff
 */
@Experimental("first attempt at providing additional retry backoff strategies")
public interface CustomBackoffStrategy {
    /**
     * Called once, after instantiation. Can be used to initialize state, if needed.
     * Implementations are free to use {@code initialDelayInMillis} as they see fit;
     * possibly even ignore, if the concept of initial delay doesn't make sense
     * for the strategy.
     *
     * @param initialDelayInMillis initial delay in milliseconds, per {@code Retry.delay} and {@code Retry.delayUnit}
     */
    default void init(long initialDelayInMillis) {
    }

    /**
     * Called to compute a delay before each retry attempt, including before the first retry.
     * Implementations must be fast and non-blocking (i.e., they must not do any IO or
     * long-running computations).
     *
     * @param exception exception that caused the retry attempt
     * @return delay in milliseconds; must be {@code >= 0}
     */
    long nextDelayInMillis(Throwable exception);
}
