package io.smallrye.faulttolerance.async.sizing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;

import org.junit.jupiter.api.Test;

public abstract class AbstractAsyncThreadPoolSizingTest {
    static final int SIZE = 10;

    @Test
    public void testAsyncThreadPoolSizing(HelloService helloService) throws InterruptedException {
        CountDownLatch startLatch = new CountDownLatch(SIZE);
        CountDownLatch endLatch = new CountDownLatch(1);

        List<CompletionStage<String>> futures = new ArrayList<>();

        for (int i = 0; i < 2 * SIZE; i++) {
            CompletionStage<String> future = helloService.hello(startLatch, endLatch);
            futures.add(future);
        }

        startLatch.await();
        endLatch.countDown();

        int ok = 0;
        int error = 0;
        for (CompletionStage<String> future : futures) {
            try {
                assertThat(future.toCompletableFuture().get()).isEqualTo("hello");
                ok++;
            } catch (ExecutionException e) {
                assertThat(e).hasCauseExactlyInstanceOf(RejectedExecutionException.class);
                error++;
            }
        }

        assertThat(ok).isEqualTo(SIZE);
        assertThat(error).isEqualTo(SIZE);
    }
}
