package io.smallrye.faulttolerance.metrics;

import com.netflix.hystrix.exception.HystrixRuntimeException;
import io.smallrye.faulttolerance.SimpleCommand;
import io.smallrye.faulttolerance.SynchronousCircuitBreaker;

public interface MetricsCollector {

    MetricsCollector NOOP = new MetricsCollector() {

        @Override
        public void onError(SimpleCommand command, HystrixRuntimeException exception) {
        }

        @Override
        public void onProcessedError(SimpleCommand command, Exception exception) {
        }

        @Override
        public void afterExecute(SimpleCommand command) {
        }

        @Override
        public void beforeExecute(SimpleCommand command) {
        }

        @Override
        public void afterSuccess(SimpleCommand command) {
        }

        @Override
        public void init(SynchronousCircuitBreaker circuitBreaker) {
        }

    };

    /**
     * Initialize and register selected circuit breaker metrics if needed.
     *
     * @param circuitBreaker
     */
    void init(SynchronousCircuitBreaker circuitBreaker);

    /**
     * Before the command is executed.
     *
     * @param command
     */
    void beforeExecute(SimpleCommand command);

    /**
     * After the command was successfully executed.
     *
     * @param command
     */
    void afterSuccess(SimpleCommand command);

    /**
     * When invocation fails with {@link HystrixRuntimeException}.
     *
     * @param command
     * @param exception
     */
    void onError(SimpleCommand command, HystrixRuntimeException exception);

    /**
     * When invocation fails with {@link HystrixRuntimeException}, after the exception was processed.
     *
     * @param command
     * @param exception May be null
     */
    void onProcessedError(SimpleCommand command, Exception exception);

    /**
     * After the command was executed.
     *
     * @param command
     */
    void afterExecute(SimpleCommand command);

}
