package io.smallrye.faulttolerance.vertx.timeout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.Condition;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.vertx.AbstractVertxTest;
import io.smallrye.faulttolerance.vertx.ContextDescription;
import io.smallrye.faulttolerance.vertx.ExecutionStyle;
import io.smallrye.faulttolerance.vertx.VertxContext;

public class AsyncTimeoutOnVertxThreadTest extends AbstractVertxTest {
    @BeforeEach
    public void setUp() {
        MyService.currentContexts.clear();
    }

    @Test
    public void timeout_eventLoop(MyService service) {
        timeout(service, ExecutionStyle.EVENT_LOOP);
    }

    @Test
    public void timeout_worker(MyService service) {
        timeout(service, ExecutionStyle.WORKER);
    }

    private void timeout(MyService service, ExecutionStyle executionStyle) {
        List<Object> results = new CopyOnWriteArrayList<>();

        runOnVertx(() -> {
            VertxContext ctx = VertxContext.current();
            for (int i = 0; i < 10; i++) {
                ctx.duplicate().execute(executionStyle, () -> {
                    MyService.currentContexts.add(VertxContext.current().describe());
                    service.hello(5000).whenComplete((value, error) -> {
                        MyService.currentContexts.add(VertxContext.current().describe());
                        results.add(error == null ? value : error);
                    });
                });
            }
        });

        // 10 calls
        await().atMost(5, TimeUnit.SECONDS).until(() -> results.size() == 10);

        assertThat(results).haveExactly(10,
                new Condition<>(it -> it instanceof TimeoutException, "failed result"));

        // 10 calls, for each of them: 1 before sleep + 1 before call + 1 after call
        assertThat(MyService.currentContexts).hasSize(30);
        assertThat(MyService.currentContexts).allMatch(it -> executionStyle == it.executionStyle);
        assertThat(MyService.currentContexts).allMatch(ContextDescription::isDuplicatedContext);
        assertThat(new HashSet<>(MyService.currentContexts)).hasSize(10);
    }

    @Test
    public void noTimeout_eventLoop(MyService service) {
        noTimeout(service, ExecutionStyle.EVENT_LOOP);
    }

    @Test
    public void noTimeout_worker(MyService service) {
        noTimeout(service, ExecutionStyle.WORKER);
    }

    private void noTimeout(MyService service, ExecutionStyle executionStyle) {
        List<Object> results = new CopyOnWriteArrayList<>();

        runOnVertx(() -> {
            VertxContext ctx = VertxContext.current();
            for (int i = 0; i < 10; i++) {
                ctx.duplicate().execute(executionStyle, () -> {
                    MyService.currentContexts.add(VertxContext.current().describe());
                    service.hello(50).whenComplete((value, error) -> {
                        MyService.currentContexts.add(VertxContext.current().describe());
                        results.add(error == null ? value : error);
                    });
                });
            }
        });

        await().atMost(5, TimeUnit.SECONDS).until(() -> results.size() == 10);

        assertThat(results).haveExactly(10,
                new Condition<>("Hello!"::equals, "successful result"));

        // 10 calls, for each of them: 1 before sleep + 1 after sleep + 1 before call + 1 after call
        assertThat(MyService.currentContexts).hasSize(40);
        assertThat(MyService.currentContexts).allMatch(it -> executionStyle == it.executionStyle);
        assertThat(MyService.currentContexts).allMatch(ContextDescription::isDuplicatedContext);
        assertThat(new HashSet<>(MyService.currentContexts)).hasSize(10);
    }
}
