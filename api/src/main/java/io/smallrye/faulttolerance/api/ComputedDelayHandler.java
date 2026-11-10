package io.smallrye.faulttolerance.api;

import java.time.Duration;

import org.eclipse.microprofile.faulttolerance.ExecutionContext;

import io.smallrye.common.annotation.Experimental;

/**
 * For each retry attempt, a new instance of the handler is created by the CDI container.
 * The instance is created before the {@link #getDelay(ExecutionContext)} method is called,
 * and after it finishes, the instance is destroyed. The implementation class can declare
 * injection points, lifecycle callbacks and so on.
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
 * <h2>Usage</h2>
 *
 * <pre>
 * &#064;ApplicationScoped
 * public class MyService {
 *     &#064;Retry
 *     &#064;ComputedDelay(MyDelayComputation.class)
 *     String doSomething() {
 *         ...
 *     }
 * }
 *
 * public class MyDelayComputation implements ComputedDelayHandler {
 *     &#064;Override
 *     public Duration getDelay(ExecutionContext context) {
 *         if (context.getFailure() instanceof MyExceptionWithDelay e) {
 *             return Duration.ofMillis(e.getDelay());
 *         }
 *         return null;
 *     }
 * }
 * </pre>
 */
@Experimental("first attempt at supporting Retry-After delay extraction")
public interface ComputedDelayHandler {
    /**
     * Determines the delay before retrying based on the execution context.
     *
     * @param context the execution context
     * @return the suggested retry delay, or {@code null} if no delay can be determined
     */
    Duration getDelay(ExecutionContext context);
}
