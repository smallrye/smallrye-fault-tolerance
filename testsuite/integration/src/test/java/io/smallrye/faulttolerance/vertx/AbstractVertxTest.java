package io.smallrye.faulttolerance.vertx;

import static org.awaitility.Awaitility.await;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import io.smallrye.faulttolerance.util.FaultToleranceIntegrationTest;
import io.vertx.core.Future;
import io.vertx.core.Vertx;

@FaultToleranceIntegrationTest
public abstract class AbstractVertxTest {
    private Vertx vertx;

    @BeforeEach
    public final void createVertx() {
        vertx = Vertx.vertx();
    }

    @AfterEach
    public final void destroyVertx() {
        Future<Void> future = vertx.close();

        await().atMost(5, TimeUnit.SECONDS).until(future::isComplete);
    }

    protected final void runOnVertx(Runnable runnable) {
        vertx.runOnContext(ignored -> {
            runnable.run();
        });
    }
}
