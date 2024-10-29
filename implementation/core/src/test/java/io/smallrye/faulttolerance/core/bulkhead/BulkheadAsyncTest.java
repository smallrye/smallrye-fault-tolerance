package io.smallrye.faulttolerance.core.bulkhead;

import static io.smallrye.faulttolerance.core.FaultToleranceContextUtil.async;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.core.Future;
import io.smallrye.faulttolerance.core.async.ThreadOffload;
import io.smallrye.faulttolerance.core.util.TestInvocation;
import io.smallrye.faulttolerance.core.util.barrier.Barrier;
import io.smallrye.faulttolerance.core.util.party.Party;

public class BulkheadAsyncTest {
    private ExecutorService executor;

    @BeforeEach
    public void setUp() {
        executor = new ThreadPoolExecutor(2, 2, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        executor.shutdownNow();
        executor.awaitTermination(1, TimeUnit.SECONDS);
    }

    @Test
    public void shouldLetOneIn() throws Throwable {
        TestInvocation<String> invocation = TestInvocation.of(() -> "shouldLetSingleThrough");
        ThreadOffload<String> execution = new ThreadOffload<>(invocation, executor);
        Bulkhead<String> bulkhead = new Bulkhead<>(execution,
                "shouldLetSingleThrough", 2, 2, false);

        Future<String> result = bulkhead.apply(async(null));
        assertThat(result.awaitBlocking()).isEqualTo("shouldLetSingleThrough");
    }

    @Test
    public void shouldLetMaxIn() throws Throwable { // max threads + max queue
        TestInvocation<String> invocation = TestInvocation.of(() -> "shouldLetMaxThrough");
        ThreadOffload<String> execution = new ThreadOffload<>(invocation, executor);
        Bulkhead<String> bulkhead = new Bulkhead<>(execution,
                "shouldLetSingleThrough", 2, 3, false);

        List<Future<String>> results = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            results.add(bulkhead.apply(async(null)));
        }
        for (Future<String> result : results) {
            assertThat(result.awaitBlocking()).isEqualTo("shouldLetMaxThrough");
        }
    }

    @Test
    public void shouldRejectMaxPlus1() throws Throwable {
        Barrier delayBarrier = Barrier.noninterruptible();

        TestInvocation<String> invocation = TestInvocation.of(() -> {
            delayBarrier.await();
            return "shouldRejectMaxPlus1";
        });
        ThreadOffload<String> execution = new ThreadOffload<>(invocation, executor);
        Bulkhead<String> bulkhead = new Bulkhead<>(execution,
                "shouldRejectMaxPlus1", 2, 3, false);

        List<Future<String>> results = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            results.add(bulkhead.apply(async(null)));
        }
        // to make sure all the tasks are in bulkhead:
        waitUntilQueueSize(bulkhead, 3, 1000);

        Future<String> plus1Call = bulkhead.apply(async(null));
        assertThatThrownBy(plus1Call::awaitBlocking).isInstanceOf(BulkheadException.class);

        delayBarrier.open();

        for (Future<String> result : results) {
            assertThat(result.awaitBlocking()).isEqualTo("shouldRejectMaxPlus1");
        }
    }

    @Test
    public void shouldLetMaxPlus1After1Left() throws Throwable {
        Barrier delayBarrier = Barrier.noninterruptible();
        Semaphore letOneInSemaphore = new Semaphore(1);

        TestInvocation<String> invocation = TestInvocation.of(() -> {
            delayBarrier.await();
            letOneInSemaphore.acquire();
            return "shouldLetMaxPlus1After1Left";
        });
        ThreadOffload<String> execution = new ThreadOffload<>(invocation, executor);
        Bulkhead<String> bulkhead = new Bulkhead<>(execution,
                "shouldLetMaxPlus1After1Left", 2, 3, false);

        List<Future<String>> results = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            results.add(bulkhead.apply(async(null)));
        }

        delayBarrier.open();

        Future<String> finishedResult = getSingleFinishedResult(results, 1000);
        results.remove(finishedResult);
        assertThat(finishedResult.awaitBlocking()).isEqualTo("shouldLetMaxPlus1After1Left");

        results.add(bulkhead.apply(async(null)));
        letOneInSemaphore.release(100);
        for (Future<String> result : results) {
            assertThat(result.awaitBlocking()).isEqualTo("shouldLetMaxPlus1After1Left");
        }
    }

    @Test
    public void shouldLetMaxPlus1After1Failed() {
        RuntimeException error = new RuntimeException("forced");

        Barrier delayBarrier = Barrier.noninterruptible();
        Semaphore letOneInSemaphore = new Semaphore(1);

        TestInvocation<String> invocation = TestInvocation.of(() -> {
            delayBarrier.await();
            letOneInSemaphore.acquire();
            throw error;
        });
        ThreadOffload<String> execution = new ThreadOffload<>(invocation, executor);
        Bulkhead<String> bulkhead = new Bulkhead<>(execution,
                "shouldLetMaxPlus1After1Failed", 2, 3, false);

        List<Future<String>> results = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            results.add(bulkhead.apply(async(null)));
        }

        delayBarrier.open();

        Future<String> finishedResult = getSingleFinishedResult(results, 1000);
        results.remove(finishedResult);
        assertThatThrownBy(finishedResult::awaitBlocking).isEqualTo(error);

        results.add(bulkhead.apply(async(null)));
        letOneInSemaphore.release(100);
        for (Future<String> result : results) {
            assertThatThrownBy(result::awaitBlocking).isEqualTo(error);
        }
    }

    @Test
    public void shouldLetMaxPlus1After1FailedCompletionStage() {
        RuntimeException error = new RuntimeException("forced");

        Barrier delayBarrier = Barrier.noninterruptible();
        Semaphore letOneInSemaphore = new Semaphore(1);

        TestInvocation<String> invocation = TestInvocation.of(() -> {
            delayBarrier.await();
            letOneInSemaphore.acquire();
            throw error;
        });
        ThreadOffload<String> execution = new ThreadOffload<>(invocation, executor);
        Bulkhead<String> bulkhead = new Bulkhead<>(execution,
                "shouldLetMaxPlus1After1Failed", 2, 3, false);

        List<Future<String>> results = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            results.add(bulkhead.apply(async(null)));
        }

        delayBarrier.open();

        Future<String> finishedResult = getSingleFinishedResult(results, 1000);
        results.remove(finishedResult);
        assertThatThrownBy(finishedResult::awaitBlocking).isEqualTo(error);

        results.add(bulkhead.apply(async(null)));
        letOneInSemaphore.release(100);
        for (Future<String> result : results) {
            assertThatThrownBy(result::awaitBlocking).isEqualTo(error);
        }
    }

    @Test
    public void shouldNotStartNextIfCSInProgress() throws Throwable {
        Party party = Party.create(1);

        TestInvocation<String> invocation = TestInvocation.of(() -> {
            party.participant().attend();
            return "shouldNotStartNextIfCSInProgress";
        });
        ThreadOffload<String> execution = new ThreadOffload<>(invocation, executor);
        Bulkhead<String> bulkhead = new Bulkhead<>(execution,
                "shouldNotStartNextIfCSInProgress", 1, 1, false);

        Future<String> firstResult = bulkhead.apply(async(null));

        party.organizer().waitForAll();

        Future<String> secondResult = bulkhead.apply(async(null));
        waitUntilQueueSize(bulkhead, 1, 100);

        party.organizer().disband();

        assertThat(firstResult.awaitBlocking()).isEqualTo("shouldNotStartNextIfCSInProgress");
        assertThat(secondResult.awaitBlocking()).isEqualTo("shouldNotStartNextIfCSInProgress");
        assertThat(bulkhead.getQueueSize()).isZero();
    }

    private static <V> void waitUntilQueueSize(Bulkhead<V> bulkhead, int size, long timeout) {
        await().pollInterval(Duration.ofMillis(10))
                .atMost(Duration.ofMillis(timeout))
                .until(() -> bulkhead.getQueueSize() == size);
    }

    private static <V> Future<V> getSingleFinishedResult(List<Future<V>> results, long timeout) {
        return await().atMost(Duration.ofMillis(timeout))
                .until(() -> getSingleFinishedResult(results), Optional::isPresent)
                .get();
    }

    private static <V> Optional<Future<V>> getSingleFinishedResult(List<Future<V>> results) {
        for (Future<V> result : results) {
            if (result.isComplete()) {
                return Optional.of(result);
            }
        }
        return Optional.empty();
    }
}
