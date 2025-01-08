package io.smallrye.faulttolerance.reuse.config.typedguard.retry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;
import io.smallrye.faulttolerance.util.WithSystemProperty;

@FaultToleranceBasicTest
@AddBeanClasses(MyFaultTolerance.class)
@WithSystemProperty(key = "smallrye.faulttolerance.\"my-fault-tolerance\".retry.max-retries", value = "4")
@WithSystemProperty(key = "smallrye.faulttolerance.\"my-fault-tolerance\".retry.delay", value = "1")
@WithSystemProperty(key = "smallrye.faulttolerance.\"my-fault-tolerance\".retry.delay-unit", value = "seconds")
@WithSystemProperty(key = "smallrye.faulttolerance.\"my-fault-tolerance\".retry.retry-on", value = "java.lang.IllegalStateException")
public class ReuseTypedGuardConfigRetryTest {
    @Test
    public void test(MyService service) {
        long start = System.currentTimeMillis();
        assertThat(service.hello()).isEqualTo("fallback");
        long end = System.currentTimeMillis();
        assertThat(end - start).isCloseTo(4000L, offset(2000L));
        assertThat(MyService.COUNTER).hasValue(5);
    }
}
