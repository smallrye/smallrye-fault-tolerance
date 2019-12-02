package com.github.ladicek.oaken_ocean.core.timeout;

import com.github.ladicek.oaken_ocean.core.util.TestException;
import com.github.ladicek.oaken_ocean.core.util.TestThread;
import com.github.ladicek.oaken_ocean.core.util.barrier.Barrier;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.github.ladicek.oaken_ocean.core.timeout.FutureTestInvocation.immediatelyReturning;
import static com.github.ladicek.oaken_ocean.core.util.TestThread.runOnTestThread;
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
// mstodo interruption test!
    @Test
    public void immediatelyReturning_value() throws Exception {
        FutureTestInvocation<String> invocation = immediatelyReturning(() -> CompletableFuture.completedFuture("foobar"));
        TestThread<Future<String>> result = runOnTestThread(new FutureTimeout<>(invocation, "test invocation", 1000, timeoutWatcher, null, asyncExecutor));
        Future<String> future = result.await();
        assertThat(future.get()).isEqualTo("foobar");
        assertThat(timeoutWatcher.timeoutWatchWasCancelled()).isTrue();
    }

    // mstodo start here
    @Test
    public void immediatelyReturning_exception() {
        TestInvocation<Void> invocation = TestInvocation.immediatelyReturning(TestException::doThrow);
        TestThread<Void> result = runOnTestThread(new SyncTimeout<>(invocation, "test invocation", 1000, timeoutWatcher, null));
        assertThatThrownBy(result::await).isExactlyInstanceOf(TestException.class);
        assertThat(timeoutWatcher.timeoutWatchWasCancelled()).isTrue();
    }

    @Test
    public void delayed_value_notTimedOut() throws Exception {
        Barrier invocationDelayBarrier = Barrier.interruptible();

        TestInvocation<String> invocation = TestInvocation.delayed(invocationDelayBarrier, () -> "foobar");
        TestThread<String> result = runOnTestThread(new SyncTimeout<>(invocation, "test invocation", 1000, timeoutWatcher, null));
        invocationDelayBarrier.open();
        assertThat(result.await()).isEqualTo("foobar");
        assertThat(timeoutWatcher.timeoutWatchWasCancelled()).isTrue();
    }

    @Test
    public void delayed_value_timedOut() throws InterruptedException {
        Barrier invocationDelayBarrier = Barrier.interruptible();

        TestInvocation<String> invocation = TestInvocation.delayed(invocationDelayBarrier, () -> "foobar");
        TestThread<String> result = runOnTestThread(new SyncTimeout<>(invocation, "test invocation", 1000, timeoutWatcher, null));
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

        TestInvocation<String> invocation = TestInvocation.delayed(invocationDelayBarrier, () -> "foobar");
        TestThread<String> result = runOnTestThread(new SyncTimeout<>(invocation, "test invocation", 1000, timeoutWatcher, null));
        watcherTimeoutElapsedBarrier.open();
        watcherExecutionInterruptedBarrier.await();
        invocationDelayBarrier.open();
        assertThatThrownBy(result::await)
              .isExactlyInstanceOf(TimeoutException.class)
              .hasMessage("test invocation timed out");
        assertThat(timeoutWatcher.timeoutWatchWasCancelled()).isFalse();
    }

    @Test
    public void delayed_value_interruptedEarly() throws InterruptedException {
        Barrier invocationStartBarrier = Barrier.interruptible();
        Barrier invocationDelayBarrier = Barrier.interruptible();

        TestInvocation<String> invocation = TestInvocation.delayed(invocationStartBarrier, invocationDelayBarrier, () -> "foobar");
        TestThread<String> executingThread = runOnTestThread(new SyncTimeout<>(invocation, "test invocation", 1000, timeoutWatcher, null));
        invocationStartBarrier.await();
        executingThread.interrupt();
        assertThatThrownBy(executingThread::await).isExactlyInstanceOf(InterruptedException.class);
        assertThat(timeoutWatcher.timeoutWatchWasCancelled()).isTrue();
    }

    @Test
    public void delayed_exception_notTimedOut() {
        Barrier invocationDelayBarrier = Barrier.interruptible();

        TestInvocation<Void> invocation = TestInvocation.delayed(invocationDelayBarrier, TestException::doThrow);
        TestThread<Void> result = runOnTestThread(new SyncTimeout<>(invocation, "test invocation", 1000, timeoutWatcher, null));
        invocationDelayBarrier.open();
        assertThatThrownBy(result::await).isExactlyInstanceOf(TestException.class);
        assertThat(timeoutWatcher.timeoutWatchWasCancelled()).isTrue();
    }

    @Test
    public void delayed_exception_timedOut() throws InterruptedException {
        Barrier invocationDelayBarrier = Barrier.interruptible();

        TestInvocation<Void> invocation = TestInvocation.delayed(invocationDelayBarrier, TestException::doThrow);
        TestThread<Void> result = runOnTestThread(new SyncTimeout<>(invocation, "test invocation", 1000, timeoutWatcher, null));
        watcherTimeoutElapsedBarrier.open();
        watcherExecutionInterruptedBarrier.await();
        assertThatThrownBy(result::await)
              .isExactlyInstanceOf(TimeoutException.class)
              .hasMessage("test invocation timed out");
        assertThat(timeoutWatcher.timeoutWatchWasCancelled()).isFalse();
    }

    @Test
    public void delayed_exception_timedOutNoninterruptibly() throws InterruptedException {
        Barrier invocationDelayBarrier = Barrier.noninterruptible();

        TestInvocation<Void> invocation = TestInvocation.delayed(invocationDelayBarrier, TestException::doThrow);
        TestThread<Void> result = runOnTestThread(new SyncTimeout<>(invocation, "test invocation", 1000, timeoutWatcher, null));
        watcherTimeoutElapsedBarrier.open();
        watcherExecutionInterruptedBarrier.await();
        invocationDelayBarrier.open();
        assertThatThrownBy(result::await)
              .isExactlyInstanceOf(TimeoutException.class)
              .hasMessage("test invocation timed out");
        assertThat(timeoutWatcher.timeoutWatchWasCancelled()).isFalse();
    }

    @Test
    public void delayed_exception_interruptedEarly() throws InterruptedException {
        Barrier invocationStartBarrier = Barrier.interruptible();
        Barrier invocationDelayBarrier = Barrier.interruptible();

        TestInvocation<Void> invocation = TestInvocation.delayed(invocationStartBarrier, invocationDelayBarrier, TestException::doThrow);
        TestThread<Void> executingThread = runOnTestThread(new SyncTimeout<>(invocation, "test invocation", 1000, timeoutWatcher, null));
        invocationStartBarrier.await();
        executingThread.interrupt();
        assertThatThrownBy(executingThread::await).isExactlyInstanceOf(InterruptedException.class);
        assertThat(timeoutWatcher.timeoutWatchWasCancelled()).isTrue();
    }
}
