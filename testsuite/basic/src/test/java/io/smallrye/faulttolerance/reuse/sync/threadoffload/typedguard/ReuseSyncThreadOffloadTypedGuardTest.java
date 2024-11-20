package io.smallrye.faulttolerance.reuse.sync.threadoffload.typedguard;

import static org.assertj.core.api.Assertions.assertThat;

import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
@AddBeanClasses(MyFaultTolerance.class)
public class ReuseSyncThreadOffloadTypedGuardTest {
    @Test
    public void test(MyService service) {
        Thread currentThread = Thread.currentThread();

        assertThat(service.hello()).isEqualTo("hello");
        assertThat(MyService.CURRENT_THREAD).hasValue(currentThread);
    }
}
