package io.smallrye.faulttolerance.vertx.retry.requestcontext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.enterprise.context.control.RequestContextController;
import javax.inject.Inject;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.vertx.AbstractVertxTest;

public class AsyncRetryWithRequestContextOnVertxThreadTest extends AbstractVertxTest {
    @Inject
    RequestContextController rcc;

    @BeforeEach
    public void setUp() {
        MyService.invocationThreads.clear();
        MyRequestScopedService.instanceIds.clear();
    }

    @Test
    public void nonblockingRetryWithRequestContext(MyService myService) {
        Assertions.setMaxStackTraceElementsDisplayed(100);

        AtomicReference<Object> result = new AtomicReference<>(null);

        runOnVertx(() -> {
            boolean activated = rcc.activate();
            try {
                myService.hello().whenComplete((value, error) -> {
                    result.set(error == null ? value : error);
                });
            } finally {
                if (activated) {
                    rcc.deactivate();
                }
            }
        });

        await().atMost(5, TimeUnit.SECONDS).until(() -> result.get() != null);

        assertThat(result.get()).isEqualTo("Hello!");

        assertThat(MyService.invocationThreads).hasSize(11); // 1 initial invocation + 10 retries
        assertThat(MyService.invocationThreads).allSatisfy(thread -> {
            assertThat(thread).contains("vert.x-eventloop");
        });
        assertThat(MyService.invocationThreads).containsOnly(MyService.invocationThreads.peek());

        assertThat(MyRequestScopedService.instanceIds).hasSize(11);
        assertThat(MyRequestScopedService.instanceIds).containsOnly(MyRequestScopedService.instanceIds.peek());
    }
}
