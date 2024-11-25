package io.smallrye.faulttolerance.config.better;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;
import io.smallrye.faulttolerance.util.WithSystemProperty;

@FaultToleranceBasicTest
@WithSystemProperty(key = "smallrye.faulttolerance.global.fallback.apply-on", value = "io.smallrye.faulttolerance.config.better.TestConfigExceptionA")
public class FallbackApplyOnConfigTest {
    @Inject
    private FallbackConfigBean bean;

    @Test
    public void applyOn() {
        assertThat(bean.applyOn()).isEqualTo("FALLBACK");
    }
}
