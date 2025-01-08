package io.smallrye.faulttolerance.reuse.config.guard.retry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import java.util.concurrent.ExecutionException;

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
public class ReuseGuardConfigRetryTest {
    @Test
    public void test(MyService service) throws ExecutionException, InterruptedException {
        long start = System.currentTimeMillis();
        assertThat(service.hello()).isEqualTo("hello");
        long end = System.currentTimeMillis();
        assertThat(end - start).isCloseTo(4000L, offset(2000L));
        assertThat(MyService.STRING_COUNTER).hasValue(5);

        start = System.currentTimeMillis();
        assertThat(service.theAnswer().toCompletableFuture().get()).isEqualTo(42);
        end = System.currentTimeMillis();
        assertThat(end - start).isCloseTo(0L, offset(2000L));
        assertThat(MyService.INT_COUNTER).hasValue(1);

        start = System.currentTimeMillis();
        assertThat(service.badNumber().subscribeAsCompletionStage().get()).isEqualTo(13L);
        end = System.currentTimeMillis();
        assertThat(end - start).isCloseTo(0L, offset(2000L));
        assertThat(MyService.LONG_COUNTER).hasValue(1);
    }
}
