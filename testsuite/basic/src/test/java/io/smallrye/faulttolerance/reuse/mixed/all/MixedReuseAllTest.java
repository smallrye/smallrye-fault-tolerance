package io.smallrye.faulttolerance.reuse.mixed.all;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.ExecutionException;

import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
@AddBeanClasses(MyFaultTolerance.class)
public class MixedReuseAllTest {
    @Test
    public void test(MyService service) throws ExecutionException, InterruptedException {
        assertThat(service.hello()).isEqualTo("hello");
        assertThat(MyService.STRING_COUNTER).hasValue(4);

        assertThat(service.theAnswer().toCompletableFuture().get()).isEqualTo(42);
        assertThat(MyService.INT_COUNTER).hasValue(4);

        assertThat(service.badNumber().subscribeAsCompletionStage().toCompletableFuture().get()).isEqualTo(13L);
        assertThat(MyService.LONG_COUNTER).hasValue(4);
    }
}
