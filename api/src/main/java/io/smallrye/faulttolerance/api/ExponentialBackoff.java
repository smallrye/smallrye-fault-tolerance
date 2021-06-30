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
 * Modifies a {@code @Retry} annotation to use exponential backoff instead of the default constant backoff.
 * May only be present on elements that are also annotated {@code @Retry}.
 * <p>
 * Exponential backoff uses the initial delay before the first retry attempt, and then increases the delays
 * exponentially. With the default factor of 2, the first few delays are: initial delay, 2 * initial delay,
 * 4 * initial delay, 8 * initial delay, 16 * initial delay, 32 * initial delay, etc. (Additionally, jitter
 * will be applied to each value.) To prevent unbounded growth of these delays, {@link #maxDelay()} should
 * be configured.
 * <p>
 * All configuration options of {@code @Retry} still apply and all their constraints are still enforced.
 * Additionally:
 * <ul>
 * <li>{@code delay}, {@code delayUnit}: is used as an initial delay, before the first retry attempt. Must be less
 * than {@link #maxDelay()}. Note that if 0, exponential backoff degenerates to zero backoff.</li>
 * </ul>
 *
 * @see #factor()
 * @see #maxDelay()
 * @see #maxDelayUnit()
 */
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
@Experimental("first attempt at providing additional retry backoff strategies")
public @interface ExponentialBackoff {
    /**
     * The multiplicative factor used when determining a delay between two retries.
     * A delay is computed as {@code factor * previousDelay}, resulting in an exponential
     * growth.
     * <p>
     * The value must be greater than or equal to 1 (though with factor of 1, exponential
     * backoff degenerates to constant backoff).
     *
     * @return the growth factor
     */
    int factor() default 2;

    /**
     * The maximum delay between retries. Defaults to 1 minute. The value must be greater than or equal to 0,
     * and must be less than {@code Retry.maxDuration} (if that is set). 0 means not set.
     * <p>
     * Note that this is different from {@code maxDuration}. This places a limit on each individual delay
     * between retries, while {@code maxDuration} places a limit on the total time all retries may take.
     *
     * @return the max delay time
     */
    long maxDelay() default 60_000;

    /**
     * The unit for {@link #maxDelay}. Defaults to {@link java.time.temporal.ChronoUnit#MILLIS}.
     *
     * @return the max delay unit
     */
    ChronoUnit maxDelayUnit() default ChronoUnit.MILLIS;
}
