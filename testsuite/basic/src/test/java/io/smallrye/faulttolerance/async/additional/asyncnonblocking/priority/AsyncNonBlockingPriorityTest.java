package io.smallrye.faulttolerance.async.additional.asyncnonblocking.priority;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletionStage;

import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
public class AsyncNonBlockingPriorityTest {
    @Test
    public void noThreadOffload(AsyncNonBlockingPriorityHelloService service) throws Exception {
        Thread mainThread = Thread.currentThread();

        CompletionStage<String> future = service.hello();
        assertThat(future.toCompletableFuture().get()).isEqualTo("hello");

        assertThat(AsyncNonBlockingPriorityHelloService.helloThread).isSameAs(mainThread);
        assertThat(AsyncNonBlockingPriorityHelloService.helloStackTrace).anySatisfy(frame -> {
            assertThat(frame.getClassName()).contains("io.smallrye.faulttolerance.core");
        });
    }
}
