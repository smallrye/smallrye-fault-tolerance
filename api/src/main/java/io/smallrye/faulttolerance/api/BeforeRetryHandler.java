package io.smallrye.faulttolerance.api;

import org.eclipse.microprofile.faulttolerance.ExecutionContext;

/**
 * For each retry attempt, a new instance of the handler is created by the CDI container.
 * The instance is created before the {@link #handle(ExecutionContext)} method is called,
 * and after it finishes, the instance is destroyed. The implementation class can declare
 * injection points, lifecycle callbacks and so on.
 *
 * <h2>Usage</h2>
 *
 * <pre>
 * &#064;ApplicationScoped
 * public class MyService {
 *     &#064;Inject
 *     OtherService otherService;
 *
 *     &#064;Retry
 *     &#064;BeforeRetry(MyBeforeRetry.class)
 *     String doSomething() {
 *         return otherService.hello();
 *     }
 * }
 *
 * public class MyBeforeRetry implements BeforeRetryHandler {
 *     &#064;Inject
 *     OtherService otherService;
 *
 *     void handle(ExecutionContext context) {
 *         otherService.reset();
 *     }
 * }
 * </pre>
 */
public interface BeforeRetryHandler {
    /**
     * Do what is necessary before the next retry attempt.
     *
     * @param context the execution context
     */
    void handle(ExecutionContext context);
}
