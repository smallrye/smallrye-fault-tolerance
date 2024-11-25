package io.smallrye.faulttolerance.config.better;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.api.RateLimitException;
import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;
import io.smallrye.faulttolerance.util.WithSystemProperty;

@FaultToleranceBasicTest
@WithSystemProperty(key = "smallrye.faulttolerance.\"io.smallrye.faulttolerance.config.better.RateLimitConfigBean/value\".rate-limit.value", value = "3")
@WithSystemProperty(key = "smallrye.faulttolerance.\"io.smallrye.faulttolerance.config.better.RateLimitConfigBean/window\".rate-limit.window", value = "100")
@WithSystemProperty(key = "smallrye.faulttolerance.\"io.smallrye.faulttolerance.config.better.RateLimitConfigBean/window\".rate-limit.window-unit", value = "millis")
@WithSystemProperty(key = "smallrye.faulttolerance.\"io.smallrye.faulttolerance.config.better.RateLimitConfigBean/minSpacing\".rate-limit.min-spacing", value = "100")
@WithSystemProperty(key = "smallrye.faulttolerance.\"io.smallrye.faulttolerance.config.better.RateLimitConfigBean/minSpacing\".rate-limit.min-spacing-unit", value = "millis")
public class RateLimitConfigTest {
    @Inject
    private RateLimitConfigBean bean;

    @Test
    public void value() throws Exception {
        for (int i = 0; i < 3; i++) {
            assertThat(bean.value()).isEqualTo("value");
        }
        assertThatThrownBy(() -> bean.value()).isExactlyInstanceOf(RateLimitException.class);
    }

    @Test
    public void window() throws Exception {
        for (int i = 0; i < 3; i++) {
            assertThat(bean.window()).isEqualTo("window");
        }
        assertThatThrownBy(() -> bean.window()).isExactlyInstanceOf(RateLimitException.class);

        Thread.sleep(500);

        assertThat(bean.window()).isEqualTo("window");
    }

    @Test
    public void minSpacing() throws Exception {
        assertThat(bean.minSpacing()).isEqualTo("minSpacing");
        assertThatThrownBy(() -> bean.minSpacing()).isExactlyInstanceOf(RateLimitException.class);

        Thread.sleep(500);

        assertThat(bean.minSpacing()).isEqualTo("minSpacing");
    }
}
