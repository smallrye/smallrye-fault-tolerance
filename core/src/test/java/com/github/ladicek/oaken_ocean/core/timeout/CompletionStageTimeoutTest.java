package com.github.ladicek.oaken_ocean.core.timeout;

import com.github.ladicek.oaken_ocean.core.util.TestException;
import com.github.ladicek.oaken_ocean.core.util.TestThread;
import com.github.ladicek.oaken_ocean.core.util.barrier.Barrier;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import static com.github.ladicek.oaken_ocean.core.util.CompletionStages.completedStage;
import static com.github.ladicek.oaken_ocean.core.util.CompletionStages.failedStage;
import static com.github.ladicek.oaken_ocean.core.util.TestThread.runOnTestThread;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        TestAction<CompletionStage<String>> action = TestAction.immediatelyReturning(() -> completedStage("foobar"));
        assertThatThrownBy(() -> new CompletionStageTimeout<>(action, "test action", -1, timeoutWatcher, testExecutor))
                .isExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void zeroTimeout() {
        TestAction<CompletionStage<String>> action = TestAction.immediatelyReturning(() -> completedStage("foobar"));
        assertThatThrownBy(() -> new CompletionStageTimeout<>(action, "test action", 0, timeoutWatcher, testExecutor))
                .isExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void immediatelyReturning_value() throws Exception {
        TestAction<CompletionStage<String>> action = TestAction.immediatelyReturning(() -> completedStage("foobar"));
        TestThread<CompletionStage<String>> result = runOnTestThread(new CompletionStageTimeout<>(action, "test action", 1000, timeoutWatcher, testExecutor));
        assertThat(result.await().toCompletableFuture().get()).isEqualTo("foobar");
        assertThat(timeoutWatcher.timeoutWatchWasCancelled()).isTrue();
    }

    @Test
    public void immediatelyReturning_directException() throws Exception {
        TestAction<CompletionStage<Void>> action = TestAction.immediatelyReturning(TestException::doThrow);
        TestThread<CompletionStage<Void>> result = runOnTestThread(new CompletionStageTimeout<>(action, "test action", 1000, timeoutWatcher, testExecutor));
        assertThatThrownBy(result.await().toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(TestException.class);
        assertThat(timeoutWatcher.timeoutWatchWasCancelled()).isTrue();
    }

    @Test
    public void immediatelyReturning_completionStageException() throws Exception {
        TestAction<CompletionStage<Void>> action = TestAction.immediatelyReturning(() -> failedStage(new TestException()));
        TestThread<CompletionStage<Void>> result = runOnTestThread(new CompletionStageTimeout<>(action, "test action", 1000, timeoutWatcher, testExecutor));
        assertThatThrownBy(result.await().toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(TestException.class);
        assertThat(timeoutWatcher.timeoutWatchWasCancelled()).isTrue();
    }

    @Test
    public void delayed_value_notTimedOut() throws Exception {
        Barrier actionDelayBarrier = Barrier.interruptible();

        TestAction<CompletionStage<String>> action = TestAction.delayed(actionDelayBarrier, () -> completedStage("foobar"));
        TestThread<CompletionStage<String>> result = runOnTestThread(new CompletionStageTimeout<>(action, "test action", 1000, timeoutWatcher, testExecutor));
        actionDelayBarrier.open();
        assertThat(result.await().toCompletableFuture().get()).isEqualTo("foobar");
        assertThat(timeoutWatcher.timeoutWatchWasCancelled()).isTrue();
    }

    @Test
    public void delayed_value_timedOut() throws Exception {
        Barrier actionDelayBarrier = Barrier.interruptible();

        TestAction<CompletionStage<String>> action = TestAction.delayed(actionDelayBarrier, () -> completedStage("foobar"));
        TestThread<CompletionStage<String>> result = runOnTestThread(new CompletionStageTimeout<>(action, "test action", 1000, timeoutWatcher, testExecutor));
        watcherTimeoutElapsedBarrier.open();
        watcherExecutionInterruptedBarrier.await();
        assertThatThrownBy(result.await().toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(TimeoutException.class)
                .hasMessageContaining("test action timed out");
        assertThat(timeoutWatcher.timeoutWatchWasCancelled()).isFalse();
    }

    @Test
    public void delayed_value_timedOutNoninterruptibly() throws Exception {
        Barrier actionDelayBarrier = Barrier.noninterruptible();

        TestAction<CompletionStage<String>> action = TestAction.delayed(actionDelayBarrier, () -> completedStage("foobar"));
        TestThread<CompletionStage<String>> result = runOnTestThread(new CompletionStageTimeout<>(action, "test action", 1000, timeoutWatcher, testExecutor));
        watcherTimeoutElapsedBarrier.open();
        watcherExecutionInterruptedBarrier.await();
        actionDelayBarrier.open();
        assertThatThrownBy(result.await().toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(TimeoutException.class)
                .hasMessageContaining("test action timed out");
        assertThat(timeoutWatcher.timeoutWatchWasCancelled()).isFalse();
    }

    @Test
    public void delayed_value_interruptedEarly() throws Exception {
        Barrier actionStartBarrier = Barrier.interruptible();
        Barrier actionDelayBarrier = Barrier.interruptible();

        TestAction<CompletionStage<String>> action = TestAction.delayed(actionStartBarrier, actionDelayBarrier, () -> completedStage("foobar"));
        TestThread<CompletionStage<String>> result = runOnTestThread(new CompletionStageTimeout<>(action, "test action", 1000, timeoutWatcher, testExecutor));
        actionStartBarrier.await();
        testExecutor.interruptExecutingThread();
        assertThatThrownBy(result.await().toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(InterruptedException.class);
        assertThat(timeoutWatcher.timeoutWatchWasCancelled()).isTrue();
    }

    @Test
    public void delayed_exception_notTimedOut() throws Exception {
        Barrier actionDelayBarrier = Barrier.interruptible();

        TestAction<CompletionStage<Void>> action = TestAction.delayed(actionDelayBarrier, () -> failedStage(new TestException()));
        TestThread<CompletionStage<Void>> result = runOnTestThread(new CompletionStageTimeout<>(action, "test action", 1000, timeoutWatcher, testExecutor));
        actionDelayBarrier.open();
        assertThatThrownBy(result.await().toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(TestException.class);
        assertThat(timeoutWatcher.timeoutWatchWasCancelled()).isTrue();
    }

    @Test
    public void delayed_exception_timedOut() throws Exception {
        Barrier actionDelayBarrier = Barrier.interruptible();

        TestAction<CompletionStage<Void>> action = TestAction.delayed(actionDelayBarrier, () -> failedStage(new TestException()));
        TestThread<CompletionStage<Void>> result = runOnTestThread(new CompletionStageTimeout<>(action, "test action", 1000, timeoutWatcher, testExecutor));
        watcherTimeoutElapsedBarrier.open();
        watcherExecutionInterruptedBarrier.await();
        assertThatThrownBy(result.await().toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(TimeoutException.class)
                .hasMessageContaining("test action timed out");
        assertThat(timeoutWatcher.timeoutWatchWasCancelled()).isFalse();
    }

    @Test
    public void delayed_exception_timedOutNoninterruptibly() throws Exception {
        Barrier actionDelayBarrier = Barrier.noninterruptible();

        TestAction<CompletionStage<Void>> action = TestAction.delayed(actionDelayBarrier, () -> failedStage(new TestException()));
        TestThread<CompletionStage<Void>> result = runOnTestThread(new CompletionStageTimeout<>(action, "test action", 1000, timeoutWatcher, testExecutor));
        watcherTimeoutElapsedBarrier.open();
        watcherExecutionInterruptedBarrier.await();
        actionDelayBarrier.open();
        assertThatThrownBy(result.await().toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(TimeoutException.class)
                .hasMessageContaining("test action timed out");
        assertThat(timeoutWatcher.timeoutWatchWasCancelled()).isFalse();
    }

    @Test
    public void delayed_exception_interruptedEarly() throws Exception {
        Barrier actionStartBarrier = Barrier.interruptible();
        Barrier actionDelayBarrier = Barrier.interruptible();

        TestAction<CompletionStage<Void>> action = TestAction.delayed(actionStartBarrier, actionDelayBarrier, () -> failedStage(new TestException()));
        TestThread<CompletionStage<Void>> result = runOnTestThread(new CompletionStageTimeout<>(action, "test action", 1000, timeoutWatcher, testExecutor));
        actionStartBarrier.await();
        testExecutor.interruptExecutingThread();
        assertThatThrownBy(result.await().toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(InterruptedException.class);
        assertThat(timeoutWatcher.timeoutWatchWasCancelled()).isTrue();
    }
}
