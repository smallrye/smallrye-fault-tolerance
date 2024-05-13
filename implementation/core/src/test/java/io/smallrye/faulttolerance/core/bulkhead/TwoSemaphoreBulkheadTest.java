package io.smallrye.faulttolerance.core.bulkhead;

import io.smallrye.faulttolerance.core.InvocationContext;
import io.smallrye.faulttolerance.core.async.FutureCancellationEvent;
import io.smallrye.faulttolerance.core.util.TestInvocation;
import io.smallrye.faulttolerance.core.util.TestThread;
import io.smallrye.faulttolerance.core.util.barrier.Barrier;
import io.smallrye.faulttolerance.core.util.party.Party;
import org.awaitility.Awaitility;
import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static io.smallrye.faulttolerance.core.util.TestThread.runOnTestThread;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

/**
 @see TwoSemaphoreBulkhead */
class TwoSemaphoreBulkheadTest {
    private ExecutorService executor;

    @BeforeEach
    public void setUp() {
        executor = Executors.newCachedThreadPool();// ~ virtual threads executor
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        executor.shutdownNow();
        assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));
    }

    @Test
    public void shouldLetOneIn() throws Exception {
        TestInvocation<String> invocation = TestInvocation.of(() -> "shouldLetSingleThrough");
        TwoSemaphoreBulkhead<String> bulkhead = new TwoSemaphoreBulkhead<>(invocation, "shouldLetSingleThrough", 2, 2);
        String result = bulkhead.apply(new InvocationContext<>(null));
        assertThat(result).isEqualTo("shouldLetSingleThrough");
    }

    @Test
    public void shouldLetMaxIn() throws Exception { // max threads + max queue
        Barrier delayBarrier = Barrier.noninterruptible();
        TestInvocation<String> invocation = TestInvocation.of(() -> {
            delayBarrier.await();
            return "shouldLetMaxThrough";
        });
        TwoSemaphoreBulkhead<String> bulkhead = new TwoSemaphoreBulkhead<>(invocation, "shouldLetSingleThrough", 2, 3);

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

        TestInvocation<String> invocation = TestInvocation.of(() -> {
            delayBarrier.await();
            return "shouldRejectMaxPlus1";
        });
        TwoSemaphoreBulkhead<String> bulkhead = new TwoSemaphoreBulkhead<>(invocation, "shouldRejectMaxPlus1", 2, 3);

        List<TestThread<String>> threads = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            threads.add(runOnTestThread(bulkhead));
        }
        waitUntilQueueSize(bulkhead, 3, Duration.ofMillis(500));

        assertThatThrownBy(() -> bulkhead.apply(new InvocationContext<>(null)))
            .isInstanceOf(BulkheadException.class);

        delayBarrier.open();
        for (int i = 0; i < 5; i++) {
            assertThat(threads.get(i).await()).isEqualTo("shouldRejectMaxPlus1");
        }
    }

    @Test
    public void shouldLetMaxPlus1After1Left() throws Exception {
        Barrier delayBarrier = Barrier.noninterruptible();
        Semaphore letOneInSemaphore = new Semaphore(1);

        TestInvocation<String> invocation = TestInvocation.of(() -> {
            delayBarrier.await();
            letOneInSemaphore.acquire();
            return "shouldLetMaxPlus1After1Left";
        });

        TwoSemaphoreBulkhead<String> bulkhead = new TwoSemaphoreBulkhead<>(invocation, "shouldLetMaxPlus1After1Left",
            2, 3);

        List<TestThread<String>> threads = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            threads.add(runOnTestThread(bulkhead));
        }

        delayBarrier.open();

        TestThread<String> finishedThread = getSingleFinishedThread(threads, Duration.ofSeconds(1));
        threads.remove(finishedThread);
        assertThat(finishedThread.await()).isEqualTo("shouldLetMaxPlus1After1Left");

        threads.add(runOnTestThread(bulkhead));
        letOneInSemaphore.release(100);
        for (TestThread<String> thread : threads) {
            assertThat(thread.await()).isEqualTo("shouldLetMaxPlus1After1Left");
        }
    }

    @Test
    public void shouldLetMaxPlus1After1Failed() throws Exception {
        RuntimeException error = new RuntimeException("forced");

        Semaphore letOneInSemaphore = new Semaphore(0);
        Semaphore finishedThreadsCount = new Semaphore(0);

        TestInvocation<String> invocation = TestInvocation.of(() -> {
            letOneInSemaphore.acquire();
            finishedThreadsCount.release();
            throw error;
        });

        TwoSemaphoreBulkhead<String> bulkhead = new TwoSemaphoreBulkhead<>(invocation, "shouldLetMaxPlus1After1Left",
            2, 3);

        List<TestThread<String>> threads = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            threads.add(runOnTestThread(bulkhead));
        }

        letOneInSemaphore.release();
        finishedThreadsCount.acquire();

        TestThread<String> finishedThread = getSingleFinishedThread(threads, Duration.ofSeconds(1));
        assertThatThrownBy(finishedThread::await).isEqualTo(error);
        threads.remove(finishedThread);

        threads.add(runOnTestThread(bulkhead));

        letOneInSemaphore.release(5);
        for (TestThread<String> thread : threads) {
            finishedThreadsCount.acquire();
            assertThatThrownBy(thread::await).isEqualTo(error);
        }
    }

    /*
     * put five elements into the bulkhead,
     * check another one cannot be inserted
     * cancel one,
     * insert another one
     * run the tasks and check results
     */
    @Test
    public void shouldLetMaxPlus1After1Canceled() throws Exception {
        Party party = Party.create(2);

        TestInvocation<String> invocation = TestInvocation.of(() -> {
            party.participant().attend();
            return "shouldLetMaxPlus1After1Canceled";
        });

        TwoSemaphoreBulkhead<String> bulkhead = new TwoSemaphoreBulkhead<>(invocation,
            "shouldLetMaxPlus1After1Canceled", 2, 3);

        List<TestThread<String>> threads = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            threads.add(runOnTestThread(bulkhead));
        }
        // to make sure the fifth added is enqueued, wait until two are started
        party.organizer().waitForAll();

        threads.add(runOnTestThread(bulkhead));

        waitUntilQueueSize(bulkhead, 3, Duration.ofSeconds(1));

        TestThread<String> failedThread = runOnTestThread(bulkhead);
        assertThatThrownBy(failedThread::await).isInstanceOf(BulkheadException.class);

        threads.remove(4).interrupt(); // cancel and remove from the list
        waitUntilQueueSize(bulkhead, 2, Duration.ofSeconds(1));

        threads.add(runOnTestThread(bulkhead));
        waitUntilQueueSize(bulkhead, 3, Duration.ofSeconds(1));

        party.organizer().disband();

        for (TestThread<String> thread : threads) {
            assertThat(thread.await()).isEqualTo("shouldLetMaxPlus1After1Canceled");
        }
    }

    private void waitUntilQueueSize(TwoSemaphoreBulkhead<String> bulkhead, int size, Duration timeout) {
        await().pollInterval(Duration.ofMillis(50))
            .atMost(timeout)
            .until(() -> bulkhead.getQueueSize() == size);
    }

    private <V> TestThread<V> getSingleFinishedThread(List<TestThread<V>> threads, Duration timeout) {
        return await().atMost(timeout)
            .until(() -> getSingleFinishedThread(threads), Optional::isPresent)
            .get();
    }

    private static <V> Optional<TestThread<V>> getSingleFinishedThread(List<TestThread<V>> threads) {
        for (TestThread<V> thread : threads) {
            if (thread.isDone()) {
                return Optional.of(thread);
            }
        }
        return Optional.empty();
    }

    /** Intended Use Case in virtual thread pool: limit queue length and concurrency */
    @Test
    void vtUseCase () {
        Awaitility.reset();
        AtomicInteger total = new AtomicInteger();
        AtomicInteger concurrent = new AtomicInteger();

        InvocationContext<Integer> fakeInvocationContext = new InvocationContext<>(()->{
            AssertionError e = new AssertionError("InvocationContext.call");
            e.printStackTrace();
            throw e;
        });

        TestInvocation<Integer> invocation = TestInvocation.of(()->{
            int index = total.incrementAndGet();
            int simultan = concurrent.incrementAndGet();
            try {
                if (simultan > 2){
                    AssertionError e = new AssertionError("concurrent > 2: " + simultan);
                    e.printStackTrace();
                    throw e;
                }
                if (index < 10){
                    Thread.sleep(1000);
                } else {
                    Thread.sleep(20);
                }
                return index;
            } finally {
                concurrent.decrementAndGet();
            }
        });
        TwoSemaphoreBulkhead<Integer> bulkhead = new TwoSemaphoreBulkhead<>(invocation, "q200c2", 2, 100);
        List<Future<Integer>> futures = new ArrayList<>();

        for (int i = 0; i < 200; i++){
            futures.add(executor.submit(()->bulkhead.
                apply(fakeInvocationContext)));
        }//f
        assertEquals(200, futures.size());

        int success = 0, failure = 0;
        for (Future<Integer> future : futures){
            try {
                future.get();
                success++;
            } catch (Throwable e){
                failure++;
            }
        }
        assertEquals(102, success);
        assertEquals(98, failure);
        assertEquals(0, concurrent.get());
        assertEquals(102, total.get());
    }

    @Test
    void _cancel_future () {
        TestInvocation<Integer> invocation = TestInvocation.of(()->{
                Thread.sleep(95000);
                return 42;
            });
        TwoSemaphoreBulkhead<Integer> bulkhead = new TwoSemaphoreBulkhead<>(invocation, "q200c2", 2, 100);

        Future<Integer> f = executor.submit(()->bulkhead.
            apply(new InvocationContext<>(()->{
                AssertionError e = new AssertionError("InvocationContext.call");
                e.printStackTrace();
                throw e;
            })));

        assertFalse(f.isDone());
        assertFalse(f.isCancelled());
        f.cancel(true);
        assertTrue(f.isDone());
        assertTrue(f.isCancelled());
        try {
            f.get();
            fail();
        } catch (Exception e) {
            assertEquals("java.util.concurrent.CancellationException", e.toString());
        }
    }

    @Test
    void _cancel_event () throws Exception {
        TestInvocation<Integer> invocation = TestInvocation.of(()->{
            Thread.sleep(1200);
            return 42;
        });
        TwoSemaphoreBulkhead<Integer> bulkhead = new TwoSemaphoreBulkhead<>(invocation, "q200c2", 1, 100);

        executor.submit(()->bulkhead.apply(new InvocationContext<>(null)));// -1 permit

        InvocationContext<Integer> ctx = new InvocationContext<>(()->{
            AssertionError e = new AssertionError("InvocationContext.call");
            e.printStackTrace();
            throw e;
        });

        Future<Integer> f = executor.submit(()->bulkhead.apply(ctx));

        assertFalse(f.isDone());
        assertFalse(f.isCancelled());
        Thread.sleep(100);// time to register event handler
        ctx.fireEvent(FutureCancellationEvent.NONINTERRUPTIBLE);
        try {
            f.get();
            fail();
        } catch (Exception e) {
            assertEquals("java.util.concurrent.ExecutionException: java.util.concurrent.CancellationException", e.toString());
        }
        assertTrue(f.isDone());
        assertFalse(f.isCancelled());
    }

    @Test
    void _cancel_event_interrupt () throws InterruptedException {
        TestInvocation<Integer> invocation = TestInvocation.of(()->{
            Thread.sleep(95000);
            return 42;
        });
        TwoSemaphoreBulkhead<Integer> bulkhead = new TwoSemaphoreBulkhead<>(invocation, "q200c2", 2, 100);

        InvocationContext<Integer> ctx = new InvocationContext<>(()->{
            AssertionError e = new AssertionError("InvocationContext.call");
            e.printStackTrace();
            throw e;
        });

        Future<Integer> f = executor.submit(()->bulkhead.
            apply(ctx));

        assertFalse(f.isDone());
        assertFalse(f.isCancelled());
        Thread.sleep(200);// time to register event handler
        ctx.fireEvent(FutureCancellationEvent.INTERRUPTIBLE);
        try {
            f.get();
            fail();
        } catch (Exception e) {
            assertEquals("java.util.concurrent.ExecutionException: java.lang.InterruptedException: sleep interrupted", e.toString());
        }
        assertTrue(f.isDone());
        assertFalse(f.isCancelled());
    }
}
