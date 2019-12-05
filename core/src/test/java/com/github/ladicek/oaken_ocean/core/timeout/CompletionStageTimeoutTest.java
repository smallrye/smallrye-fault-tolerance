package com.github.ladicek.oaken_ocean.core.timeout;

import static com.github.ladicek.oaken_ocean.core.util.CompletionStages.completedStage;
import static com.github.ladicek.oaken_ocean.core.util.CompletionStages.failedStage;
import static com.github.ladicek.oaken_ocean.core.util.TestThread.runOnTestThread;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.junit.Before;
import org.junit.Test;

import com.github.ladicek.oaken_ocean.core.util.TestException;
import com.github.ladicek.oaken_ocean.core.util.TestExecutor;
import com.github.ladicek.oaken_ocean.core.util.TestThread;
import com.github.ladicek.oaken_ocean.core.util.barrier.Barrier;

public class CompletionStageTimeoutTest {
    private Barrier watcherTimeoutElapsedBarrier;
    private Barrier watcherExecutionInterruptedBarrier;

    private TestTimeoutWatcher timeoutWatcher;

    private TestExecutor testExecutor;

    @Before
    public void setUp() {
        watcherTimeoutElapsedBarrier = Barrier.interruptible();
        watcherExecutionInterruptedBarrier = Barrier.interruptible();

        timeoutWatcher = new TestTimeoutWatcher(watcherTimeoutElapsedBarrier, watcherExecutionInterruptedBarrier);

        testExecutor = new TestExecutor();
    }

    @Test
    public void negativeTimeout() {
        TestInvocation<CompletionStage<String>> invocation = TestInvocation
                .immediatelyReturning(() -> completedStage("foobar"));
        assertThatThrownBy(
                () -> new CompletionStageTimeout<>(invocation, "test invocation", -1, timeoutWatcher, testExecutor, null))
                        .isExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void zeroTimeout() {
        TestInvocation<CompletionStage<String>> invocation = TestInvocation
                .immediatelyReturning(() -> completedStage("foobar"));
        assertThatThrownBy(
                () -> new CompletionStageTimeout<>(invocation, "test invocation", 0, timeoutWatcher, testExecutor, null))
                        .isExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void immediatelyReturning_value() throws Exception {
        TestInvocation<CompletionStage<String>> invocation = TestInvocation
                .immediatelyReturning(() -> completedStage("foobar"));
        TestThread<CompletionStage<String>> result = runOnTestThread(
                new CompletionStageTimeout<>(invocation, "test invocation", 1000, timeoutWatcher, testExecutor, null));
        assertThat(result.await().toCompletableFuture().get()).isEqualTo("foobar");
        assertThat(timeoutWatcher.timeoutWatchWasCancelled()).isTrue();
    }

    @Test
    public void immediatelyReturning_directException() throws Exception {
        TestInvocation<CompletionStage<Void>> invocation = TestInvocation.immediatelyReturning(TestException::doThrow);
        TestThread<CompletionStage<Void>> result = runOnTestThread(
                new CompletionStageTimeout<>(invocation, "test invocation", 1000, timeoutWatcher, testExecutor, null));
        assertThatThrownBy(result.await().toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(TestException.class);
        assertThat(timeoutWatcher.timeoutWatchWasCancelled()).isTrue();
    }

    @Test
    public void immediatelyReturning_completionStageException() throws Exception {
        TestInvocation<CompletionStage<Void>> invocation = TestInvocation
                .immediatelyReturning(() -> failedStage(new TestException()));
        TestThread<CompletionStage<Void>> result = runOnTestThread(
                new CompletionStageTimeout<>(invocation, "test invocation", 1000, timeoutWatcher, testExecutor, null));
        assertThatThrownBy(result.await().toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(TestException.class);
        assertThat(timeoutWatcher.timeoutWatchWasCancelled()).isTrue();
    }

    @Test
    public void delayed_value_notTimedOut() throws Exception {
        Barrier invocationDelayBarrier = Barrier.interruptible();

        TestInvocation<CompletionStage<String>> invocation = TestInvocation.delayed(invocationDelayBarrier,
                () -> completedStage("foobar"));
        TestThread<CompletionStage<String>> result = runOnTestThread(
                new CompletionStageTimeout<>(invocation, "test invocation", 1000, timeoutWatcher, testExecutor, null));
        invocationDelayBarrier.open();
        assertThat(result.await().toCompletableFuture().get()).isEqualTo("foobar");
        assertThat(timeoutWatcher.timeoutWatchWasCancelled()).isTrue();
    }

    @Test
    public void delayed_value_timedOut() throws Exception {
        Barrier invocationDelayBarrier = Barrier.interruptible();

        TestInvocation<CompletionStage<String>> invocation = TestInvocation.delayed(invocationDelayBarrier,
                () -> completedStage("foobar"));
        TestThread<CompletionStage<String>> result = runOnTestThread(
                new CompletionStageTimeout<>(invocation, "test invocation", 1000, timeoutWatcher, testExecutor, null));
        watcherTimeoutElapsedBarrier.open();
        watcherExecutionInterruptedBarrier.await();
        assertThatThrownBy(result.await().toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(TimeoutException.class)
                .hasMessageContaining("test invocation timed out");
        assertThat(timeoutWatcher.timeoutWatchWasCancelled()).isFalse();
    }

    @Test
    public void delayed_value_timedOutNoninterruptibly() throws Exception {
        Barrier invocationDelayBarrier = Barrier.noninterruptible();

        TestInvocation<CompletionStage<String>> invocation = TestInvocation.delayed(invocationDelayBarrier,
                () -> completedStage("foobar"));
        TestThread<CompletionStage<String>> result = runOnTestThread(
                new CompletionStageTimeout<>(invocation, "test invocation", 1000, timeoutWatcher, testExecutor, null));
        watcherTimeoutElapsedBarrier.open();
        watcherExecutionInterruptedBarrier.await();
        invocationDelayBarrier.open();
        assertThatThrownBy(result.await().toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(TimeoutException.class)
                .hasMessageContaining("test invocation timed out");
        assertThat(timeoutWatcher.timeoutWatchWasCancelled()).isFalse();
    }

    @Test
    public void delayed_value_interruptedEarly() throws Exception {
        Barrier invocationStartBarrier = Barrier.interruptible();
        Barrier invocationDelayBarrier = Barrier.interruptible();

        TestInvocation<CompletionStage<String>> invocation = TestInvocation.delayed(invocationStartBarrier,
                invocationDelayBarrier, () -> completedStage("foobar"));
        TestThread<CompletionStage<String>> result = runOnTestThread(
                new CompletionStageTimeout<>(invocation, "test invocation", 1000, timeoutWatcher, testExecutor, null));
        invocationStartBarrier.await();
        testExecutor.interruptExecutingThread();
        assertThatThrownBy(result.await().toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(InterruptedException.class);
        assertThat(timeoutWatcher.timeoutWatchWasCancelled()).isTrue();
    }

    @Test
    public void delayed_exception_notTimedOut() throws Exception {
        Barrier invocationDelayBarrier = Barrier.interruptible();

        TestInvocation<CompletionStage<Void>> invocation = TestInvocation.delayed(invocationDelayBarrier,
                () -> failedStage(new TestException()));
        TestThread<CompletionStage<Void>> result = runOnTestThread(
                new CompletionStageTimeout<>(invocation, "test invocation", 1000, timeoutWatcher, testExecutor, null));
        invocationDelayBarrier.open();
        assertThatThrownBy(result.await().toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(TestException.class);
        assertThat(timeoutWatcher.timeoutWatchWasCancelled()).isTrue();
    }

    @Test
    public void delayed_exception_timedOut() throws Exception {
        Barrier invocationDelayBarrier = Barrier.interruptible();

        TestInvocation<CompletionStage<Void>> invocation = TestInvocation.delayed(invocationDelayBarrier,
                () -> failedStage(new TestException()));
        TestThread<CompletionStage<Void>> result = runOnTestThread(
                new CompletionStageTimeout<>(invocation, "test invocation", 1000, timeoutWatcher, testExecutor, null));
        watcherTimeoutElapsedBarrier.open();
        watcherExecutionInterruptedBarrier.await();
        assertThatThrownBy(result.await().toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(TimeoutException.class)
                .hasMessageContaining("test invocation timed out");
        assertThat(timeoutWatcher.timeoutWatchWasCancelled()).isFalse();
    }

    @Test
    public void delayed_exception_timedOutNoninterruptibly() throws Exception {
        Barrier invocationDelayBarrier = Barrier.noninterruptible();

        TestInvocation<CompletionStage<Void>> invocation = TestInvocation.delayed(invocationDelayBarrier,
                () -> failedStage(new TestException()));
        TestThread<CompletionStage<Void>> result = runOnTestThread(
                new CompletionStageTimeout<>(invocation, "test invocation", 1000, timeoutWatcher, testExecutor, null));
        watcherTimeoutElapsedBarrier.open();
        watcherExecutionInterruptedBarrier.await();
        invocationDelayBarrier.open();
        assertThatThrownBy(result.await().toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(TimeoutException.class)
                .hasMessageContaining("test invocation timed out");
        assertThat(timeoutWatcher.timeoutWatchWasCancelled()).isFalse();
    }

    @Test
    public void delayed_exception_interruptedEarly() throws Exception {
        Barrier invocationStartBarrier = Barrier.interruptible();
        Barrier invocationDelayBarrier = Barrier.interruptible();

        TestInvocation<CompletionStage<Void>> invocation = TestInvocation.delayed(invocationStartBarrier,
                invocationDelayBarrier, () -> failedStage(new TestException()));
        TestThread<CompletionStage<Void>> result = runOnTestThread(
                new CompletionStageTimeout<>(invocation, "test invocation", 1000, timeoutWatcher, testExecutor, null));
        invocationStartBarrier.await();
        testExecutor.interruptExecutingThread();
        assertThatThrownBy(result.await().toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(InterruptedException.class);
        assertThat(timeoutWatcher.timeoutWatchWasCancelled()).isTrue();
    }

    @Test
    public void immediatelyReturning_completionStageTimedOut() throws Exception {
        Barrier barrier = Barrier.interruptible();

        TestInvocation<CompletionStage<Void>> invocation = TestInvocation.immediatelyReturning(
                () -> CompletableFuture.supplyAsync(() -> {
                    try {
                        barrier.await();
                    } catch (InterruptedException e) {
                        throw new RuntimeException("interrupted");
                    }
                    return null;
                }));
        TestThread<CompletionStage<Void>> result = runOnTestThread(
                new CompletionStageTimeout<>(invocation, "test invocation", 1000, timeoutWatcher, testExecutor, null));
        watcherTimeoutElapsedBarrier.open();
        watcherExecutionInterruptedBarrier.await();
        assertThatThrownBy(result.await().toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(TimeoutException.class);
        barrier.open();
    }
}
