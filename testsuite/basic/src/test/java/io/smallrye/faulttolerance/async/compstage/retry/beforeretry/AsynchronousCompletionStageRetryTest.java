package io.smallrye.faulttolerance.async.compstage.retry.beforeretry;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
public class AsynchronousCompletionStageRetryTest {
    @Test
    public void testAsyncBeforeRetry(AsyncHelloService helloService) throws InterruptedException, ExecutionException {
        assertThat(helloService.hello().toCompletableFuture().get()).isEqualTo("Fallback");
        assertThat(AsyncHelloService.COUNTER.get()).isEqualTo(3);
        assertThat(AsyncHelloService.BEFORE_RETRY_COUNTER.get()).isEqualTo(2);
    }
}
