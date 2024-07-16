package io.smallrye.faulttolerance.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.enterprise.util.Nonbinding;

import org.eclipse.microprofile.faulttolerance.ExecutionContext;

import io.smallrye.common.annotation.Experimental;

/**
 * Defines a handler or method that should be called before any retry, but not before the initial attempt.
 * May only be present on elements that are also annotated {@code @Retry}.
 * <p>
 * The before retry action must be fast and non-blocking (i.e., it must not do any IO or
 * long-running computations) and must not throw an exception.
 * <p>
 * Only one of {@link #value()} and {@link #methodName()} can be specified. If both of them are,
 * deployment problem occurs.
 *
 * @see #value()
 * @see #methodName()
 */
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
@Experimental("first attempt at providing before retry actions")
public @interface BeforeRetry {
    class DEFAULT implements BeforeRetryHandler {
        @Override
        public void handle(ExecutionContext context) {
        }
    }

    /**
     * Specify the before retry handler class to be used. A new instance of the handler class
     * is created for each retry.
     *
     * @return the before retry handler class
     */
    @Nonbinding
    Class<? extends BeforeRetryHandler> value() default DEFAULT.class;

    /**
     * Specifies the name of the method to call before each retry. This method belongs to the same class
     * as the method that is being retried. The method must have no parameters and return {@code void}.
     *
     * @return the method to call before each retry
     */
    @Nonbinding
    String methodName() default "";
}
