package io.smallrye.faulttolerance.vertx.retry.fallback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.vertx.AbstractVertxTest;

public class AsyncRetryFallbackOnVertxThreadTest extends AbstractVertxTest {
    @Test
    public void nonblockingRetryFallback(MyService myService) {
        AtomicReference<Object> result = new AtomicReference<>(null);

        runOnVertx(() -> {
            myService.hello().whenComplete((value, error) -> {
                result.set(error == null ? value : error);
            });
        });

        await().atMost(5, TimeUnit.SECONDS).until(() -> result.get() != null);

        assertThat(result.get()).isEqualTo("Hello fallback!");

        assertThat(MyService.invocationThreads).hasSize(12); // 1 initial invocation + 10 retries + 1 fallback
        assertThat(MyService.invocationThreads).allSatisfy(thread -> {
            assertThat(thread).contains("vert.x-eventloop");
        });
        assertThat(MyService.invocationThreads).containsOnly(MyService.invocationThreads.peek());
    }
}
