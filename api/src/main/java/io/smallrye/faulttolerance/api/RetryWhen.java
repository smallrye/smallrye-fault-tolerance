package io.smallrye.faulttolerance.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Predicate;

import io.smallrye.common.annotation.Experimental;

/**
 * Modifies a {@code @Retry} annotation to retry when given predicate returns {@code true}.
 * May only be present on elements that are also annotated {@code @Retry}. If this annotation
 * is present and the {@code @RetryWhen.exception} member is set, the {@code @Retry.retryOn}
 * and {@code @Retry.abortOn} members must not be set.
 * <p>
 * For each usage of the {@code @RetryWhen} annotation, all given {@link Predicate}s are
 * instantiated once. The predicate classes must have a {@code public}, zero-parameter
 * constructor. They must be thread-safe, ideally stateless.
 *
 * @see #exception()
 * @see #result()
 */
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
@Experimental("first attempt at providing predicate-based retries")
public @interface RetryWhen {
    /**
     * Class of the predicate that will be used to determine whether the action should be retried
     * if the action has returned a result.
     * <p>
     * Even if the guarded action is asynchronous, the predicate takes a produced result.
     * The predicate is never passed a {@link java.util.concurrent.CompletionStage CompletionStage} or so.
     *
     * @return predicate class
     */
    Class<? extends Predicate<Object>> result() default NeverOnResult.class;

    /**
     * Class of the predicate that will be used to determine whether the action should be retried
     * if the action has thrown an exception.
     * <p>
     * Even if the guarded action is asynchronous, the predicate takes a produced exception.
     * The predicate is never passed a {@link java.util.concurrent.CompletionStage CompletionStage} or so.
     *
     * @return predicate class
     */
    Class<? extends Predicate<Throwable>> exception() default AlwaysOnException.class;
}
