package io.smallrye.faulttolerance.async.additional.nonblocking.retry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
public class RetryNonblockingAsyncTest {
    @Test
    public void noThreadOffloadAndRetry(RetryNonblockingHelloService service) {
        Thread mainThread = Thread.currentThread();

        CompletionStage<String> future = service.hello();
        assertThatThrownBy(() -> future.toCompletableFuture().get())
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(RuntimeException.class);

        // no delay between retries, all executions happen on the same thread
        // if there _was_ a delay, subsequent retries would be offloaded to another thread
        assertThat(service.getHelloThreads()).allSatisfy(thread -> {
            assertThat(thread).isSameAs(mainThread);
        });
        assertThat(service.getHelloStackTraces()).allSatisfy(stackTrace -> {
            assertThat(stackTrace).anySatisfy(frame -> {
                assertThat(frame.getClassName()).contains("io.smallrye.faulttolerance.core");
            });
        });

        // 1 initial execution + 3 retries
        assertThat(service.getInvocationCounter()).hasValue(4);
    }
}
