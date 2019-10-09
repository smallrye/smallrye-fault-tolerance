package com.github.ladicek.oaken_ocean.core.composition;

import com.github.ladicek.oaken_ocean.core.retry.TestAction;
import com.github.ladicek.oaken_ocean.core.util.TestException;
import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.ladicek.oaken_ocean.core.composition.Strategies.fallback;
import static com.github.ladicek.oaken_ocean.core.composition.Strategies.retry;
import static org.assertj.core.api.Assertions.assertThat;

public class FallbackAndRetryTest {
    @Test
    public void shouldFallbackAfterRetrying() throws Exception {
        Callable<String> operation = fallback(retry(TestException::doThrow));

        assertThat(operation.call()).isEqualTo("fallback after [retry reached max retries or max retry duration]");
    }

    @Test
    public void shouldNotFallbackOnSuccess() throws Exception {
        Callable<String> operation = fallback(retry(() -> "foobar"));

        assertThat(operation.call()).isEqualTo("foobar");
    }

    @Test
    public void shouldNotFallbackOnSuccessAtSecondAttempt() throws Exception {
        AtomicInteger failures = new AtomicInteger(0);

        Callable<String> operation =
                fallback(
                        retry(
                                TestAction.initiallyFailing(
                                        3, () -> {
                                            failures.incrementAndGet();
                                            return new RuntimeException();
                                        },
                                        () -> String.format("success after %d failures", failures.get())
                                )
                        )
                );

        assertThat(operation.call()).isEqualTo("success after 3 failures");
    }
}
