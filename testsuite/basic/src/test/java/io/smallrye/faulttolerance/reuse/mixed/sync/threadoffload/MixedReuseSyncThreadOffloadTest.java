package io.smallrye.faulttolerance.reuse.mixed.sync.threadoffload;

import static org.assertj.core.api.Assertions.assertThat;

import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
@AddBeanClasses(MyFaultTolerance.class)
public class MixedReuseSyncThreadOffloadTest {
    @Test
    public void test(MyService service) {
        Thread currentThread = Thread.currentThread();

        assertThat(service.hello()).isEqualTo("hello");
        assertThat(MyService.CURRENT_THREAD_STRING).hasValue(currentThread);

        assertThat(service.theAnswer()).isEqualTo(42);
        assertThat(MyService.CURRENT_THREAD_INT).hasValue(currentThread);
    }
}
