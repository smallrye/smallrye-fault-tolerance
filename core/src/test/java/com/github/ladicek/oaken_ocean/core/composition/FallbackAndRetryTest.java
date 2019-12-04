package com.github.ladicek.oaken_ocean.core.composition;

import static com.github.ladicek.oaken_ocean.core.Invocation.invocation;
import static com.github.ladicek.oaken_ocean.core.composition.Strategies.fallback;
import static com.github.ladicek.oaken_ocean.core.composition.Strategies.retry;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.github.ladicek.oaken_ocean.core.FaultToleranceStrategy;
import com.github.ladicek.oaken_ocean.core.SimpleInvocationContext;
import com.github.ladicek.oaken_ocean.core.retry.TestInvocation;
import com.github.ladicek.oaken_ocean.core.util.TestException;

public class FallbackAndRetryTest {
    @Test
    public void shouldFallbackAfterRetrying() throws Exception {
        FaultToleranceStrategy<String, SimpleInvocationContext<String>> operation = fallback(retry(invocation()));

        assertThat(operation.apply(new SimpleInvocationContext<>(TestException::doThrow)))
                .isEqualTo("fallback after TestException");
    }

    @Test
    public void shouldNotFallbackOnSuccess() throws Exception {
        FaultToleranceStrategy<String, SimpleInvocationContext<String>> operation = fallback(retry(invocation()));

        assertThat(operation.apply(new SimpleInvocationContext<>(() -> "foobar"))).isEqualTo("foobar");
    }

    @Test
    public void shouldNotFallbackOnSuccessAtSecondAttempt() throws Exception {
        AtomicInteger failures = new AtomicInteger(0);

        FaultToleranceStrategy<String, SimpleInvocationContext<String>> operation = fallback(
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
