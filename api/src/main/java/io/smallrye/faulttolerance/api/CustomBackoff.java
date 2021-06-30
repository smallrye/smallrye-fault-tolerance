package io.smallrye.faulttolerance.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.smallrye.common.annotation.Experimental;

/**
 * Modifies a {@code @Retry} annotation to use a custom backoff strategy instead of the default constant backoff.
 * May only be present on elements that are also annotated {@code @Retry}.
 * <p>
 * For each invocation of a method annotated {@code @Retry}, an instance of given {@link CustomBackoffStrategy}
 * is created and used for computing delays between individual retry attempts, including before the first retry.
 * <p>
 * All configuration options of {@code @Retry} still apply and all their constraints are still enforced.
 * Additionally:
 * <ul>
 * <li>{@code delay}, {@code delayUnit}: passed to {@link CustomBackoffStrategy#init(long)} as an
 * initial delay. The strategy may ignore it if the concept of initial delay doesn't make sense.</li>
 * </ul>
 *
 * @see #value()
 */
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
@Experimental("first attempt at providing additional retry backoff strategies")
public @interface CustomBackoff {
    /**
     * Class of the custom backoff strategy that will be used to compute retry delays.
     *
     * @return custom backoff strategy class
     *
     * @see CustomBackoffStrategy
     */
    Class<? extends CustomBackoffStrategy> value();
}
