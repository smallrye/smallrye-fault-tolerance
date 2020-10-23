package io.smallrye.faulttolerance.async.compstage.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
public class AsynchronousCompletionStageExceptionHandlingTest {
    @Test
    public void test(AsyncHelloService helloService) throws InterruptedException, ExecutionException {
        assertThat(helloService.hello().toCompletableFuture().get()).isEqualTo("hello fallback");
        assertThat(AsyncHelloService.COUNTER.get()).isEqualTo(5);
    }
}
