package io.smallrye.faulttolerance.reuse.config.guard.timeout.disable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;
import io.smallrye.faulttolerance.util.WithSystemProperty;

@FaultToleranceBasicTest
@AddBeanClasses(MyFaultTolerance.class)
@WithSystemProperty(key = "smallrye.faulttolerance.\"my-fault-tolerance\".timeout.enabled", value = "false")
public class ReuseGuardConfigTimeoutDisableTest {
    @Test
    public void test(MyService service) throws InterruptedException {
        long start = System.currentTimeMillis();
        assertThat(service.hello()).isEqualTo("hello");
        long end = System.currentTimeMillis();
        assertThat(end - start).isCloseTo(3000L, offset(2000L));

        start = System.currentTimeMillis();
        assertThat(service.helloCompletionStage().toCompletableFuture().join()).isEqualTo("hello");
        end = System.currentTimeMillis();
        assertThat(end - start).isCloseTo(3000L, offset(2000L));

        start = System.currentTimeMillis();
        assertThat(service.helloUni().subscribeAsCompletionStage().join()).isEqualTo("hello");
        end = System.currentTimeMillis();
        assertThat(end - start).isCloseTo(3000L, offset(2000L));
    }
}
