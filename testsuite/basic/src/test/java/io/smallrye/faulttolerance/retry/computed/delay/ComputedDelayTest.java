package io.smallrye.faulttolerance.retry.computed.delay;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;

import org.jboss.weld.junit.jupiter.auto.AddBeanClasses;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
@AddBeanClasses(MyComputedDelayDependency.class)
public class ComputedDelayTest {
    @BeforeEach
    public void setUp() {
        MyComputedDelayHandler.dependencyIds.clear();
        MyComputedDelayHandler.observedResults.clear();
        MyComputedDelayHandler.observedFailures.clear();
        ComputedDelayService.invocationTimes.clear();
        ComputedDelayService.attempts.set(0);
    }

    @Test
    public void testComputedDelay(ComputedDelayService service) {
        service.retryWithComputedDelay();

        assertThat(ComputedDelayService.attempts.get()).isEqualTo(4);
        assertThat(MyComputedDelayHandler.dependencyIds).hasSize(3);

        long delay1 = ComputedDelayService.invocationTimes.get(1) - ComputedDelayService.invocationTimes.get(0);
        long delay2 = ComputedDelayService.invocationTimes.get(2) - ComputedDelayService.invocationTimes.get(1);
        long delay3 = ComputedDelayService.invocationTimes.get(3) - ComputedDelayService.invocationTimes.get(2);
        assertThat(delay1).isGreaterThanOrEqualTo(100); // should be 150ms
        assertThat(delay2).isGreaterThanOrEqualTo(100); // should be 150ms
        assertThat(delay3).isGreaterThanOrEqualTo(100); // should be 150ms
    }

    @Test
    public void testComputedDelayOnResult(ComputedDelayService service) {
        service.retryOnResultWithComputedDelay();

        assertThat(ComputedDelayService.attempts.get()).isEqualTo(4);
        assertThat(MyComputedDelayHandler.dependencyIds).hasSize(3);

        long delay1 = ComputedDelayService.invocationTimes.get(1) - ComputedDelayService.invocationTimes.get(0);
        long delay2 = ComputedDelayService.invocationTimes.get(2) - ComputedDelayService.invocationTimes.get(1);
        long delay3 = ComputedDelayService.invocationTimes.get(3) - ComputedDelayService.invocationTimes.get(2);
        assertThat(delay1).isGreaterThanOrEqualTo(100); // should be 150ms
        assertThat(delay2).isGreaterThanOrEqualTo(100); // should be 150ms
        assertThat(delay3).isGreaterThanOrEqualTo(100); // should be 150ms
    }

    @Test
    public void testComputedDelayOnPrimitiveResult(ComputedDelayService service) {
        assertThat(service.retryOnPrimitiveResultWithComputedDelay()).isEqualTo(0);

        assertThat(ComputedDelayService.attempts.get()).isEqualTo(4);
        assertThat(MyComputedDelayHandler.dependencyIds).hasSize(3);

        // the primitive result must reach the handler boxed, and must not be confused with a failure
        assertThat(MyComputedDelayHandler.observedResults)
                .containsExactly(150, 150, 150)
                .allMatch(Integer.class::isInstance);
        assertThat(MyComputedDelayHandler.observedFailures).hasSize(3).allMatch(Objects::isNull);

        long delay1 = ComputedDelayService.invocationTimes.get(1) - ComputedDelayService.invocationTimes.get(0);
        long delay2 = ComputedDelayService.invocationTimes.get(2) - ComputedDelayService.invocationTimes.get(1);
        long delay3 = ComputedDelayService.invocationTimes.get(3) - ComputedDelayService.invocationTimes.get(2);
        assertThat(delay1).isGreaterThanOrEqualTo(100); // should be 150ms, from the boxed result
        assertThat(delay2).isGreaterThanOrEqualTo(100); // should be 150ms, from the boxed result
        assertThat(delay3).isGreaterThanOrEqualTo(100); // should be 150ms, from the boxed result
    }

    @Test
    public void testComputedDelayOnResultAndException(ComputedDelayService service) {
        service.retryOnResultOrExceptionWithComputedDelay();

        assertThat(ComputedDelayService.attempts.get()).isEqualTo(4);
        assertThat(MyComputedDelayHandler.dependencyIds).hasSize(3);

        long delay1 = ComputedDelayService.invocationTimes.get(1) - ComputedDelayService.invocationTimes.get(0);
        long delay2 = ComputedDelayService.invocationTimes.get(2) - ComputedDelayService.invocationTimes.get(1);
        long delay3 = ComputedDelayService.invocationTimes.get(3) - ComputedDelayService.invocationTimes.get(2);
        assertThat(delay1).isGreaterThanOrEqualTo(100); // should be 150ms, from result
        assertThat(delay2).isGreaterThanOrEqualTo(150); // should be 200ms, from exception
        assertThat(delay3).isGreaterThanOrEqualTo(100); // should be 150ms, from result
    }

    @Test
    public void testComputedDelayFallbackToExponential(ComputedDelayService service) {
        service.retryWithFallbackToExponential();

        assertThat(ComputedDelayService.attempts.get()).isEqualTo(4);
        assertThat(MyComputedDelayHandler.dependencyIds).hasSize(3);

        long delay1 = ComputedDelayService.invocationTimes.get(1) - ComputedDelayService.invocationTimes.get(0);
        long delay2 = ComputedDelayService.invocationTimes.get(2) - ComputedDelayService.invocationTimes.get(1);
        long delay3 = ComputedDelayService.invocationTimes.get(3) - ComputedDelayService.invocationTimes.get(2);
        assertThat(delay1).isGreaterThanOrEqualTo(30); // should be 50ms
        assertThat(delay2).isGreaterThanOrEqualTo(70); // should be 100ms
        assertThat(delay3).isGreaterThanOrEqualTo(150); // should be 200ms
    }

    @Test
    public void testComputedDelayNonPositiveDelay(ComputedDelayService service) {
        service.retryWithNegativeDelay();

        assertThat(ComputedDelayService.attempts.get()).isEqualTo(4);
        assertThat(MyComputedDelayHandler.dependencyIds).hasSize(3);

        long delay1 = ComputedDelayService.invocationTimes.get(1) - ComputedDelayService.invocationTimes.get(0);
        long delay2 = ComputedDelayService.invocationTimes.get(2) - ComputedDelayService.invocationTimes.get(1);
        long delay3 = ComputedDelayService.invocationTimes.get(3) - ComputedDelayService.invocationTimes.get(2);
        assertThat(delay1).isGreaterThanOrEqualTo(30); // should be 50ms
        assertThat(delay2).isGreaterThanOrEqualTo(30); // should be 50ms
        assertThat(delay3).isGreaterThanOrEqualTo(30); // should be 50ms
    }

    @Test
    public void testComputedDelayExceptionInHandler(ComputedDelayService service) {
        service.retryWithExceptionInHandler();

        assertThat(ComputedDelayService.attempts.get()).isEqualTo(4);
        assertThat(MyComputedDelayHandler.dependencyIds).hasSize(3);

        long delay1 = ComputedDelayService.invocationTimes.get(1) - ComputedDelayService.invocationTimes.get(0);
        long delay2 = ComputedDelayService.invocationTimes.get(2) - ComputedDelayService.invocationTimes.get(1);
        long delay3 = ComputedDelayService.invocationTimes.get(3) - ComputedDelayService.invocationTimes.get(2);
        assertThat(delay1).isGreaterThanOrEqualTo(30); // should be 50ms
        assertThat(delay2).isGreaterThanOrEqualTo(30); // should be 50ms
        assertThat(delay3).isGreaterThanOrEqualTo(30); // should be 50ms
    }

    @Test
    public void testAsyncComputedDelayOnResult(ComputedDelayService service) throws Exception {
        RetryAfterResult result = service.asyncRetryOnResultWithComputedDelay().toCompletableFuture().get();

        assertThat(result.shouldRetry()).isFalse();
        assertThat(ComputedDelayService.attempts.get()).isEqualTo(4);
        assertThat(MyComputedDelayHandler.dependencyIds).hasSize(3);

        // the handler must see the unwrapped value the CompletionStage produced, not the CompletionStage itself
        assertThat(MyComputedDelayHandler.observedResults).hasSize(3);
        assertThat(MyComputedDelayHandler.observedResults)
                .allMatch(RetryAfterResult.class::isInstance)
                .extracting(RetryAfterResult.class::cast)
                .allMatch(RetryAfterResult::shouldRetry);
        assertThat(MyComputedDelayHandler.observedFailures).hasSize(3).allMatch(Objects::isNull);

        long delay1 = ComputedDelayService.invocationTimes.get(1) - ComputedDelayService.invocationTimes.get(0);
        long delay2 = ComputedDelayService.invocationTimes.get(2) - ComputedDelayService.invocationTimes.get(1);
        long delay3 = ComputedDelayService.invocationTimes.get(3) - ComputedDelayService.invocationTimes.get(2);
        assertThat(delay1).isGreaterThanOrEqualTo(100); // should be 150ms, from result
        assertThat(delay2).isGreaterThanOrEqualTo(100); // should be 150ms, from result
        assertThat(delay3).isGreaterThanOrEqualTo(100); // should be 150ms, from result
    }

    @Test
    public void testAsyncComputedDelayOnFailure(ComputedDelayService service) throws Exception {
        RetryAfterResult result = service.asyncRetryOnFailureWithComputedDelay().toCompletableFuture().get();

        assertThat(result.shouldRetry()).isFalse();
        assertThat(ComputedDelayService.attempts.get()).isEqualTo(4);
        assertThat(MyComputedDelayHandler.dependencyIds).hasSize(3);

        assertThat(MyComputedDelayHandler.observedResults).hasSize(3).allMatch(Objects::isNull);
        assertThat(MyComputedDelayHandler.observedFailures)
                .hasSize(3)
                .allMatch(RetryAfterException.class::isInstance);

        long delay1 = ComputedDelayService.invocationTimes.get(1) - ComputedDelayService.invocationTimes.get(0);
        long delay2 = ComputedDelayService.invocationTimes.get(2) - ComputedDelayService.invocationTimes.get(1);
        long delay3 = ComputedDelayService.invocationTimes.get(3) - ComputedDelayService.invocationTimes.get(2);
        assertThat(delay1).isGreaterThanOrEqualTo(100); // should be 150ms, from failure
        assertThat(delay2).isGreaterThanOrEqualTo(100); // should be 150ms, from failure
        assertThat(delay3).isGreaterThanOrEqualTo(100); // should be 150ms, from failure
    }
}
