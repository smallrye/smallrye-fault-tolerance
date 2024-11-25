package io.smallrye.faulttolerance.config.better;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.core.util.barrier.Barrier;
import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;
import io.smallrye.faulttolerance.util.WithSystemProperty;

@FaultToleranceBasicTest
@WithSystemProperty(key = "smallrye.faulttolerance.\"io.smallrye.faulttolerance.config.better.BulkheadConfigBean/value\".bulkhead.value", value = "1")
@WithSystemProperty(key = "smallrye.faulttolerance.\"io.smallrye.faulttolerance.config.better.BulkheadConfigBean/waitingTaskQueue\".bulkhead.waiting-task-queue", value = "1")
public class BulkheadConfigTest {
    @Inject
    private BulkheadConfigBean bean;

    private ExecutorService executor;

    @BeforeEach
    public void setUp() {
        executor = Executors.newCachedThreadPool();
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        executor.shutdownNow();
        executor.awaitTermination(1, TimeUnit.SECONDS);
    }

    @Test
    public void value() throws Exception {
        Barrier barrier = Barrier.interruptible();

        executor.submit(() -> bean.value(barrier));
        Thread.sleep(500);
        assertThatThrownBy(() -> bean.value(null)).isExactlyInstanceOf(BulkheadException.class);

        barrier.open();
    }

    @Test
    public void waitingTaskQueue() throws Exception {
        Barrier barrier1 = Barrier.interruptible();
        Barrier barrier2 = Barrier.interruptible();

        executor.submit(() -> bean.waitingTaskQueue(barrier1));
        executor.submit(() -> bean.waitingTaskQueue(barrier2));
        Thread.sleep(500);
        assertThatThrownBy(() -> bean.waitingTaskQueue(null).get())
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(BulkheadException.class);

        barrier1.open();
        barrier2.open();
    }
}
