package io.smallrye.faulttolerance.retry.backoff.custom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
@EnabledOnOs(OS.LINUX)
public class CustomBackoffRetryTest {
    @Inject
    private CustomBackoffRetryService service;

    @BeforeEach
    public void setUp() {
        TestBackoffStrategy.clear();
    }

    @Test
    public void retryOnFailure() {
        assertThatThrownBy(() -> {
            service.helloWithFailures();
        }).isExactlyInstanceOf(NullPointerException.class);
        // 250 + 250 + 250 > 600
        assertThat(service.getAttempts()).hasValue(3);

        assertThat(TestBackoffStrategy.initialDelay).isEqualTo(100);
        assertThat(TestBackoffStrategy.results).containsExactly(null, null, null);
        assertThat(TestBackoffStrategy.exceptions).containsExactly(
                IllegalArgumentException.class,
                IllegalStateException.class,
                NullPointerException.class);
    }

    @Test
    public void retryOnResult() {
        assertThat(service.helloWithResults()).isEqualTo("hello");
        assertThat(service.getAttempts()).hasValue(4);

        assertThat(TestBackoffStrategy.initialDelay).isEqualTo(10);
        assertThat(TestBackoffStrategy.results).containsExactly(
                Integer.class,
                Double.class,
                Object.class);
        assertThat(TestBackoffStrategy.exceptions).containsExactly(null, null, null);
    }
}
