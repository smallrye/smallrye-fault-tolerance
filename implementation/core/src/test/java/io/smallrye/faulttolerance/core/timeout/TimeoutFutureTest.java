package io.smallrye.faulttolerance.core.timeout;

import static io.smallrye.faulttolerance.core.util.TestThread.runOnTestThread;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.core.util.TestException;
import io.smallrye.faulttolerance.core.util.TestInvocation;
import io.smallrye.faulttolerance.core.util.TestThread;
import io.smallrye.faulttolerance.core.util.barrier.Barrier;
import io.smallrye.faulttolerance.core.util.party.Party;

public class TimeoutFutureTest {
    private Barrier timerElapsedBarrier;
    private Barrier timerTaskFinishedBarrier;

    private TestTimer timer;

    private ExecutorService asyncExecutor;

    @BeforeEach
    public void setUp() {
        timerElapsedBarrier = Barrier.interruptible();
        timerTaskFinishedBarrier = Barrier.interruptible();

        timer = new TestTimer(timerElapsedBarrier, timerTaskFinishedBarrier);

        asyncExecutor = Executors.newFixedThreadPool(4);
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        asyncExecutor.shutdown();
        asyncExecutor.awaitTermination(10, TimeUnit.SECONDS);

        timer.shutdown();
    }

    @Test
    public void failOnLackOfExecutor() {
        TestInvocation<java.util.concurrent.Future<String>> invocation = TestInvocation.of(() -> completedFuture("foobar"));
        Timeout<Future<String>> timeout = new Timeout<>(invocation, "test invocation", 1000, timer);
        assertThatThrownBy(() -> new FutureTimeout<>(timeout, null))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Executor must be set");
    }

    @Test
    public void immediatelyReturning_value() throws Exception {
        TestThread<java.util.concurrent.Future<String>> testThread = runFutureTimeout(() -> completedFuture("foobar"));
        assertThat(testThread.await().get()).isEqualTo("foobar");
        assertThat(timer.timerTaskCancelled()).isTrue();
    }

    @Test
    public void immediatelyReturning_exception() {
        TestThread<java.util.concurrent.Future<String>> testThread = runFutureTimeout(TestException::doThrow);
        assertThatThrownBy(testThread::await).isExactlyInstanceOf(TestException.class);
        assertThat(timer.timerTaskCancelled()).isTrue();
    }

    @Test
    public void delayed_value_notTimedOut() throws Exception {
        Barrier delayBarrier = Barrier.interruptible();

        TestThread<java.util.concurrent.Future<String>> testThread = runFutureTimeout(() -> {
            delayBarrier.await();
            return completedFuture("foobar");
        });
        delayBarrier.open();

        java.util.concurrent.Future<String> future = testThread.await();
        assertThat(future.get()).isEqualTo("foobar");
        assertThat(future.isDone()).isTrue();
        assertThat(timer.timerTaskCancelled()).isTrue();
    }

    @Test
    public void delayed_value_timedOut() throws InterruptedException {
        Barrier delayBarrier = Barrier.interruptible();

        TestThread<java.util.concurrent.Future<String>> testThread = runFutureTimeout(() -> {
            delayBarrier.await();
            return completedFuture("foobar");
        });
        timerElapsedBarrier.open();
        timerTaskFinishedBarrier.await();

        assertThatThrownBy(testThread::await)
                .isExactlyInstanceOf(TimeoutException.class)
                .hasMessage("test invocation timed out");
        assertThat(timer.timerTaskCancelled()).isFalse(); // watcher should not be canceled if it caused the stop
    }

    @Test
    public void delayed_value_timedOutNoninterruptibly() throws InterruptedException {
        Barrier delayBarrier = Barrier.noninterruptible();

        TestThread<java.util.concurrent.Future<String>> testThread = runFutureTimeout(() -> {
            delayBarrier.await();
            return completedFuture("foobar");
        });
        timerElapsedBarrier.open();
        timerTaskFinishedBarrier.await();

        assertThatThrownBy(testThread::await)
                .isExactlyInstanceOf(TimeoutException.class)
                .hasMessage("test invocation timed out");
        assertThat(timer.timerTaskCancelled()).isFalse(); // watcher should not be canceled if it caused the stop

        delayBarrier.open();
    }

    @Test
    public void delayed_value_cancelled() throws InterruptedException {
        Party party = Party.create(1);

        TestThread<java.util.concurrent.Future<String>> testThread = runFutureTimeout(() -> {
            party.participant().attend();
            return completedFuture("foobar");
        });

        party.organizer().waitForAll();
        testThread.interrupt();

        assertThatThrownBy(testThread::await)
                .isExactlyInstanceOf(InterruptedException.class);
        assertThat(timer.timerTaskCancelled()).isFalse(); // watcher should not be canceled if it caused the stop

        party.organizer().disband(); // not strictly necessary, but would cause tearDown to wait
    }

    @Test
    public void delayed_value_selfInterrupted() {
        Barrier delayBarrier = Barrier.interruptible();

        TestThread<java.util.concurrent.Future<String>> testThread = runFutureTimeout(() -> {
            Thread.currentThread().interrupt();
            delayBarrier.await();
            return completedFuture("foobar");
        });

        delayBarrier.open();

        assertThatThrownBy(testThread::await).isExactlyInstanceOf(InterruptedException.class);
        assertThat(timer.timerTaskCancelled()).isTrue();
    }

    @Test
    public void immediate_value_nonInterruptibleCancelShouldBePropagated() throws Exception {
        Barrier delayBarrier = Barrier.interruptible();

        TestThread<java.util.concurrent.Future<String>> testThread = runFutureTimeout(() -> {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    delayBarrier.await();
                } catch (InterruptedException e) {
                    throw new CompletionException(e);
                }
                return "foobar";
            });
        });

        java.util.concurrent.Future<String> future = testThread.await();
        future.cancel(false);

        assertThat(future.isCancelled()).isTrue();
        assertThatThrownBy(future::get).isExactlyInstanceOf(CancellationException.class);
        delayBarrier.open();
    }

    @Test
    public void immediate_value_interruptibleCancelShouldBePropagated() throws Exception {
        Barrier delayBarrier = Barrier.interruptible();

        TestThread<java.util.concurrent.Future<String>> testThread = runFutureTimeout(() -> {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    delayBarrier.await();
                } catch (InterruptedException e) {
                    throw new CompletionException(e);
                }
                return "foobar";
            });
        });

        java.util.concurrent.Future<String> future = testThread.await();
        future.cancel(true);

        assertThat(future.isCancelled()).isTrue();
        // this changed with 10/12/2019 refactoring, previously it was interrupted exception
        // looks okay though
        assertThatThrownBy(future::get).isExactlyInstanceOf(CancellationException.class);
        delayBarrier.open();
    }

    @Test
    public void delayed_value_timedGetRethrowsEventualError() throws Exception {
        RuntimeException exception = new RuntimeException("forced");

        TestThread<java.util.concurrent.Future<String>> testThread = runFutureTimeout(() -> {
            return CompletableFuture.supplyAsync(() -> {
                throw exception;
            });
        });

        java.util.concurrent.Future<String> future = testThread.await();

        assertThatThrownBy(() -> future.get(1000, TimeUnit.MILLISECONDS))
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCause(exception);
    }

    @Test
    public void delayed_value_getRethrowsError() throws Exception {
        RuntimeException exception = new RuntimeException("forced");

        TestThread<java.util.concurrent.Future<String>> testThread = runFutureTimeout(() -> {
            return CompletableFuture.supplyAsync(() -> {
                throw exception;
            });
        });

        java.util.concurrent.Future<String> future = testThread.await();

        assertThatThrownBy(future::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCause(exception);
    }

    private TestThread<java.util.concurrent.Future<String>> runFutureTimeout(
            Callable<java.util.concurrent.Future<String>> action) {
        TestInvocation<java.util.concurrent.Future<String>> invocation = TestInvocation.of(action);
        Timeout<Future<String>> timeout = new Timeout<>(invocation, "test invocation", 1000, timer);
        return runOnTestThread(new FutureTimeout<>(timeout, asyncExecutor), false);
    }
}
