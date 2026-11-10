package io.smallrye.faulttolerance.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.enterprise.util.Nonbinding;

import io.smallrye.common.annotation.Experimental;

/**
 * Defines a handler that should be called before any retry to obtain a suggested retry delay from the failure.
 * May only be present on elements that are also annotated {@code @Retry}.
 * <p>
 * If this handler returns a zero or positive {@link java.time.Duration Duration}, that duration is used
 * as the retry delay (with jitter applied, as configured on {@code @Retry}).
 * <p>
 * If this handler returns {@code null}, the configured delay and backoff strategy are used instead.
 * <p>
 * The handler must not return a negative duration. If it does, a warning is logged and the configured delay
 * and backoff strategy are used instead.
 * <p>
 * The computed delay handler must be fast and non-blocking (i.e., it must not do any IO or long-running computations)
 * and must not throw an exception.
 *
 * @see #value()
 */
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
@Experimental("first attempt at supporting Retry-After delay extraction")
public @interface ComputedDelay {
    /**
     * Specify the computed delay handler class to be used.
     *
     * @return the computed delay handler class
     */
    @Nonbinding
    Class<? extends ComputedDelayHandler> value();
}
