package io.smallrye.faulttolerance.vertx;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import io.smallrye.faulttolerance.util.FaultToleranceIntegrationTest;
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
        vertx.close();
    }

    protected final void runOnVertx(Runnable runnable) {
        vertx.runOnContext(ignored -> {
            runnable.run();
        });
    }
}
