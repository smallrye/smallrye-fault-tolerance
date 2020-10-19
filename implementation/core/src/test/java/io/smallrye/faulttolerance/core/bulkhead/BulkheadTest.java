package io.smallrye.faulttolerance.core.bulkhead;

import static io.smallrye.faulttolerance.core.util.TestThread.runOnTestThread;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.junit.Test;

import io.smallrye.faulttolerance.core.InvocationContext;
import io.smallrye.faulttolerance.core.util.TestThread;
import io.smallrye.faulttolerance.core.util.barrier.Barrier;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public class BulkheadTest {
    @Test
    public void shouldLetSingleThrough() throws Exception {
        TestInvocation<String> invocation = TestInvocation.immediatelyReturning(() -> "shouldLetSingleThrough");
        SemaphoreBulkhead<String> bulkhead = new SemaphoreBulkhead<>(invocation, "shouldLetSingleThrough", 2);
        String result = bulkhead.apply(new InvocationContext<>(() -> "ignored"));
        assertThat(result).isEqualTo("shouldLetSingleThrough");
    }

    @Test
    public void shouldLetMaxThrough() throws Exception {
        Barrier delayBarrier = Barrier.noninterruptible();
        TestInvocation<String> invocation = TestInvocation.delayed(delayBarrier, () -> "shouldLetMaxThrough");
        SemaphoreBulkhead<String> bulkhead = new SemaphoreBulkhead<>(invocation, "shouldLetMaxThrough", 5);

        List<TestThread<String>> threads = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            threads.add(runOnTestThread(bulkhead));
        }
        delayBarrier.open();
        for (int i = 0; i < 5; i++) {
            assertThat(threads.get(i).await()).isEqualTo("shouldLetMaxThrough");
        }
    }

    @Test
    public void shouldRejectMaxPlus1() throws Exception {
        Barrier delayBarrier = Barrier.noninterruptible();
        CountDownLatch startedLatch = new CountDownLatch(5);
        TestInvocation<String> invocation = TestInvocation.immediatelyReturning(() -> {
            startedLatch.countDown();
            delayBarrier.await();
            return "shouldRejectMaxPlus1";
        });
        SemaphoreBulkhead<String> bulkhead = new SemaphoreBulkhead<>(invocation, "shouldRejectMaxPlus1", 5);

        List<TestThread<String>> threads = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            threads.add(runOnTestThread(bulkhead));
        }
        startedLatch.await(); // makes sure that all the threads have put their runnables into bulkhead
        assertThatThrownBy(() -> bulkhead.apply(new InvocationContext<>(() -> "")))
                .isExactlyInstanceOf(BulkheadException.class);
        delayBarrier.open();

        for (int i = 0; i < 5; i++) {
            assertThat(threads.get(i).await()).isEqualTo("shouldRejectMaxPlus1");
        }
    }

    @Test
    public void shouldLetMaxPlus1After1Left() throws Exception {
        Barrier delayBarrier = Barrier.noninterruptible();
        Semaphore letOneInSemaphore = new Semaphore(1);
        Semaphore finishedThreadsCount = new Semaphore(0);
        List<TestThread<String>> finishedThreads = new Vector<>();

        TestInvocation<String> invocation = TestInvocation.delayed(delayBarrier, () -> {
            letOneInSemaphore.acquire();
            // noinspection unchecked
            finishedThreads.add((TestThread<String>) Thread.currentThread());
            finishedThreadsCount.release();
            return "shouldLetMaxPlus1After1Left";
        });

        SemaphoreBulkhead<String> bulkhead = new SemaphoreBulkhead<>(invocation, "shouldLetMaxPlus1After1Left", 5);

        for (int i = 0; i < 5; i++) {
            runOnTestThread(bulkhead);
        }

        delayBarrier.open();
        finishedThreadsCount.acquire();
        assertThat(finishedThreads.get(0).await()).isEqualTo("shouldLetMaxPlus1After1Left");

        runOnTestThread(bulkhead);

        letOneInSemaphore.release(5);
        for (int i = 1; i < 6; i++) {
            finishedThreadsCount.acquire();
            assertThat(finishedThreads.get(i).await()).isEqualTo("shouldLetMaxPlus1After1Left");
        }
    }

    @Test
    public void shouldLetMaxPlus1After1Failed() throws Exception {
        RuntimeException error = new RuntimeException("forced");

        Barrier delayBarrier = Barrier.noninterruptible();
        Semaphore letOneInSemaphore = new Semaphore(1);
        Semaphore finishedThreadsCount = new Semaphore(0);
        List<TestThread<String>> finishedThreads = new Vector<>();

        TestInvocation<String> invocation = TestInvocation.delayed(delayBarrier, () -> {
            letOneInSemaphore.acquire();
            //noinspection unchecked
            finishedThreads.add((TestThread<String>) Thread.currentThread());
            finishedThreadsCount.release();
            throw error;
        });

        SemaphoreBulkhead<String> bulkhead = new SemaphoreBulkhead<>(invocation, "shouldLetMaxPlus1After1Left", 5);

        for (int i = 0; i < 5; i++) {
            runOnTestThread(bulkhead);
        }

        delayBarrier.open();
        finishedThreadsCount.acquire();
        assertThatThrownBy(finishedThreads.get(0)::await).isEqualTo(error);

        runOnTestThread(bulkhead);

        letOneInSemaphore.release(5);
        for (int i = 1; i < 6; i++) {
            finishedThreadsCount.acquire();
            assertThatThrownBy(finishedThreads.get(0)::await).isEqualTo(error);
        }
    }
}
