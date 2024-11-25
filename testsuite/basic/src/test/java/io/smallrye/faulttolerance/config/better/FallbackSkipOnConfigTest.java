package io.smallrye.faulttolerance.config.better;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;
import io.smallrye.faulttolerance.util.WithSystemProperty;

@FaultToleranceBasicTest
@WithSystemProperty(key = "smallrye.faulttolerance.global.fallback.skip-on", value = "io.smallrye.faulttolerance.config.better.TestConfigExceptionA")
public class FallbackSkipOnConfigTest {
    @Inject
    private FallbackConfigBean bean;

    @Test
    public void skipOn() {
        assertThatThrownBy(() -> bean.skipOn()).isExactlyInstanceOf(TestConfigExceptionA.class);
    }
}
