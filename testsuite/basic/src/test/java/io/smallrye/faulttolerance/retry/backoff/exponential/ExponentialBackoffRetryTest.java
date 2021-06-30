package io.smallrye.faulttolerance.retry.backoff.exponential;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
@EnabledOnOs(OS.LINUX)
public class ExponentialBackoffRetryTest {
    @Inject
    private ExponentialBackoffRetryService service;

    @Test
    public void test() {
        assertThatThrownBy(() -> {
            service.hello();
        }).isExactlyInstanceOf(IllegalArgumentException.class);
        // 100 + 200 + 400 + 800 > 1000
        assertThat(service.getAttempts()).hasValue(4);
    }
}
