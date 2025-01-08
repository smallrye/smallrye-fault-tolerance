package io.smallrye.faulttolerance.reuse.config.typedguard.retry.disable;

import static org.assertj.core.api.Assertions.assertThat;

import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;
import io.smallrye.faulttolerance.util.WithSystemProperty;

@FaultToleranceBasicTest
@AddBeanClasses(MyFaultTolerance.class)
@WithSystemProperty(key = "smallrye.faulttolerance.\"my-fault-tolerance\".retry.enabled", value = "false")
public class ReuseTypedGuardConfigRetryDisableTest {
    @Test
    public void test(MyService service) {
        assertThat(service.hello()).isEqualTo("fallback");
        assertThat(MyService.COUNTER).hasValue(1);
    }
}
