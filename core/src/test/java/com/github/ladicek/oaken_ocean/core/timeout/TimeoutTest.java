package com.github.ladicek.oaken_ocean.core.timeout;

import com.github.ladicek.oaken_ocean.core.util.TestException;
import com.github.ladicek.oaken_ocean.core.util.barrier.Barrier;
import com.github.ladicek.oaken_ocean.core.util.TestThread;
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
        TestAction<String> action = TestAction.immediatelyReturning(() -> "foobar");
        assertThatThrownBy(() -> new Timeout<>(action, "test action", -1, timeoutWatcher))
                .isExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void immediatelyReturning_value() throws Exception {
        TestAction<String> action = TestAction.immediatelyReturning(() -> "foobar");
        TestThread<String> result = runOnTestThread(new Timeout<>(action, "test action", 1000, timeoutWatcher));
        assertThat(result.await()).isEqualTo("foobar");
        assertThat(timeoutWatcher.timeoutWatchWasCancelled()).isTrue();
    }

    @Test
    public void immediatelyReturning_exception() {
        TestAction<Void> action = TestAction.immediatelyReturning(TestException::doThrow);
        TestThread<Void> result = runOnTestThread(new Timeout<>(action, "test action", 1000, timeoutWatcher));
        assertThatThrownBy(result::await).isExactlyInstanceOf(TestException.class);
        assertThat(timeoutWatcher.timeoutWatchWasCancelled()).isTrue();
    }

    @Test
    public void delayed_value_notTimedOut() throws Exception {
        Barrier actionDelayBarrier = Barrier.interruptible();

        TestAction<String> action = TestAction.delayed(actionDelayBarrier, () -> "foobar");
        TestThread<String> result = runOnTestThread(new Timeout<>(action, "test action", 1000, timeoutWatcher));
        actionDelayBarrier.open();
        assertThat(result.await()).isEqualTo("foobar");
        assertThat(timeoutWatcher.timeoutWatchWasCancelled()).isTrue();
    }

    @Test
    public void delayed_value_timedOut() throws InterruptedException {
        Barrier actionDelayBarrier = Barrier.interruptible();

        TestAction<String> action = TestAction.delayed(actionDelayBarrier, () -> "foobar");
        TestThread<String> result = runOnTestThread(new Timeout<>(action, "test action", 1000, timeoutWatcher));
        watcherTimeoutElapsedBarrier.open();
        watcherExecutionInterruptedBarrier.await();
        assertThatThrownBy(result::await)
                .isExactlyInstanceOf(TimeoutException.class)
                .hasMessage("test action timed out");
        assertThat(timeoutWatcher.timeoutWatchWasCancelled()).isFalse();
    }

    @Test
    public void delayed_value_timedOutNoninterruptibly() throws InterruptedException {
        Barrier actionDelayBarrier = Barrier.noninterruptible();

        TestAction<String> action = TestAction.delayed(actionDelayBarrier, () -> "foobar");
        TestThread<String> result = runOnTestThread(new Timeout<>(action, "test action", 1000, timeoutWatcher));
        watcherTimeoutElapsedBarrier.open();
        watcherExecutionInterruptedBarrier.await();
        actionDelayBarrier.open();
        assertThatThrownBy(result::await)
                .isExactlyInstanceOf(TimeoutException.class)
                .hasMessage("test action timed out");
        assertThat(timeoutWatcher.timeoutWatchWasCancelled()).isFalse();
    }

    @Test
    public void delayed_value_interruptedEarly() throws InterruptedException {
        Barrier actionStartBarrier = Barrier.interruptible();
        Barrier actionDelayBarrier = Barrier.interruptible();

        TestAction<String> action = TestAction.delayed(actionStartBarrier, actionDelayBarrier, () -> "foobar");
        TestThread<String> executingThread = runOnTestThread(new Timeout<>(action, "test action", 1000, timeoutWatcher));
        actionStartBarrier.await();
        executingThread.interrupt();
        assertThatThrownBy(executingThread::await).isExactlyInstanceOf(InterruptedException.class);
        assertThat(timeoutWatcher.timeoutWatchWasCancelled()).isTrue();
    }

    @Test
    public void delayed_exception_notTimedOut() {
        Barrier actionDelayBarrier = Barrier.interruptible();

        TestAction<Void> action = TestAction.delayed(actionDelayBarrier, TestException::doThrow);
        TestThread<Void> result = runOnTestThread(new Timeout<>(action, "test action", 1000, timeoutWatcher));
        actionDelayBarrier.open();
        assertThatThrownBy(result::await).isExactlyInstanceOf(TestException.class);
        assertThat(timeoutWatcher.timeoutWatchWasCancelled()).isTrue();
    }

    @Test
    public void delayed_exception_timedOut() throws InterruptedException {
        Barrier actionDelayBarrier = Barrier.interruptible();

        TestAction<Void> action = TestAction.delayed(actionDelayBarrier, TestException::doThrow);
        TestThread<Void> result = runOnTestThread(new Timeout<>(action, "test action", 1000, timeoutWatcher));
        watcherTimeoutElapsedBarrier.open();
        watcherExecutionInterruptedBarrier.await();
        assertThatThrownBy(result::await)
                .isExactlyInstanceOf(TimeoutException.class)
                .hasMessage("test action timed out");
        assertThat(timeoutWatcher.timeoutWatchWasCancelled()).isFalse();
    }

    @Test
    public void delayed_exception_timedOutNoninterruptibly() throws InterruptedException {
        Barrier actionDelayBarrier = Barrier.noninterruptible();

        TestAction<Void> action = TestAction.delayed(actionDelayBarrier, TestException::doThrow);
        TestThread<Void> result = runOnTestThread(new Timeout<>(action, "test action", 1000, timeoutWatcher));
        watcherTimeoutElapsedBarrier.open();
        watcherExecutionInterruptedBarrier.await();
        actionDelayBarrier.open();
        assertThatThrownBy(result::await)
                .isExactlyInstanceOf(TimeoutException.class)
                .hasMessage("test action timed out");
        assertThat(timeoutWatcher.timeoutWatchWasCancelled()).isFalse();
    }

    @Test
    public void delayed_exception_interruptedEarly() throws InterruptedException {
        Barrier actionStartBarrier = Barrier.interruptible();
        Barrier actionDelayBarrier = Barrier.interruptible();

        TestAction<Void> action = TestAction.delayed(actionStartBarrier, actionDelayBarrier, TestException::doThrow);
        TestThread<Void> executingThread = runOnTestThread(new Timeout<>(action, "test action", 1000, timeoutWatcher));
        actionStartBarrier.await();
        executingThread.interrupt();
        assertThatThrownBy(executingThread::await).isExactlyInstanceOf(InterruptedException.class);
        assertThat(timeoutWatcher.timeoutWatchWasCancelled()).isTrue();
    }
}
