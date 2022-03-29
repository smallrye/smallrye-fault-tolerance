package io.smallrye.faulttolerance.reuse.async.completionstage;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.ExecutionException;

import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
@AddBeanClasses(MyFaultTolerance.class)
public class ReuseAsyncCompletionStageTest {
    @Test
    public void test(MyService service) throws ExecutionException, InterruptedException {
        assertThat(service.hello().toCompletableFuture().get()).isEqualTo("fallback");
    }
}
