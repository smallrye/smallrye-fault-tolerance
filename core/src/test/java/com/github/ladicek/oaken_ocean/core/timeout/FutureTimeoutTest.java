package com.github.ladicek.oaken_ocean.core.timeout;

import com.github.ladicek.oaken_ocean.core.Cancellator;
import com.github.ladicek.oaken_ocean.core.FutureInvocationContext;
import com.github.ladicek.oaken_ocean.core.util.CancellableTestThread;
import com.github.ladicek.oaken_ocean.core.util.TestException;
import com.github.ladicek.oaken_ocean.core.util.barrier.Barrier;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.github.ladicek.oaken_ocean.core.timeout.FutureTestInvocation.immediatelyReturning;
import static com.github.ladicek.oaken_ocean.core.util.CancellableTestThread.mockContext;
import static com.github.ladicek.oaken_ocean.core.util.CancellableTestThread.runOnTestThread;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        CancellableTestThread<String> result =
              runOnTestThread(new FutureTimeout<>(invocation, "test invocation", 1000, timeoutWatcher, null, asyncExecutor), mockContext());
        Future<String> future = result.await();
        assertThat(future.get()).isEqualTo("foobar");
        assertThat(timeoutWatcher.timeoutWatchWasCancelled()).isTrue();
    }

    @Test
    public void immediatelyReturning_exception() {
        FutureTestInvocation<Void> invocation = FutureTestInvocation.immediatelyReturning(() -> {
            throw new TestException();
        });
        CancellableTestThread<Void> result = runOnTestThread(new FutureTimeout<>(invocation, "test invocation",
              1000, timeoutWatcher, null, asyncExecutor), mockContext());

        assertThatThrownBy(result::await).isExactlyInstanceOf(TestException.class);
        assertThat(timeoutWatcher.timeoutWatchWasCancelled()).isTrue();
    }

    @Test
    public void delayed_value_notTimedOut() throws Exception {
        Barrier invocationDelayBarrier = Barrier.interruptible();

        FutureTestInvocation<String> invocation =
              FutureTestInvocation.delayed(invocationDelayBarrier, () -> CompletableFuture.completedFuture("foobar"));
        CancellableTestThread<String> result =
              runOnTestThread(new FutureTimeout<>(invocation, "test invocation",
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

        FutureTestInvocation<String> invocation =
              FutureTestInvocation.delayed(invocationDelayBarrier, () -> CompletableFuture.completedFuture("foobar"));
        CancellableTestThread<String> result = runOnTestThread(new FutureTimeout<>(invocation, "test invocation",
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

        FutureTestInvocation<String> invocation =
              FutureTestInvocation.delayed(invocationDelayBarrier, () -> CompletableFuture.completedFuture("foobar"));
        CancellableTestThread<String> result =
              runOnTestThread(new FutureTimeout<>(invocation, "test invocation", 1000,
                    timeoutWatcher, null, asyncExecutor), mockContext());
        watcherTimeoutElapsedBarrier.open();
        watcherExecutionInterruptedBarrier.await();
        invocationDelayBarrier.open();
        assertThatThrownBy(result::await)
              .isExactlyInstanceOf(TimeoutException.class)
              .hasMessage("test invocation timed out");
        assertThat(timeoutWatcher.timeoutWatchWasCancelled()).isTrue();
    }

    @Test
    public void delayed_value_Cancelled() throws InterruptedException {
        Barrier invocationStartBarrier = Barrier.interruptible();
        Barrier invocationDelayBarrier = Barrier.interruptible();

        Callable<Future<String>> action = () -> CompletableFuture.completedFuture("foobar");
        FutureTestInvocation<String> invocation = FutureTestInvocation.delayed(invocationStartBarrier, invocationDelayBarrier, action);
        Cancellator cancellator = new Cancellator();

        CancellableTestThread<String> executingThread =
              runOnTestThread(new FutureTimeout<>(invocation, "test invocation",
              1000, timeoutWatcher, null, asyncExecutor), new FutureInvocationContext<>(cancellator, null));
        invocationStartBarrier.await();

        cancellator.cancel(false);
        assertThatThrownBy(executingThread::await).isExactlyInstanceOf(InterruptedException.class);
        assertThat(timeoutWatcher.timeoutWatchWasCancelled()).isTrue();
    }
// mstodo
//    // mstodo interruption test!
//    // mstodo start here
//
//    @Test
//    public void delayed_exception_notTimedOut() {
//        Barrier invocationDelayBarrier = Barrier.interruptible();
//
//        TestInvocation<Void> invocation = TestInvocation.delayed(invocationDelayBarrier, TestException::doThrow);
//        TestThread<Void> result = runOnTestThread(new SyncTimeout<>(invocation, "test invocation", 1000, timeoutWatcher, null));
//        invocationDelayBarrier.open();
//        assertThatThrownBy(result::await).isExactlyInstanceOf(TestException.class);
//        assertThat(timeoutWatcher.timeoutWatchWasCancelled()).isTrue();
//    }
//
//    @Test
//    public void delayed_exception_timedOut() throws InterruptedException {
//        Barrier invocationDelayBarrier = Barrier.interruptible();
//
//        TestInvocation<Void> invocation = TestInvocation.delayed(invocationDelayBarrier, TestException::doThrow);
//        TestThread<Void> result = runOnTestThread(new SyncTimeout<>(invocation, "test invocation", 1000, timeoutWatcher, null));
//        watcherTimeoutElapsedBarrier.open();
//        watcherExecutionInterruptedBarrier.await();
//        assertThatThrownBy(result::await)
//              .isExactlyInstanceOf(TimeoutException.class)
//              .hasMessage("test invocation timed out");
//        assertThat(timeoutWatcher.timeoutWatchWasCancelled()).isFalse();
//    }
//
//    @Test
//    public void delayed_exception_timedOutNoninterruptibly() throws InterruptedException {
//        Barrier invocationDelayBarrier = Barrier.noninterruptible();
//
//        TestInvocation<Void> invocation = TestInvocation.delayed(invocationDelayBarrier, TestException::doThrow);
//        TestThread<Void> result = runOnTestThread(new SyncTimeout<>(invocation, "test invocation", 1000, timeoutWatcher, null));
//        watcherTimeoutElapsedBarrier.open();
//        watcherExecutionInterruptedBarrier.await();
//        invocationDelayBarrier.open();
//        assertThatThrownBy(result::await)
//              .isExactlyInstanceOf(TimeoutException.class)
//              .hasMessage("test invocation timed out");
//        assertThat(timeoutWatcher.timeoutWatchWasCancelled()).isFalse();
//    }
//
//    @Test
//    public void delayed_exception_interruptedEarly() throws InterruptedException {
//        Barrier invocationStartBarrier = Barrier.interruptible();
//        Barrier invocationDelayBarrier = Barrier.interruptible();
//
//        TestInvocation<Void> invocation = TestInvocation.delayed(invocationStartBarrier, invocationDelayBarrier, TestException::doThrow);
//        TestThread<Void> executingThread = runOnTestThread(new SyncTimeout<>(invocation, "test invocation", 1000, timeoutWatcher, null));
//        invocationStartBarrier.await();
//        executingThread.interrupt();
//        assertThatThrownBy(executingThread::await).isExactlyInstanceOf(InterruptedException.class);
//        assertThat(timeoutWatcher.timeoutWatchWasCancelled()).isTrue();
//    }
}
