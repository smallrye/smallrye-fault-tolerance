package io.smallrye.faulttolerance.reuse.async.uni.fallback.typedguard;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
@AddBeanClasses(MyFaultTolerance.class)
public class ReuseAsyncUniFallbackTypedGuardTest {
    @Test
    public void test(MyService service) {
        assertThat(service.hello().await().atMost(Duration.ofSeconds(1))).isEqualTo("better fallback");
        assertThat(MyService.COUNTER).hasValue(3);
    }
}
