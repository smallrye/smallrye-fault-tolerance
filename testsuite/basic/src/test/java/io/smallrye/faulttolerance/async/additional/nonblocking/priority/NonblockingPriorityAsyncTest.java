package io.smallrye.faulttolerance.async.additional.nonblocking.priority;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletionStage;

import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
public class NonblockingPriorityAsyncTest {
    @Test
    public void noThreadOffload(NonblockingPriorityHelloService service) throws Exception {
        Thread mainThread = Thread.currentThread();

        CompletionStage<String> future = service.hello();
        assertThat(future.toCompletableFuture().get()).isEqualTo("hello");

        assertThat(service.getHelloThread()).isSameAs(mainThread);
        assertThat(service.getHelloStackTrace()).anySatisfy(frame -> {
            assertThat(frame.getClassName()).contains("io.smallrye.faulttolerance.core");
        });
    }
}
