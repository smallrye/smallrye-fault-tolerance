package io.smallrye.faulttolerance.reuse.config.guard.retry.disable;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.ExecutionException;

import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;
import io.smallrye.faulttolerance.util.WithSystemProperty;

@FaultToleranceBasicTest
@AddBeanClasses(MyFaultTolerance.class)
@WithSystemProperty(key = "smallrye.faulttolerance.\"my-fault-tolerance\".retry.enabled", value = "false")
public class ReuseGuardConfigRetryDisableTest {
    @Test
    public void test(MyService service) throws ExecutionException, InterruptedException {
        assertThat(service.hello()).isEqualTo("hello");
        assertThat(MyService.STRING_COUNTER).hasValue(1);

        assertThat(service.theAnswer().toCompletableFuture().get()).isEqualTo(42);
        assertThat(MyService.INT_COUNTER).hasValue(1);

        assertThat(service.badNumber().subscribeAsCompletionStage().get()).isEqualTo(13L);
        assertThat(MyService.LONG_COUNTER).hasValue(1);
    }
}
