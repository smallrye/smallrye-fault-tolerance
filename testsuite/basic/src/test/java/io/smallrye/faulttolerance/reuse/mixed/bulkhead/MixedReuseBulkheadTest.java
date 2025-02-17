package io.smallrye.faulttolerance.reuse.mixed.bulkhead;

import static io.smallrye.faulttolerance.core.util.SneakyThrow.sneakyThrow;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.core.util.barrier.Barrier;
import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
@AddBeanClasses(MyFaultTolerance.class)
public class MixedReuseBulkheadTest {
    @Test
    public void test(MyService service) throws ExecutionException, InterruptedException {
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
        service.theAnswer(barrier2);
        service.badNumber(barrier3).subscribeAsCompletionStage();

        Thread.sleep(500);

        // queued
        service.theAnswer(barrier4);
        service.badNumber(barrier5).subscribeAsCompletionStage();

        // rejected
        assertThatThrownBy(() -> service.hello(Barrier.interruptible())).isExactlyInstanceOf(BulkheadException.class);
        assertThatThrownBy(() -> service.theAnswer(Barrier.interruptible()).toCompletableFuture().join())
                .isExactlyInstanceOf(CompletionException.class)
                .hasCauseExactlyInstanceOf(BulkheadException.class);
        assertThatThrownBy(() -> service.badNumber(Barrier.interruptible()).subscribeAsCompletionStage().join())
                .isExactlyInstanceOf(CompletionException.class)
                .hasCauseExactlyInstanceOf(BulkheadException.class);

        barrier1.open();
        barrier2.open();
        barrier3.open();
        barrier4.open();
        barrier5.open();
    }
}
