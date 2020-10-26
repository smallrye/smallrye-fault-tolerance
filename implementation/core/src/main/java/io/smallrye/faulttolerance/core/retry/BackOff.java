package io.smallrye.faulttolerance.core.retry;

/**
 * Computes the delay value.
 * <p>
 * For each invocation of the retry strategy, one instance is obtained and used for computing all delay values.
 * This instance may be used from multiple threads, but is not used concurrently.
 * That is, implementations can hold state, but must take care of its visibility across threads.
 */
public interface BackOff {
    /** @return non-negative number */
    long getInMillis();

    BackOff ZERO = () -> 0;
}
