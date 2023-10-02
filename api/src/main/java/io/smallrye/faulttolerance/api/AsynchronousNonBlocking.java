package io.smallrye.faulttolerance.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.interceptor.InterceptorBinding;

import io.smallrye.common.annotation.Experimental;

/**
 * Alternative to MicroProfile Fault Tolerance's {@link org.eclipse.microprofile.faulttolerance.Asynchronous @Asynchronous}
 * for guarding non-blocking asynchronous methods (executed on the original thread). It may only be present on methods
 * that declare return type of {@link java.util.concurrent.CompletionStage CompletionStage}. Other than that, it has the same
 * meaning as MicroProfile Fault Tolerance's {@link org.eclipse.microprofile.faulttolerance.Asynchronous @Asynchronous}.
 * More specifically:
 * <p>
 * When a method marked with this annotation is called, the method call is allowed to proceed on the original thread.
 * It is assumed that the guarded method will, at some point, perform some non-blocking asynchronous operation(s),
 * such as non-blocking IO, and that it synchronously returns a {@code CompletionStage}. It is further assumed that
 * the completion of the asynchronos non-blocking operation(s) executed by the guarded method is followed by
 * completion of the returned {@code CompletionStage}.
 * <p>
 * When the guarded method returns, a {@code CompletionStage} is returned to the caller and can be used to access
 * the result of the asynchronous execution, when it completes.
 * <p>
 * Before the asynchronous execution completes, the {@code CompletionStage} returned to the caller is incomplete.
 * Once the asynchronous execution completes, the {@code CompletionStage} returned to the caller assumes behavior
 * that is equivalent to the {@code CompletionStage} returned by the guarded method. If the guarded method
 * synchronously throws an exception, the returned {@code CompletionStage} completes with that exception.
 * <p>
 * If a method marked with this annotation doesn't declare return type of {@code CompletionStage},
 * {@link org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException
 * FaultToleranceDefinitionException}
 * occurs during deployment.
 * <p>
 * If a class is annotated with this annotation, all its methods are treated as if they were marked with this annotation.
 * If one of the methods doesn't return {@code CompletionStage}, {@code FaultToleranceDefinitionException}
 * occurs during deployment.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
@InterceptorBinding
@Inherited
@Experimental("second attempt at better handling of blocking/non-blocking asynchrony")
public @interface AsynchronousNonBlocking {
}
