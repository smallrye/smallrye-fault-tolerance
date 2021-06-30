package io.smallrye.faulttolerance.retry.backoff.exponential.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junitpioneer.jupiter.SetSystemProperty;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
@SetSystemProperty(key = "io.smallrye.faulttolerance.retry.backoff.exponential.config.ConfiguredExponentialBackoffRetryService/hello/ExponentialBackoff/factor", value = "3")
@EnabledOnOs(OS.LINUX)
public class ConfiguredExponentialBackoffRetryTest {
    @Inject
    private ConfiguredExponentialBackoffRetryService service;

    @Test
    public void test() {
        assertThatThrownBy(() -> {
            service.hello();
        }).isExactlyInstanceOf(IllegalArgumentException.class);
        // 100 + 300 + 900 > 800
        assertThat(service.getAttempts()).hasValue(3);
    }
}
