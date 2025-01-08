package io.smallrye.faulttolerance.reuse.config.guard.bulkhead.disable;

import static io.smallrye.faulttolerance.core.util.SneakyThrow.sneakyThrow;

import java.util.ArrayList;
import java.util.List;

import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.core.util.barrier.Barrier;
import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;
import io.smallrye.faulttolerance.util.WithSystemProperty;

@FaultToleranceBasicTest
@AddBeanClasses(MyFaultTolerance.class)
@WithSystemProperty(key = "smallrye.faulttolerance.\"my-fault-tolerance\".bulkhead.enabled", value = "false")
public class ReuseGuardConfigBulkheadDisableTest {
    @Test
    public void test(MyService service) throws InterruptedException {
        List<Barrier> barriers = new ArrayList<>();
        List<Barrier> threadStopBarriers = new ArrayList<>();

        for (int i = 0; i < 20; i++) {
            Barrier barrier1 = Barrier.interruptible();
            Barrier barrier2 = Barrier.interruptible();
            Barrier barrier3 = Barrier.interruptible();
            Barrier threadStop = Barrier.interruptible();

            new Thread(() -> {
                try {
                    service.hello(barrier1);
                } catch (InterruptedException e) {
                    throw sneakyThrow(e);
                } finally {
                    threadStop.open();
                }
            }).start();
            service.helloCompletionStage(barrier2);
            service.helloUni(barrier3).subscribeAsCompletionStage();

            barriers.add(barrier1);
            barriers.add(barrier2);
            barriers.add(barrier3);
            threadStopBarriers.add(threadStop);
        }

        for (Barrier barrier : barriers) {
            barrier.open();
        }
        for (Barrier barrier : threadStopBarriers) {
            barrier.await();
        }
    }
}
