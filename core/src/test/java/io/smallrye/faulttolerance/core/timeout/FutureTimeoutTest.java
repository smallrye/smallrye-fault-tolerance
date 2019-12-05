package io.smallrye.faulttolerance.core.timeout;

import static io.smallrye.faulttolerance.core.timeout.FutureTestInvocation.immediatelyReturning;
import static io.smallrye.faulttolerance.core.util.FutureTestThread.mockContext;
import static io.smallrye.faulttolerance.core.util.FutureTestThread.runOnTestThread;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.junit.Before;
import org.junit.Test;

import io.smallrye.faulttolerance.core.Cancellator;
import io.smallrye.faulttolerance.core.FutureInvocationContext;
import io.smallrye.faulttolerance.core.util.FutureTestThread;
import io.smallrye.faulttolerance.core.util.TestException;
import io.smallrye.faulttolerance.core.util.barrier.Barrier;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public class FutureTimeoutTest {
    private Barrier watcherTimeoutElapsedBarrier;
    private Barrier watcherExecutionInterruptedBarrier;

    private TestTimeoutWatcher timeoutWatcher;
    private Executor asyncExecutor = Executors.newFixedThreadPool(4);

    @Before
    public void setUp() {
        watcherTimeoutElapsedBarrier = Barrier.interruptible();
        watcherExecutionInterruptedBarrier = Barrier.interruptible();

        timeoutWatcher = new TestTimeoutWatcher(watcherTimeoutElapsedBarrier, watcherExecutionInterruptedBarrier);
    }

    @Test
    public void failOnLackOfExecutor() {
        FutureTestInvocation<String> invocation = immediatelyReturning(() -> CompletableFuture.completedFuture("foobar"));
        assertThatThrownBy(() -> new FutureTimeout<>(invocation, "test invocation", 1000, timeoutWatcher, null, null))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Async Future execution requires an asyncExecutor, none provided");
    }

    @Test
    public void immediatelyReturning_value() throws Exception {
        FutureTestInvocation<String> invocation = immediatelyReturning(() -> CompletableFuture.completedFuture("foobar"));
        FutureTestThread<String> result = runOnTestThread(
                new FutureTimeout<>(invocation, "test invocation", 1000, timeoutWatcher, null, asyncExecutor), mockContext());
        Future<String> future = result.await();
        assertThat(future.get()).isEqualTo("foobar");
        assertThat(timeoutWatcher.timeoutWatchWasCancelled()).isTrue();
    }

    @Test
    public void immediatelyReturning_exception() {
        FutureTestInvocation<Void> invocation = FutureTestInvocation.immediatelyReturning(() -> {
            throw new TestException();
        });
        FutureTestThread<Void> result = runOnTestThread(new FutureTimeout<>(invocation, "test invocation",
                1000, timeoutWatcher, null, asyncExecutor), mockContext());

        assertThatThrownBy(result::await).isExactlyInstanceOf(TestException.class);
        assertThat(timeoutWatcher.timeoutWatchWasCancelled()).isTrue();
    }

    @Test
    public void delayed_value_notTimedOut() throws Exception {
        Barrier invocationDelayBarrier = Barrier.interruptible();

        FutureTestInvocation<String> invocation = FutureTestInvocation.delayed(invocationDelayBarrier,
                () -> CompletableFuture.completedFuture("foobar"));
        FutureTestThread<String> result = runOnTestThread(new FutureTimeout<>(invocation, "test invocation",
                1000, timeoutWatcher, null, asyncExecutor), mockContext());
        invocationDelayBarrier.open();
        Future<String> future = result.await();
        assertThat(future.get()).isEqualTo("foobar");
        assertThat(future.isDone()).isEqualTo(true);
        assertThat(timeoutWatcher.timeoutWatchWasCancelled()).isTrue();
    }

    @Test
    public void delayed_value_timedOut() throws InterruptedException {
        Barrier invocationDelayBarrier = Barrier.interruptible();

        FutureTestInvocation<String> invocation = FutureTestInvocation.delayed(invocationDelayBarrier,
                () -> CompletableFuture.completedFuture("foobar"));
        FutureTestThread<String> result = runOnTestThread(new FutureTimeout<>(invocation, "test invocation",
                1000, timeoutWatcher, null, asyncExecutor), mockContext());
        watcherTimeoutElapsedBarrier.open();
        watcherExecutionInterruptedBarrier.await();
        assertThatThrownBy(result::await)
                .isExactlyInstanceOf(TimeoutException.class)
                .hasMessage("test invocation timed out");
        assertThat(timeoutWatcher.timeoutWatchWasCancelled()).isFalse();
    }

    @Test
    public void delayed_value_timedOutNoninterruptibly() throws InterruptedException {
        Barrier invocationDelayBarrier = Barrier.noninterruptible();

        FutureTestInvocation<String> invocation = FutureTestInvocation.delayed(invocationDelayBarrier,
                () -> CompletableFuture.completedFuture("foobar"));
        FutureTestThread<String> result = runOnTestThread(new FutureTimeout<>(invocation, "test invocation", 1000,
                timeoutWatcher, null, asyncExecutor), mockContext());
        watcherTimeoutElapsedBarrier.open();
        watcherExecutionInterruptedBarrier.await();
        invocationDelayBarrier.open();
        assertThatThrownBy(result::await)
                .isExactlyInstanceOf(TimeoutException.class)
                .hasMessage("test invocation timed out");
        assertThat(timeoutWatcher.timeoutWatchWasCancelled()).isFalse(); // watcher should not be canceled if it caused the stop
    }

    @Test
    public void delayed_value_cancelled() throws InterruptedException {
        Barrier invocationStartBarrier = Barrier.interruptible();
        Barrier invocationDelayBarrier = Barrier.interruptible();

        Callable<Future<String>> action = () -> CompletableFuture.completedFuture("foobar");
        FutureTestInvocation<String> invocation = FutureTestInvocation.delayed(invocationStartBarrier, invocationDelayBarrier,
                action);
        Cancellator cancellator = new Cancellator();

        FutureTestThread<String> executingThread = runOnTestThread(new FutureTimeout<>(invocation, "test invocation",
                100000, timeoutWatcher, null, asyncExecutor), new FutureInvocationContext<>(cancellator, null));
        invocationStartBarrier.await();

        cancellator.cancel(false);
        assertThatThrownBy(executingThread::await).isExactlyInstanceOf(InterruptedException.class);
        assertThat(timeoutWatcher.timeoutWatchWasCancelled()).isTrue();
    }

    @Test
    public void delayed_value_selfInterrupted() throws Exception {
        Barrier delayBarrier = Barrier.interruptible();

        Callable<Future<String>> action = () -> {
            Thread.currentThread().interrupt();
            delayBarrier.await();
            return CompletableFuture.completedFuture("foobar");
        };
        FutureTestInvocation<String> invocation = FutureTestInvocation.immediatelyReturning(action);
        Cancellator cancellator = new Cancellator();

        FutureTestThread<String> executingThread = runOnTestThread(new FutureTimeout<>(invocation, "test invocation",
                100000, timeoutWatcher, null, asyncExecutor), new FutureInvocationContext<>(cancellator, null));
        delayBarrier.open();

        assertThatThrownBy(executingThread::await).isExactlyInstanceOf(InterruptedException.class);
        assertThat(timeoutWatcher.timeoutWatchWasCancelled()).isTrue();
    }

    @Test
    public void immediate_value_nonInterruptibleCancelShouldBePropagated() throws Exception {
        Barrier delayBarrier = Barrier.interruptible();

        Callable<Future<String>> action = () -> CompletableFuture.supplyAsync(() -> {
            try {
                delayBarrier.await();
            } catch (InterruptedException e) {
                throw new CompletionException(e);
            }
            return "foobar";
        });
        FutureTestInvocation<String> invocation = FutureTestInvocation.immediatelyReturning(action);

        FutureTestThread<String> executingThread = runOnTestThread(new FutureTimeout<>(invocation, "test invocation",
                100000, timeoutWatcher, null, asyncExecutor), mockContext());

        Future<String> future = executingThread.await();
        future.cancel(false);

        assertThat(future.isCancelled());
        delayBarrier.open();
    }

    @Test
    public void immediate_value_interruptibleCancelShouldBePropagated() throws Exception {
        Barrier delayBarrier = Barrier.interruptible();

        Callable<Future<String>> action = () -> CompletableFuture.supplyAsync(() -> {
            try {
                delayBarrier.await();
            } catch (InterruptedException e) {
                throw new CompletionException(e);
            }
            return "foobar";
        });
        FutureTestInvocation<String> invocation = FutureTestInvocation.immediatelyReturning(action);

        FutureTestThread<String> executingThread = runOnTestThread(new FutureTimeout<>(invocation, "test invocation",
                100000, timeoutWatcher, null, asyncExecutor), mockContext());

        Future<String> future = executingThread.await();
        future.cancel(true);

        assertThat(future.isCancelled());
        assertThatThrownBy(future::get).isExactlyInstanceOf(InterruptedException.class);
        delayBarrier.open();
    }

    @Test
    public void delayed_value_timedGetRethrowsEventualError() throws Exception {
        RuntimeException exception = new RuntimeException("forced");

        Callable<Future<String>> action = () -> CompletableFuture.supplyAsync(() -> {
            throw exception;
        });
        FutureTestInvocation<String> invocation = FutureTestInvocation.immediatelyReturning(action);

        FutureTestThread<String> executingThread = runOnTestThread(new FutureTimeout<>(invocation, "test invocation",
                100000, timeoutWatcher, null, asyncExecutor), mockContext());

        Future<String> future = executingThread.await();

        assertThatThrownBy(() -> future.get(1000, TimeUnit.MILLISECONDS))
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCause(exception);
    }

    @Test
    public void delayed_value_getRethrowsError() throws Exception {
        RuntimeException exception = new RuntimeException("forced");

        Callable<Future<String>> action = () -> CompletableFuture.supplyAsync(() -> {
            throw exception;
        });
        FutureTestInvocation<String> invocation = FutureTestInvocation.immediatelyReturning(action);

        FutureTestThread<String> executingThread = runOnTestThread(new FutureTimeout<>(invocation, "test invocation",
                100000, timeoutWatcher, null, asyncExecutor), mockContext());

        Future<String> future = executingThread.await();

        assertThatThrownBy(future::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCause(exception);
    }
}
