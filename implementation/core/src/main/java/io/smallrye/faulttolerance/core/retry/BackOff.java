package io.smallrye.faulttolerance.core.retry;

/**
 * Computes the delay value.
 * <p>
 * For each invocation of the retry strategy, one instance is obtained and used for computing all delay values.
 * This instance may be used from multiple threads, but is not used concurrently.
 * That is, implementations can hold state, but must take care of its visibility across threads.
 */
public interface BackOff {
    /**
     * @param result result causing the retry attempt before which the delay occurs,
     *        or {@code null} if the retry attempt is caused by an exception
     * @param cause exception causing the retry attempt before which the delay occurs,
     *        or {@code null} if the retry attempt is caused by a result
     * @return non-negative number
     */
    long getInMillis(Object result, Throwable cause);

    BackOff ZERO = (result, cause) -> 0;
}
