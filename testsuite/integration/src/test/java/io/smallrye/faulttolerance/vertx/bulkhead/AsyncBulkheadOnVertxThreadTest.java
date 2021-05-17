package io.smallrye.faulttolerance.vertx.bulkhead;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.Condition;
import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.vertx.AbstractVertxTest;

public class AsyncBulkheadOnVertxThreadTest extends AbstractVertxTest {
    @BeforeEach
    public void setUp() {
        MyService.invocationThreads.clear();
    }

    @Test
    public void nonblockingBulkhead(MyService myService) {
        CopyOnWriteArrayList<Object> results = new CopyOnWriteArrayList<>();

        runOnVertx(() -> {
            for (int i = 0; i < 10; i++) {
                myService.hello().whenComplete((value, error) -> {
                    results.add(error == null ? value : error);
                });
            }
        });

        // 3 immediate invocations + 3 queued invocations + 4 rejected from bulkhead
        await().atMost(5, TimeUnit.SECONDS).until(() -> results.size() == 10);

        assertThat(results).haveExactly(6,
                new Condition<>("Hello!"::equals, "successful result"));
        assertThat(results).haveExactly(4,
                new Condition<>(it -> it instanceof BulkheadException, "failed result"));

        // 3 immediate invocations + 3 queued invocations
        // 2 identical items for each invocation
        assertThat(MyService.invocationThreads).hasSize(12);
        assertThat(MyService.invocationThreads).allSatisfy(thread -> {
            assertThat(thread).contains("vert.x-eventloop");
        });
        assertThat(MyService.invocationThreads).containsOnly(MyService.invocationThreads.peek());
    }
}
