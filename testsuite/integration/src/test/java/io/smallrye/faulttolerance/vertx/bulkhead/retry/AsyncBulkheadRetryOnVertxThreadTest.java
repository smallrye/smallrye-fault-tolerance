package io.smallrye.faulttolerance.vertx.bulkhead.retry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.Condition;
import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.vertx.AbstractVertxTest;

public class AsyncBulkheadRetryOnVertxThreadTest extends AbstractVertxTest {
    @Test
    public void nonblockingBulkhead(MyService myService) {
        CopyOnWriteArrayList<Object> results = new CopyOnWriteArrayList<>();

        runOnVertx(() -> {
            for (int i = 0; i < 20; i++) {
                myService.hello().whenComplete((value, error) -> {
                    results.add(error == null ? value : error);
                });
            }
        });

        // 3 immediate invocations + 3 immediately queued invocations + 6 successfully retried rejections from bulkhead
        // + 8 unsuccessfully retried rejections from bulkhead
        await().atMost(5, TimeUnit.SECONDS).until(() -> results.size() == 20);

        assertThat(results).haveExactly(12,
                new Condition<>("Hello!"::equals, "successful result"));
        assertThat(results).haveExactly(8,
                new Condition<>(it -> it instanceof BulkheadException, "failed result"));

        // 3 immediate invocations + 3 queued invocations + 6 successfully retried rejections from bulkhead
        // 2 identical items for each invocation
        assertThat(MyService.invocationThreads).hasSize(24);
        assertThat(MyService.invocationThreads).allSatisfy(thread -> {
            assertThat(thread).contains("vert.x-eventloop");
        });
        assertThat(MyService.invocationThreads).containsOnly(MyService.invocationThreads.peek());
    }
}
