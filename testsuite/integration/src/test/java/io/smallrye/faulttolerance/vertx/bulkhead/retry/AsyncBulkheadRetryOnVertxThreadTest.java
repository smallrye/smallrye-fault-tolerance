package io.smallrye.faulttolerance.vertx.bulkhead.retry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.Condition;
import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.vertx.AbstractVertxTest;
import io.smallrye.faulttolerance.vertx.ContextDescription;
import io.smallrye.faulttolerance.vertx.ExecutionStyle;
import io.smallrye.faulttolerance.vertx.VertxContext;

public class AsyncBulkheadRetryOnVertxThreadTest extends AbstractVertxTest {
    @BeforeEach
    public void setUp() {
        MyService.currentContexts.clear();
    }

    @Test
    public void eventLoop(MyService myService) {
        test(myService, ExecutionStyle.EVENT_LOOP);
    }

    @Test
    public void worker(MyService myService) {
        test(myService, ExecutionStyle.WORKER);
    }

    private void test(MyService myService, ExecutionStyle executionStyle) {
        List<Object> results = new CopyOnWriteArrayList<>();

        runOnVertx(() -> {
            VertxContext ctx = VertxContext.current();
            for (int i = 0; i < 20; i++) {
                ctx.duplicate().execute(executionStyle, () -> {
                    MyService.currentContexts.add(VertxContext.current().describe());
                    myService.hello().whenComplete((value, error) -> {
                        MyService.currentContexts.add(VertxContext.current().describe());
                        results.add(error == null ? value : error);
                    });
                });
            }
        });

        // 3 immediate calls + 3 immediately queued calls + 6 successfully retried rejections + 8 unsuccessfully retried rejections
        await().atMost(5, TimeUnit.SECONDS).until(() -> results.size() == 20);

        assertThat(results).haveExactly(12,
                new Condition<>("Hello!"::equals, "successful result"));
        assertThat(results).haveExactly(8,
                new Condition<>(it -> it instanceof BulkheadException, "failed result"));

        // 3 immediate calls + 3 queued calls + 6 successfully retried rejections: 4 identical items for each
        // 8 unsuccessfully retried rejections: 2 identical items for each
        assertThat(MyService.currentContexts).hasSize(64);
        assertThat(MyService.currentContexts).allMatch(it -> executionStyle == it.executionStyle);
        assertThat(MyService.currentContexts).allMatch(ContextDescription::isDuplicatedContext);
        assertThat(new HashSet<>(MyService.currentContexts)).hasSize(20);
    }
}
