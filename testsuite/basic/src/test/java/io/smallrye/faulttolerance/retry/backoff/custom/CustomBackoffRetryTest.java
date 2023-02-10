package io.smallrye.faulttolerance.retry.backoff.custom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
@EnabledOnOs(OS.LINUX)
public class CustomBackoffRetryTest {
    @Inject
    private CustomBackoffRetryService service;

    @Test
    public void test() {
        assertThatThrownBy(() -> {
            service.hello();
        }).isExactlyInstanceOf(NullPointerException.class);
        // 250 + 250 + 250 > 600
        assertThat(service.getAttempts()).hasValue(3);

        assertThat(TestBackoffStrategy.initialDelay).isEqualTo(100);
        assertThat(TestBackoffStrategy.exceptions).containsExactly(
                IllegalArgumentException.class,
                IllegalStateException.class,
                NullPointerException.class);
    }
}
