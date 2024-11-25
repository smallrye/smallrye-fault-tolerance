package io.smallrye.faulttolerance.config.better;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;
import io.smallrye.faulttolerance.util.WithSystemProperty;

@FaultToleranceBasicTest
@WithSystemProperty(key = "smallrye.faulttolerance.\"io.smallrye.faulttolerance.config.better.FallbackConfigBean/applyOn\".fallback.apply-on", value = "io.smallrye.faulttolerance.config.better.TestConfigExceptionA")
@WithSystemProperty(key = "smallrye.faulttolerance.\"io.smallrye.faulttolerance.config.better.FallbackConfigBean/skipOn\".fallback.skip-on", value = "io.smallrye.faulttolerance.config.better.TestConfigExceptionA")
@WithSystemProperty(key = "smallrye.faulttolerance.\"io.smallrye.faulttolerance.config.better.FallbackConfigBean/fallbackMethod\".fallback.fallback-method", value = "anotherFallback")
@WithSystemProperty(key = "smallrye.faulttolerance.\"io.smallrye.faulttolerance.config.better.FallbackConfigBean/fallbackHandler\".fallback.value", value = "io.smallrye.faulttolerance.config.better.FallbackHandlerB")
public class FallbackConfigTest {
    @Inject
    private FallbackConfigBean bean;

    @Test
    public void applyOn() {
        assertThat(bean.applyOn()).isEqualTo("FALLBACK");
    }

    @Test
    public void skipOn() {
        assertThatThrownBy(() -> bean.skipOn()).isExactlyInstanceOf(TestConfigExceptionA.class);
    }

    @Test
    public void fallbackMethod() {
        assertThat(bean.fallbackMethod()).isEqualTo("ANOTHER FALLBACK");
    }

    @Test
    public void fallbackHandler() {
        assertThat(bean.fallbackHandler()).isEqualTo("FallbackHandlerB");
    }
}
