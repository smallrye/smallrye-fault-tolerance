package io.smallrye.faulttolerance.reuse.config.guard.ratelimit.disable;

import static org.assertj.core.api.Assertions.assertThat;

import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;
import io.smallrye.faulttolerance.util.WithSystemProperty;

@FaultToleranceBasicTest
@AddBeanClasses(MyFaultTolerance.class)
@WithSystemProperty(key = "smallrye.faulttolerance.\"my-fault-tolerance\".rate-limit.enabled", value = "false")
public class ReuseGuardConfigRateLimitDisableTest {
    @Test
    public void test(MyService service) throws InterruptedException {
        for (int i = 0; i < 20; i++) {
            assertThat(service.hello()).isEqualTo("hello");
            assertThat(service.helloCompletionStage().toCompletableFuture().join()).isEqualTo("hello");
            assertThat(service.helloUni().subscribeAsCompletionStage().join()).isEqualTo("hello");
        }
    }
}
