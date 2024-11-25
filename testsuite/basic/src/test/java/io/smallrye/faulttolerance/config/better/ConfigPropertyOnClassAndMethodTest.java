package io.smallrye.faulttolerance.config.better;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;
import io.smallrye.faulttolerance.util.WithSystemProperty;

@FaultToleranceBasicTest
@WithSystemProperty(key = "smallrye.faulttolerance.\"io.smallrye.faulttolerance.config.better.ConfigPropertyBean\".retry.max-retries", value = "5")
@WithSystemProperty(key = "smallrye.faulttolerance.\"io.smallrye.faulttolerance.config.better.ConfigPropertyBean/triggerException\".retry.max-retries", value = "6")
public class ConfigPropertyOnClassAndMethodTest {
    @Inject
    private ConfigPropertyBean bean;

    @Test
    void test() {
        assertThatThrownBy(() -> bean.triggerException()).isExactlyInstanceOf(IllegalStateException.class);
        assertThat(bean.getRetry()).isEqualTo(7);
    }
}
