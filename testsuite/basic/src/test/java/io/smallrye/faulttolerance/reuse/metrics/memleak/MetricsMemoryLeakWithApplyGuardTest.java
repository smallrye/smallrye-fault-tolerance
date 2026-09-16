package io.smallrye.faulttolerance.reuse.metrics.memleak;

import static org.assertj.core.api.Assertions.assertThat;

import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@Disabled("this only reproduces a memory leak with -Xmx128m")
@FaultToleranceBasicTest
@AddBeanClasses(MyFaultTolerance.class)
public class MetricsMemoryLeakWithApplyGuardTest {
    @Test
    public void test(MyService service) {
        for (int i = 0; i < 1_000_000; i++) {
            assertThat(service.hello()).isEqualTo("fallback");
        }
    }
}
