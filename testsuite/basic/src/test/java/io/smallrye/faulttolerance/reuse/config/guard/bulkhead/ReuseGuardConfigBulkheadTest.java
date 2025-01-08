package io.smallrye.faulttolerance.reuse.config.guard.bulkhead;

import static io.smallrye.faulttolerance.core.util.SneakyThrow.sneakyThrow;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.CompletionException;

import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.core.util.barrier.Barrier;
import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;
import io.smallrye.faulttolerance.util.WithSystemProperty;

@FaultToleranceBasicTest
@AddBeanClasses(MyFaultTolerance.class)
@WithSystemProperty(key = "smallrye.faulttolerance.\"my-fault-tolerance\".bulkhead.value", value = "3")
@WithSystemProperty(key = "smallrye.faulttolerance.\"my-fault-tolerance\".bulkhead.waiting-task-queue", value = "2")
public class ReuseGuardConfigBulkheadTest {
    @Test
    public void test(MyService service) throws InterruptedException {
        Barrier threadStart = Barrier.interruptible();
        Barrier barrier1 = Barrier.interruptible();
        Barrier barrier2 = Barrier.interruptible();
        Barrier barrier3 = Barrier.interruptible();
        Barrier barrier4 = Barrier.interruptible();
        Barrier barrier5 = Barrier.interruptible();

        // accepted
        new Thread(() -> {
            threadStart.open();
            try {
                service.hello(barrier1);
            } catch (InterruptedException e) {
                throw sneakyThrow(e);
            }
        }).start();
        threadStart.await();
        service.helloCompletionStage(barrier2);
        service.helloUni(barrier3).subscribeAsCompletionStage();

        Thread.sleep(500);

        // queued
        service.helloCompletionStage(barrier4);
        service.helloUni(barrier5).subscribeAsCompletionStage();

        assertThatThrownBy(() -> service.hello(Barrier.interruptible())).isExactlyInstanceOf(BulkheadException.class);
        assertThatThrownBy(service.helloCompletionStage(Barrier.interruptible()).toCompletableFuture()::join)
                .isExactlyInstanceOf(CompletionException.class)
                .hasCauseExactlyInstanceOf(BulkheadException.class);
        assertThatThrownBy(service.helloUni(Barrier.interruptible()).subscribeAsCompletionStage()::join)
                .isExactlyInstanceOf(CompletionException.class)
                .hasCauseExactlyInstanceOf(BulkheadException.class);

        barrier1.open();
        barrier2.open();
        barrier3.open();
        barrier4.open();
        barrier5.open();
    }
}
