package com.github.ladicek.oaken_ocean.core.timeout;

import com.github.ladicek.oaken_ocean.core.util.TestException;
import com.github.ladicek.oaken_ocean.core.util.TestThread;
import com.github.ladicek.oaken_ocean.core.util.barrier.Barrier;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.junit.Before;
import org.junit.Test;

import static com.github.ladicek.oaken_ocean.core.util.TestThread.runOnTestThread;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TimeoutTest {
    private Barrier watcherTimeoutElapsedBarrier;
    private Barrier watcherExecutionInterruptedBarrier;

    private TestTimeoutWatcher timeoutWatcher;

    @Before
    public void setUp() {
        watcherTimeoutElapsedBarrier = Barrier.interruptible();
        watcherExecutionInterruptedBarrier = Barrier.interruptible();

        timeoutWatcher = new TestTimeoutWatcher(watcherTimeoutElapsedBarrier, watcherExecutionInterruptedBarrier);
    }

    @Test
    public void negativeTimeout() {
        TestInvocation<String> invocation = TestInvocation.immediatelyReturning(() -> "foobar");
        assertThatThrownBy(() -> new Timeout<>(invocation, "test invocation", -1, timeoutWatcher))
                .isExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void zeroTimeout() {
        TestInvocation<String> invocation = TestInvocation.immediatelyReturning(() -> "foobar");
        assertThatThrownBy(() -> new Timeout<>(invocation, "test invocation", 0, timeoutWatcher))
                .isExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void immediatelyReturning_value() throws Exception {
        TestInvocation<String> invocation = TestInvocation.immediatelyReturning(() -> "foobar");
        TestThread<String> result = runOnTestThread(new Timeout<>(invocation, "test invocation", 1000, timeoutWatcher));
        assertThat(result.await()).isEqualTo("foobar");
        assertThat(timeoutWatcher.timeoutWatchWasCancelled()).isTrue();
    }

    @Test
    public void immediatelyReturning_exception() {
        TestInvocation<Void> invocation = TestInvocation.immediatelyReturning(TestException::doThrow);
        TestThread<Void> result = runOnTestThread(new Timeout<>(invocation, "test invocation", 1000, timeoutWatcher));
        assertThatThrownBy(result::await).isExactlyInstanceOf(TestException.class);
        assertThat(timeoutWatcher.timeoutWatchWasCancelled()).isTrue();
    }

    @Test
    public void delayed_value_notTimedOut() throws Exception {
        Barrier invocationDelayBarrier = Barrier.interruptible();

        TestInvocation<String> invocation = TestInvocation.delayed(invocationDelayBarrier, () -> "foobar");
        TestThread<String> result = runOnTestThread(new Timeout<>(invocation, "test invocation", 1000, timeoutWatcher));
        invocationDelayBarrier.open();
        assertThat(result.await()).isEqualTo("foobar");
        assertThat(timeoutWatcher.timeoutWatchWasCancelled()).isTrue();
    }

    @Test
    public void delayed_value_timedOut() throws InterruptedException {
        Barrier invocationDelayBarrier = Barrier.interruptible();

        TestInvocation<String> invocation = TestInvocation.delayed(invocationDelayBarrier, () -> "foobar");
        TestThread<String> result = runOnTestThread(new Timeout<>(invocation, "test invocation", 1000, timeoutWatcher));
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
        TestThread<String> result = runOnTestThread(new Timeout<>(invocation, "test invocation", 1000, timeoutWatcher));
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
        TestThread<String> executingThread = runOnTestThread(new Timeout<>(invocation, "test invocation", 1000, timeoutWatcher));
        invocationStartBarrier.await();
        executingThread.interrupt();
        assertThatThrownBy(executingThread::await).isExactlyInstanceOf(InterruptedException.class);
        assertThat(timeoutWatcher.timeoutWatchWasCancelled()).isTrue();
    }

    @Test
    public void delayed_exception_notTimedOut() {
        Barrier invocationDelayBarrier = Barrier.interruptible();

        TestInvocation<Void> invocation = TestInvocation.delayed(invocationDelayBarrier, TestException::doThrow);
        TestThread<Void> result = runOnTestThread(new Timeout<>(invocation, "test invocation", 1000, timeoutWatcher));
        invocationDelayBarrier.open();
        assertThatThrownBy(result::await).isExactlyInstanceOf(TestException.class);
        assertThat(timeoutWatcher.timeoutWatchWasCancelled()).isTrue();
    }

    @Test
    public void delayed_exception_timedOut() throws InterruptedException {
        Barrier invocationDelayBarrier = Barrier.interruptible();

        TestInvocation<Void> invocation = TestInvocation.delayed(invocationDelayBarrier, TestException::doThrow);
        TestThread<Void> result = runOnTestThread(new Timeout<>(invocation, "test invocation", 1000, timeoutWatcher));
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
        TestThread<Void> result = runOnTestThread(new Timeout<>(invocation, "test invocation", 1000, timeoutWatcher));
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
        TestThread<Void> executingThread = runOnTestThread(new Timeout<>(invocation, "test invocation", 1000, timeoutWatcher));
        invocationStartBarrier.await();
        executingThread.interrupt();
        assertThatThrownBy(executingThread::await).isExactlyInstanceOf(InterruptedException.class);
        assertThat(timeoutWatcher.timeoutWatchWasCancelled()).isTrue();
    }
}
