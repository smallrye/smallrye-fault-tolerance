package io.smallrye.faulttolerance.retry.backoff.fibonacci;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
@EnabledOnOs(OS.LINUX)
public class FibonacciBackoffRetryTest {
    @Inject
    private FibonacciBackoffRetryService service;

    @Test
    public void test() {
        assertThatThrownBy(() -> {
            service.hello();
        }).isExactlyInstanceOf(IllegalArgumentException.class);
        // 100 + 200 + 300 + 500 > 800
        assertThat(service.getAttempts()).hasValue(4);
    }
}
