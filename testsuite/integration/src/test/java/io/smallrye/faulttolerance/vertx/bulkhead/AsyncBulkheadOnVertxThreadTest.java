package io.smallrye.faulttolerance.vertx.bulkhead;

import static io.smallrye.faulttolerance.util.AssertjUtil.condition;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.vertx.AbstractVertxTest;
import io.smallrye.faulttolerance.vertx.ContextDescription;
import io.smallrye.faulttolerance.vertx.ExecutionStyle;
import io.smallrye.faulttolerance.vertx.VertxContext;

public class AsyncBulkheadOnVertxThreadTest extends AbstractVertxTest {
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
            for (int i = 0; i < 10; i++) {
                ctx.duplicate().execute(executionStyle, () -> {
                    MyService.currentContexts.add(VertxContext.current().describe());
                    myService.hello().whenComplete((value, error) -> {
                        MyService.currentContexts.add(VertxContext.current().describe());
                        results.add(error == null ? value : error);
                    });
                });
            }
        });

        // 3 immediate calls + 3 queued calls + 4 rejected from bulkhead
        await().atMost(5, TimeUnit.SECONDS).until(() -> results.size() == 10);

        assertThat(results).haveExactly(6, condition("Hello!"::equals));
        assertThat(results).haveExactly(4, condition(it -> it instanceof BulkheadException));

        // 3 immediate calls + 3 queued calls: 4 identical items for each
        // 4 rejected calls: 2 identical items for each
        assertThat(MyService.currentContexts).hasSize(32);
        assertThat(MyService.currentContexts).allMatch(it -> it.executionStyle == executionStyle);
        assertThat(MyService.currentContexts).allMatch(ContextDescription::isDuplicatedContext);
        assertThat(new HashSet<>(MyService.currentContexts)).hasSize(10);
    }
}
