package io.smallrye.faulttolerance.vertx.retry.requestcontext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.enterprise.context.control.RequestContextController;
import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.vertx.AbstractVertxTest;
import io.smallrye.faulttolerance.vertx.ContextDescription;
import io.smallrye.faulttolerance.vertx.ExecutionStyle;
import io.smallrye.faulttolerance.vertx.VertxContext;

public class AsyncRetryWithRequestContextOnVertxThreadTest extends AbstractVertxTest {
    @Inject
    RequestContextController rcc;

    @BeforeEach
    public void setUp() {
        MyService.currentContexts.clear();
        MyRequestScopedService.instanceIds.clear();
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
        AtomicReference<Object> result = new AtomicReference<>(null);

        runOnVertx(() -> {
            VertxContext.current().duplicate().execute(executionStyle, () -> {
                boolean activated = rcc.activate();
                try {
                    MyService.currentContexts.add(VertxContext.current().describe());
                    myService.hello().whenComplete((value, error) -> {
                        MyService.currentContexts.add(VertxContext.current().describe());
                        result.set(error == null ? value : error);
                    });
                } finally {
                    if (activated) {
                        rcc.deactivate();
                    }
                }
            });
        });

        await().atMost(5, TimeUnit.SECONDS).until(() -> result.get() != null);

        assertThat(result.get()).isEqualTo("Hello!");

        // 1 initial call + 10 retries + 1 before call + 1 after call
        assertThat(MyService.currentContexts).hasSize(13);
        assertThat(MyService.currentContexts).allMatch(it -> executionStyle == it.executionStyle);
        assertThat(MyService.currentContexts).allMatch(ContextDescription::isDuplicatedContext);
        assertThat(new HashSet<>(MyService.currentContexts)).hasSize(1);

        // 1 initial invocation + 10 retries
        assertThat(MyRequestScopedService.instanceIds).hasSize(11);
        assertThat(MyRequestScopedService.instanceIds).containsOnly(MyRequestScopedService.instanceIds.peek());
    }
}
