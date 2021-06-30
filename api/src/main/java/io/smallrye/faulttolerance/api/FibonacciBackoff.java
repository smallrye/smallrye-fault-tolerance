package io.smallrye.faulttolerance.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.temporal.ChronoUnit;

import io.smallrye.common.annotation.Experimental;

/**
 * Modifies a {@code @Retry} annotation to use Fibonacci backoff instead of the default constant backoff.
 * May only be present on elements that are also annotated {@code @Retry}.
 * <p>
 * Fibonacci backoff uses the initial delay before the first retry attempt, and then increases the delays
 * per the Fibonacci sequence. The first few delays are: initial delay, 2 * initial delay, 3 * initial delay,
 * 5 * initial delay, 8 * initial delay, 13 * initial delay, etc. (Additionally, jitter will be applied
 * to each value.) To prevent unbounded growth of these delays, {@link #maxDelay()} should be configured.
 * <p>
 * All configuration options of {@code @Retry} still apply and all their constraints are still enforced.
 * Additionally:
 * <ul>
 * <li>{@code delay}, {@code delayUnit}: is used as an initial delay, before the first retry attempt. Must be less
 * than {@link #maxDelay()}. Note that if 0, Fibonacci backoff degenerates to zero backoff.</li>
 * </ul>
 *
 * @see #maxDelay()
 * @see #maxDelayUnit()
 */
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
@Experimental("first attempt at providing additional retry backoff strategies")
public @interface FibonacciBackoff {
    /**
     * The maximum delay between retries. Defaults to 1 minute. The value must be greater than or equal to 0,
     * and must be less than {@code Retry.maxDuration}. 0 means not set.
     * <p>
     * Note that this is different from {@code maxDuration}. This places a limit on each individual delay
     * between retries, while {@code maxDuration} places a limit on the total time all retries may take.
     *
     * @return the max delay time
     */
    long maxDelay() default 60_000;

    /**
     * The unit for {@link #maxDelay}. Defaults to {@link ChronoUnit#MILLIS}.
     *
     * @return the max delay unit
     */
    ChronoUnit maxDelayUnit() default ChronoUnit.MILLIS;
}
