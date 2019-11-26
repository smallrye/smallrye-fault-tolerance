package com.github.ladicek.oaken_ocean.core.fallback;

import com.github.ladicek.oaken_ocean.core.util.TestException;
import com.github.ladicek.oaken_ocean.core.util.TestExecutor;
import com.github.ladicek.oaken_ocean.core.util.TestThread;
import com.github.ladicek.oaken_ocean.core.util.barrier.Barrier;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import static com.github.ladicek.oaken_ocean.core.util.CompletionStages.completedStage;
import static com.github.ladicek.oaken_ocean.core.util.CompletionStages.failedStage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CompletionStageFallbackTest {
    private TestExecutor testExecutor;

    @Before
    public void setUp() {
        testExecutor = new TestExecutor();
    }

    @Test
    public void immediatelyReturning_valueThenValue() throws Exception {
        TestAction<CompletionStage<String>> action = TestAction.immediatelyReturning(() -> completedStage("foobar"));
        TestThread<CompletionStage<String>> result = TestThread.runOnTestThread(new CompletionStageFallback<>(action, "test action", e -> completedStage("fallback"), testExecutor));
        assertThat(result.await().toCompletableFuture().get()).isEqualTo("foobar");
    }

    @Test
    public void immediatelyReturning_valueThenDirectException() throws Exception {
        TestAction<CompletionStage<String>> action = TestAction.immediatelyReturning(() -> completedStage("foobar"));
        TestThread<CompletionStage<String>> result = TestThread.runOnTestThread(new CompletionStageFallback<>(action, "test action", e -> TestException.doThrow(), testExecutor));
        assertThat(result.await().toCompletableFuture().get()).isEqualTo("foobar");
    }

    @Test
    public void immediatelyReturning_valueThenCompletionStageException() throws Exception {
        TestAction<CompletionStage<String>> action = TestAction.immediatelyReturning(() -> completedStage("foobar"));
        TestThread<CompletionStage<String>> result = TestThread.runOnTestThread(new CompletionStageFallback<>(action, "test action", e -> failedStage(new TestException()), testExecutor));
        assertThat(result.await().toCompletableFuture().get()).isEqualTo("foobar");
    }

    @Test
    public void immediatelyReturning_directExceptionThenValue() throws Exception {
        TestAction<CompletionStage<String>> action = TestAction.immediatelyReturning(TestException::doThrow);
        TestThread<CompletionStage<String>> result = TestThread.runOnTestThread(new CompletionStageFallback<>(action, "test action", e -> completedStage("fallback"), testExecutor));
        assertThat(result.await().toCompletableFuture().get()).isEqualTo("fallback");
    }

    @Test
    public void immediatelyReturning_completionStageExceptionThenValue() throws Exception {
        TestAction<CompletionStage<String>> action = TestAction.immediatelyReturning(() -> failedStage(new TestException()));
        TestThread<CompletionStage<String>> result = TestThread.runOnTestThread(new CompletionStageFallback<>(action, "test action", e -> completedStage("fallback"), testExecutor));
        assertThat(result.await().toCompletableFuture().get()).isEqualTo("fallback");
    }

    @Test
    public void immediatelyReturning_directExceptionThenDirectException() throws Exception {
        TestAction<CompletionStage<Void>> action = TestAction.immediatelyReturning(TestException::doThrow);
        TestThread<CompletionStage<Void>> result = TestThread.runOnTestThread(new CompletionStageFallback<>(action, "test action", e -> { throw new RuntimeException(); }, testExecutor));
        assertThatThrownBy(result.await().toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(RuntimeException.class);
    }

    @Test
    public void immediatelyReturning_directExceptionThenCompletionStageException() throws Exception {
        TestAction<CompletionStage<Void>> action = TestAction.immediatelyReturning(TestException::doThrow);
        TestThread<CompletionStage<Void>> result = TestThread.runOnTestThread(new CompletionStageFallback<>(action, "test action", e -> failedStage(new RuntimeException()), testExecutor));
        assertThatThrownBy(result.await().toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(RuntimeException.class);
    }

    @Test
    public void immediatelyReturning_completionStageExceptionThenDirectException() throws Exception {
        TestAction<CompletionStage<Void>> action = TestAction.immediatelyReturning(() -> failedStage(new TestException()));
        TestThread<CompletionStage<Void>> result = TestThread.runOnTestThread(new CompletionStageFallback<>(action, "test action", e -> { throw new RuntimeException(); }, testExecutor));
        assertThatThrownBy(result.await().toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(RuntimeException.class);
    }

    @Test
    public void immediatelyReturning_completionStageExceptionThenCompletionStageException() throws Exception {
        TestAction<CompletionStage<Void>> action = TestAction.immediatelyReturning(() -> failedStage(new TestException()));
        TestThread<CompletionStage<Void>> result = TestThread.runOnTestThread(new CompletionStageFallback<>(action, "test action", e -> failedStage(new RuntimeException()), testExecutor));
        assertThatThrownBy(result.await().toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(RuntimeException.class);
    }

    // testing interruption and especially self-interruption isn't exactly meaningful,
    // the tests just codify existing behavior

    @Test
    public void waitingOnBarrier_interruptedInAction() throws Exception {
        Barrier startBarrier = Barrier.interruptible();
        Barrier endBarrier = Barrier.interruptible();
        TestAction<CompletionStage<String>> action = TestAction.waitingOnBarrier(startBarrier, endBarrier, () -> completedStage("foobar"));
        TestThread<CompletionStage<String>> executingThread = TestThread.runOnTestThread(new CompletionStageFallback<>(action, "test action", e -> completedStage("fallback"), testExecutor));
        startBarrier.await();
        testExecutor.interruptExecutingThread();
        assertThatThrownBy(executingThread.await().toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(InterruptedException.class);
    }

    @Test
    public void waitingOnBarrier_interruptedInFallback_directException() throws Exception {
        TestAction<CompletionStage<String>> action = TestAction.immediatelyReturning(TestException::doThrow);
        Barrier startBarrier = Barrier.interruptible();
        Barrier endBarrier = Barrier.interruptible();
        FallbackFunction<CompletionStage<String>> fallback = e -> {
            startBarrier.open();
            endBarrier.await();
            return completedStage("fallback");
        };
        TestThread<CompletionStage<String>> executingThread = TestThread.runOnTestThread(new CompletionStageFallback<>(action, "test action", fallback, testExecutor));
        startBarrier.await();
        testExecutor.interruptExecutingThread();
        assertThatThrownBy(executingThread.await().toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(InterruptedException.class);
    }

    @Test
    public void waitingOnBarrier_interruptedInFallback_completionStageException() throws Exception {
        TestAction<CompletionStage<String>> action = TestAction.immediatelyReturning(() -> failedStage(new TestException()));
        Barrier startBarrier = Barrier.interruptible();
        Barrier endBarrier = Barrier.interruptible();
        FallbackFunction<CompletionStage<String>> fallback = e -> {
            startBarrier.open();
            endBarrier.await();
            return completedStage("fallback");
        };
        TestThread<CompletionStage<String>> executingThread = TestThread.runOnTestThread(new CompletionStageFallback<>(action, "test action", fallback, testExecutor));
        startBarrier.await();
        testExecutor.interruptExecutingThread();
        assertThatThrownBy(executingThread.await().toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(InterruptedException.class);
    }

    @Test
    public void selfInterruptedInAction_value() throws Exception {
        Callable<CompletionStage<String>> action = () -> {
            Thread.currentThread().interrupt();
            return completedStage("foobar");
        };
        TestThread<CompletionStage<String>> result = TestThread.runOnTestThread(new CompletionStageFallback<>(action, "test action", e -> completedStage("fallback"), testExecutor));
        assertThat(result.await().toCompletableFuture().get()).isEqualTo("foobar");
    }

    @Test
    public void selfInterruptedInAction_directException() throws Exception {
        Callable<CompletionStage<String>> action = () -> {
            Thread.currentThread().interrupt();
            throw new RuntimeException();
        };
        TestThread<CompletionStage<String>> executingThread = TestThread.runOnTestThread(new CompletionStageFallback<>(action, "test action", e -> completedStage("fallback"), testExecutor));
        assertThatThrownBy(executingThread.await().toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(InterruptedException.class);
    }

    @Test
    public void selfInterruptedInAction_completionStageException() throws Exception {
        Callable<CompletionStage<String>> action = () -> {
            Thread.currentThread().interrupt();
            return failedStage(new RuntimeException());
        };
        TestThread<CompletionStage<String>> executingThread = TestThread.runOnTestThread(new CompletionStageFallback<>(action, "test action", e -> completedStage("fallback"), testExecutor));
        assertThatThrownBy(executingThread.await().toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(InterruptedException.class);
    }

    @Test
    public void selfInterruptedInFallback_value() throws Exception {
        TestAction<CompletionStage<String>> action = TestAction.immediatelyReturning(TestException::doThrow);
        FallbackFunction<CompletionStage<String>> fallback = e -> {
            Thread.currentThread().interrupt();
            return completedStage("fallback");
        };
        TestThread<CompletionStage<String>> result = TestThread.runOnTestThread(new CompletionStageFallback<>(action, "test action", fallback, testExecutor));
        assertThat(result.await().toCompletableFuture().get()).isEqualTo("fallback");
    }

    @Test
    public void selfInterruptedInFallback_directException() throws Exception {
        TestAction<CompletionStage<String>> action = TestAction.immediatelyReturning(TestException::doThrow);
        FallbackFunction<CompletionStage<String>> fallback = e -> {
            Thread.currentThread().interrupt();
            throw new RuntimeException();
        };
        TestThread<CompletionStage<String>> executingThread = TestThread.runOnTestThread(new CompletionStageFallback<>(action, "test action", fallback, testExecutor));
        assertThatThrownBy(executingThread.await().toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(RuntimeException.class);
    }

    @Test
    public void selfInterruptedInFallback_completionStageException() throws Exception {
        TestAction<CompletionStage<String>> action = TestAction.immediatelyReturning(TestException::doThrow);
        FallbackFunction<CompletionStage<String>> fallback = e -> {
            Thread.currentThread().interrupt();
            return failedStage(new RuntimeException());
        };
        TestThread<CompletionStage<String>> executingThread = TestThread.runOnTestThread(new CompletionStageFallback<>(action, "test action", fallback, testExecutor));
        assertThatThrownBy(executingThread.await().toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(RuntimeException.class);
    }
}
