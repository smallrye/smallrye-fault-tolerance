package io.smallrye.faulttolerance.reuse.mixed.sync.fallback;

import static org.assertj.core.api.Assertions.assertThat;

import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
@AddBeanClasses(MyFaultTolerance.class)
public class MixedReuseSyncFallbackTest {
    @Test
    public void test(MyService service) {
        assertThat(service.hello()).isEqualTo("hello");
        assertThat(MyService.STRING_COUNTER).hasValue(3);

        assertThat(service.theAnswer()).isEqualTo(42);
        assertThat(MyService.INT_COUNTER).hasValue(3);
    }
}
