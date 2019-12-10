package io.smallrye.faulttolerance.core.composition;

import static io.smallrye.faulttolerance.core.Invocation.invocation;
import static io.smallrye.faulttolerance.core.composition.Strategies.fallback;
import static io.smallrye.faulttolerance.core.composition.Strategies.retry;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.InvocationContext;
import io.smallrye.faulttolerance.core.retry.TestInvocation;
import io.smallrye.faulttolerance.core.util.TestException;

public class FallbackAndRetryTest {
    @Test
    public void shouldFallbackAfterRetrying() throws Exception {
        FaultToleranceStrategy<String> operation = fallback(retry(invocation()));

        assertThat(operation.apply(new InvocationContext<>(TestException::doThrow)))
                .isEqualTo("fallback after TestException");
    }

    @Test
    public void shouldNotFallbackOnSuccess() throws Exception {
        FaultToleranceStrategy<String> operation = fallback(retry(invocation()));

        assertThat(operation.apply(new InvocationContext<>(() -> "foobar"))).isEqualTo("foobar");
    }

    @Test
    public void shouldNotFallbackOnSuccessAtSecondAttempt() throws Exception {
        AtomicInteger failures = new AtomicInteger(0);

        FaultToleranceStrategy<String> operation = fallback(
                retry(
                        TestInvocation.initiallyFailing(
                                3, () -> {
                                    failures.incrementAndGet();
                                    return new RuntimeException();
                                },
                                () -> String.format("success after %d failures", failures.get()))));

        assertThat(operation.apply(null)).isEqualTo("success after 3 failures");
    }
}
