package io.smallrye.faulttolerance.vertx.retry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.assertj.core.api.Condition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.vertx.AbstractVertxTest;
import io.smallrye.faulttolerance.vertx.ContextDescription;
import io.smallrye.faulttolerance.vertx.ExecutionStyle;
import io.smallrye.faulttolerance.vertx.VertxContext;

public class AsyncRetryOnVertxThreadTest extends AbstractVertxTest {
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
                    myService.hello(new AtomicInteger(0)).whenComplete((value, error) -> {
                        MyService.currentContexts.add(VertxContext.current().describe());
                        results.add(error == null ? value : error);
                    });
                });
            }
        });

        // 10 calls
        await().atMost(5, TimeUnit.SECONDS).until(() -> results.size() == 10);

        assertThat(results).haveExactly(10,
                new Condition<>("Hello!"::equals, "successful result"));

        // 10 calls, for each of them: 1 initial call + 10 retries + 1 before call + 1 after call
        assertThat(MyService.currentContexts).hasSize(130);
        assertThat(MyService.currentContexts).allMatch(it -> executionStyle == it.executionStyle);
        assertThat(MyService.currentContexts).allMatch(ContextDescription::isDuplicatedContext);
        assertThat(new HashSet<>(MyService.currentContexts)).hasSize(10);
    }
}
