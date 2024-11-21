package io.smallrye.faulttolerance.reuse.mixed.async.both.fallback;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.ExecutionException;

import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
@AddBeanClasses(MyFaultTolerance.class)
public class MixedReuseAsyncBothFallbackTest {
    @Test
    public void test(MyService service) throws ExecutionException, InterruptedException {
        assertThat(service.hello().toCompletableFuture().get()).isEqualTo("hello");
        assertThat(MyService.STRING_COUNTER).hasValue(3);

        assertThat(service.theAnswer().subscribeAsCompletionStage().get()).isEqualTo(42);
        assertThat(MyService.INT_COUNTER).hasValue(3);
    }
}
