package io.smallrye.faulttolerance.reuse.sync;

import static org.assertj.core.api.Assertions.assertThat;

import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
@AddBeanClasses(MyFaultTolerance.class)
public class ReuseSyncTest {
    @Test
    public void test(MyService service) {
        assertThat(service.hello()).isEqualTo("fallback");
    }
}
