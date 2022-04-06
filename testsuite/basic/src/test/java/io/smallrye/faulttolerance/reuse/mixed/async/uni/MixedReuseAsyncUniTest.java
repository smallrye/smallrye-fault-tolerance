package io.smallrye.faulttolerance.reuse.mixed.async.uni;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.ExecutionException;

import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
@AddBeanClasses(MyFaultTolerance.class)
public class MixedReuseAsyncUniTest {
    @Test
    public void test(MyService service) throws ExecutionException, InterruptedException {
        assertThat(service.hello().await().indefinitely()).isEqualTo("hello");
        assertThat(MyService.STRING_COUNTER).hasValue(4);

        assertThat(service.theAnswer().await().indefinitely()).isEqualTo(42);
        assertThat(MyService.INT_COUNTER).hasValue(4);
    }
}
