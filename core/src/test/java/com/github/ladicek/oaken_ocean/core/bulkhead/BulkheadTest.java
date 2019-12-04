package com.github.ladicek.oaken_ocean.core.bulkhead;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Semaphore;

import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.junit.Test;

import com.github.ladicek.oaken_ocean.core.SimpleInvocationContext;
import com.github.ladicek.oaken_ocean.core.util.TestThread;
import com.github.ladicek.oaken_ocean.core.util.barrier.Barrier;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public class BulkheadTest {
    @Test
    public void shouldLetSingleThrough() throws Exception {
        TestInvocation<String> invocation = TestInvocation.immediatelyReturning(() -> "shouldLetSingleThrough");
        SyncBulkhead<String> bulkhead = new SyncBulkhead<>(invocation, "shouldLetSingleThrough", 2, null);
        String result = bulkhead.apply(null);
        assertThat(result).isEqualTo("shouldLetSingleThrough");
    }

    @Test
    public void shouldLetMaxThrough() throws Exception {
        Barrier delayBarrier = Barrier.noninterruptible();
        TestInvocation<String> invocation = TestInvocation.delayed(delayBarrier, () -> "shouldLetMaxThrough");
        SyncBulkhead<String> bulkhead = new SyncBulkhead<>(invocation, "shouldLetMaxThrough", 5, null);

        List<TestThread<String>> threads = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            threads.add(TestThread.runOnTestThread(bulkhead));
        }
        delayBarrier.open();
        for (int i = 0; i < 5; i++) {
            assertThat(threads.get(i).await()).isEqualTo("shouldLetMaxThrough");
        }
    }

    @Test
    public void shouldRejectMaxPlus1() throws Exception {
        Barrier startBarrier = Barrier.noninterruptible();
        Barrier delayBarrier = Barrier.noninterruptible();
        TestInvocation<String> invocation = TestInvocation.delayed(startBarrier, delayBarrier, () -> "shouldRejectMaxPlus1");
        SyncBulkhead<String> bulkhead = new SyncBulkhead<>(invocation, "shouldRejectMaxPlus1", 5, null);

        List<TestThread<String>> threads = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            threads.add(TestThread.runOnTestThread(bulkhead));
        }
        startBarrier.await();
        assertThatThrownBy(() -> bulkhead.apply(new SimpleInvocationContext<>(() -> "")))
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
            finishedThreads.add((TestThread<String>) Thread.currentThread());
            finishedThreadsCount.release();
            return "shouldLetMaxPlus1After1Left";
        });

        SyncBulkhead<String> bulkhead = new SyncBulkhead<>(invocation, "shouldLetMaxPlus1After1Left", 5, null);

        List<TestThread<String>> threads = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            threads.add(TestThread.runOnTestThread(bulkhead));
        }

        delayBarrier.open();
        finishedThreadsCount.acquire();
        assertThat(finishedThreads.get(0).await()).isEqualTo("shouldLetMaxPlus1After1Left");

        threads.add(TestThread.runOnTestThread(bulkhead));

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
            finishedThreads.add((TestThread<String>) Thread.currentThread());
            finishedThreadsCount.release();
            throw error;
        });

        SyncBulkhead<String> bulkhead = new SyncBulkhead<>(invocation, "shouldLetMaxPlus1After1Left", 5, null);

        List<TestThread<String>> threads = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            threads.add(TestThread.runOnTestThread(bulkhead));
        }

        delayBarrier.open();
        finishedThreadsCount.acquire();
        assertThatThrownBy(finishedThreads.get(0)::await).isEqualTo(error);

        threads.add(TestThread.runOnTestThread(bulkhead));

        letOneInSemaphore.release(5);
        for (int i = 1; i < 6; i++) {
            finishedThreadsCount.acquire();
            assertThatThrownBy(finishedThreads.get(0)::await).isEqualTo(error);
        }
    }

}
