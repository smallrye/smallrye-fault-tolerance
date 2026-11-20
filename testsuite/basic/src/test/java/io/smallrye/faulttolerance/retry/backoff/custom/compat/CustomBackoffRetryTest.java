package io.smallrye.faulttolerance.retry.backoff.custom.compat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
public class CustomBackoffRetryTest {
    @Inject
    private CustomBackoffRetryService service;

    @BeforeEach
    public void setUp() {
        TestBackoffStrategy.clear();
        CustomBackoffRetryService.attempts.set(0);
    }

    @Test
    public void retryOnFailure() {
        assertThatThrownBy(() -> service.helloWithFailures())
                .isExactlyInstanceOf(NullPointerException.class);

        assertThat(CustomBackoffRetryService.attempts).hasValue(3); // 1 initial invocation + 2 retries
        assertThat(TestBackoffStrategy.initialDelay).isEqualTo(100);
        assertThat(TestBackoffStrategy.exceptions).containsExactly(
                IllegalArgumentException.class,
                IllegalStateException.class);
    }

    @Test
    public void retryOnResult() {
        assertThat(service.helloWithResults()).isEqualTo("hello");

        assertThat(CustomBackoffRetryService.attempts).hasValue(3); // 1 initial invocation + 2 retries
        assertThat(TestBackoffStrategy.initialDelay).isEqualTo(10);
        // a retry caused by a rejected result has no failure, and the deprecated overload receives `null`
        assertThat(TestBackoffStrategy.exceptions).containsExactly(null, null);
    }
}
