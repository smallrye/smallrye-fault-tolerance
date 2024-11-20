package io.smallrye.faulttolerance.reuse.sync.fallback.typedguard;

import static org.assertj.core.api.Assertions.assertThat;

import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
@AddBeanClasses(MyFaultTolerance.class)
public class ReuseSyncFallbackTypedGuardTest {
    @Test
    public void test(MyService service) {
        assertThat(service.hello()).isEqualTo("better fallback");
        assertThat(MyService.COUNTER).hasValue(3);
    }
}
