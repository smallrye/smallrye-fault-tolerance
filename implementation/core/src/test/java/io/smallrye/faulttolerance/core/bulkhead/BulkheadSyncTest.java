package io.smallrye.faulttolerance.core.bulkhead;

import static io.smallrye.faulttolerance.core.FaultToleranceContextUtil.sync;
import static io.smallrye.faulttolerance.core.util.TestThread.runOnTestThread;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Semaphore;

import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.core.Future;
import io.smallrye.faulttolerance.core.util.TestInvocation;
import io.smallrye.faulttolerance.core.util.TestThread;
import io.smallrye.faulttolerance.core.util.barrier.Barrier;
import io.smallrye.faulttolerance.core.util.party.Party;

public class BulkheadSyncTest {
    @Test
    public void shouldLetOneIn() throws Throwable {
        TestInvocation<String> invocation = TestInvocation.of(() -> "shouldLetSingleThrough");
        Bulkhead<String> bulkhead = new Bulkhead<>(invocation, "shouldLetSingleThrough", 2, 0, false);
        Future<String> result = bulkhead.apply(sync(null));
        assertThat(result.awaitBlocking()).isEqualTo("shouldLetSingleThrough");
    }

    @Test
    public void shouldLetMaxIn() throws Exception {
        Barrier delayBarrier = Barrier.noninterruptible();
        TestInvocation<String> invocation = TestInvocation.of(() -> {
            delayBarrier.await();
            return "shouldLetMaxThrough";
        });
        Bulkhead<String> bulkhead = new Bulkhead<>(invocation, "shouldLetMaxThrough", 5, 0, false);

        List<TestThread<String>> threads = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            threads.add(runOnTestThread(bulkhead, false));
        }
        delayBarrier.open();
        for (int i = 0; i < 5; i++) {
            assertThat(threads.get(i).await()).isEqualTo("shouldLetMaxThrough");
        }
    }

    @Test
    public void shouldRejectMaxPlus1() throws Exception {
        int size = 5;

        Party party = Party.create(size);
        TestInvocation<String> invocation = TestInvocation.of(() -> {
            party.participant().attend();
            return "shouldRejectMaxPlus1";
        });
        Bulkhead<String> bulkhead = new Bulkhead<>(invocation, "shouldRejectMaxPlus1", size, 0, false);

        List<TestThread<String>> threads = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            threads.add(runOnTestThread(bulkhead, false));
        }

        party.organizer().waitForAll(); // make sure all threads have entered the bulkhead
        assertThatThrownBy(bulkhead.apply(sync(null))::awaitBlocking)
                .isExactlyInstanceOf(BulkheadException.class);
        party.organizer().disband();

        for (int i = 0; i < size; i++) {
            assertThat(threads.get(i).await()).isEqualTo("shouldRejectMaxPlus1");
        }
    }

    @Test
    public void shouldLetMaxPlus1After1Left() throws Exception {
        Barrier delayBarrier = Barrier.noninterruptible();
        Semaphore letOneInSemaphore = new Semaphore(1);
        Semaphore finishedThreadsCount = new Semaphore(0);
        List<TestThread<String>> finishedThreads = new Vector<>();

        TestInvocation<String> invocation = TestInvocation.of(() -> {
            delayBarrier.await();
            letOneInSemaphore.acquire();
            // noinspection unchecked
            finishedThreads.add((TestThread<String>) Thread.currentThread());
            finishedThreadsCount.release();
            return "shouldLetMaxPlus1After1Left";
        });

        Bulkhead<String> bulkhead = new Bulkhead<>(invocation, "shouldLetMaxPlus1After1Left", 5, 0, false);

        for (int i = 0; i < 5; i++) {
            runOnTestThread(bulkhead, false);
        }

        delayBarrier.open();
        finishedThreadsCount.acquire();
        assertThat(finishedThreads.get(0).await()).isEqualTo("shouldLetMaxPlus1After1Left");

        runOnTestThread(bulkhead, false);

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

        TestInvocation<String> invocation = TestInvocation.of(() -> {
            delayBarrier.await();
            letOneInSemaphore.acquire();
            //noinspection unchecked
            finishedThreads.add((TestThread<String>) Thread.currentThread());
            finishedThreadsCount.release();
            throw error;
        });

        Bulkhead<String> bulkhead = new Bulkhead<>(invocation, "shouldLetMaxPlus1After1Left", 5, 0, false);

        for (int i = 0; i < 5; i++) {
            runOnTestThread(bulkhead, false);
        }

        delayBarrier.open();
        finishedThreadsCount.acquire();
        assertThatThrownBy(finishedThreads.get(0)::await).isEqualTo(error);

        runOnTestThread(bulkhead, false);

        letOneInSemaphore.release(5);
        for (int i = 1; i < 6; i++) {
            finishedThreadsCount.acquire();
            assertThatThrownBy(finishedThreads.get(0)::await).isEqualTo(error);
        }
    }
}
