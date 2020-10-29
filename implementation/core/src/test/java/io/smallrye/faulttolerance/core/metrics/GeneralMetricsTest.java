package io.smallrye.faulttolerance.core.metrics;

import static io.smallrye.faulttolerance.core.Invocation.invocation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.function.Supplier;

import org.junit.Test;

import io.smallrye.faulttolerance.core.InvocationContext;
import io.smallrye.faulttolerance.core.circuit.breaker.CircuitBreakerEvents;
import io.smallrye.faulttolerance.core.util.TestException;

// TODO should have a test for all metrics
public class GeneralMetricsTest {
    @Test
    public void successfulInvocation() throws Exception {
        MockMetricsRecorder metrics = new MockMetricsRecorder();

        MetricsCollector<String> collector = new MetricsCollector<>(invocation(), metrics, false);
        assertThat(collector.apply(new InvocationContext<>(() -> "foobar"))).isEqualTo("foobar");

        assertThat(metrics.valueReturned).isEqualTo(1);
        assertThat(metrics.exceptionThrown).isEqualTo(0);
    }

    @Test
    public void failingInvocation() {
        MockMetricsRecorder metrics = new MockMetricsRecorder();

        MetricsCollector<Void> collector = new MetricsCollector<>(invocation(), metrics, false);
        assertThatThrownBy(() -> collector.apply(new InvocationContext<>(TestException::doThrow)))
                .isExactlyInstanceOf(TestException.class);

        assertThat(metrics.valueReturned).isEqualTo(0);
        assertThat(metrics.exceptionThrown).isEqualTo(1);
    }

    private static class MockMetricsRecorder implements MetricsRecorder {
        int valueReturned;
        int exceptionThrown;

        @Override
        public void executionFinished(boolean succeeded, boolean fallbackDefined, boolean fallbackApplied) {
            if (succeeded) {
                valueReturned++;
            } else {
                exceptionThrown++;
            }
        }

        @Override
        public void retryAttempted() {
        }

        @Override
        public void retryValueReturned(boolean retried) {
        }

        @Override
        public void retryExceptionNotRetryable(boolean retried) {
        }

        @Override
        public void retryMaxRetriesReached(boolean retried) {
        }

        @Override
        public void retryMaxDurationReached(boolean retried) {
        }

        @Override
        public void timeoutFinished(boolean timedOut, long time) {
        }

        @Override
        public void circuitBreakerFinished(CircuitBreakerEvents.Result result) {
        }

        @Override
        public void circuitBreakerMovedToOpen() {
        }

        @Override
        public void registerCircuitBreakerTimeSpentInClosed(Supplier<Long> supplier) {
        }

        @Override
        public void registerCircuitBreakerTimeSpentInOpen(Supplier<Long> supplier) {
        }

        @Override
        public void registerCircuitBreakerTimeSpentInHalfOpen(Supplier<Long> supplier) {
        }

        @Override
        public void bulkheadDecisionMade(boolean accepted) {
        }

        @Override
        public void registerBulkheadExecutionsRunning(Supplier<Long> supplier) {
        }

        @Override
        public void registerBulkheadExecutionsWaiting(Supplier<Long> supplier) {
        }

        @Override
        public void updateBulkheadRunningDuration(long time) {
        }

        @Override
        public void updateBulkheadWaitingDuration(long time) {
        }
    }
}
