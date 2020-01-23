package io.smallrye.faulttolerance.core.fallback;

import static io.smallrye.faulttolerance.core.util.CompletionStages.completedStage;
import static io.smallrye.faulttolerance.core.util.CompletionStages.failedStage;
import static io.smallrye.faulttolerance.core.util.TestThread.runOnTestThread;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import org.junit.Before;
import org.junit.Test;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.util.SetOfThrowables;
import io.smallrye.faulttolerance.core.util.TestException;
import io.smallrye.faulttolerance.core.util.TestExecutor;
import io.smallrye.faulttolerance.core.util.TestThread;
import io.smallrye.faulttolerance.core.util.barrier.Barrier;

public class CompletionStageFallbackTest {
    private TestExecutor testExecutor;

    @Before
    public void setUp() {
        testExecutor = new TestExecutor();
    }

    @Test
    public void immediatelyReturning_valueThenValue() throws Exception {
        TestInvocation<CompletionStage<String>> invocation = TestInvocation
                .immediatelyReturning(() -> completedStage("foobar"));
        TestThread<CompletionStage<String>> result = runOnTestThread(new CompletionStageFallback<>(invocation,
                "test invocation", e -> completedStage("fallback"),
                SetOfThrowables.ALL, SetOfThrowables.EMPTY, testExecutor, null));
        assertThat(result.await().toCompletableFuture().get()).isEqualTo("foobar");
    }

    @Test
    public void immediatelyReturning_valueThenDirectException() throws Exception {
        TestInvocation<CompletionStage<String>> invocation = TestInvocation
                .immediatelyReturning(() -> completedStage("foobar"));
        TestThread<CompletionStage<String>> result = runOnTestThread(new CompletionStageFallback<>(invocation,
                "test invocation", e -> TestException.doThrow(),
                SetOfThrowables.ALL, SetOfThrowables.EMPTY, testExecutor, null));
        assertThat(result.await().toCompletableFuture().get()).isEqualTo("foobar");
    }

    @Test
    public void immediatelyReturning_valueThenCompletionStageException() throws Exception {
        TestInvocation<CompletionStage<String>> invocation = TestInvocation
                .immediatelyReturning(() -> completedStage("foobar"));
        TestThread<CompletionStage<String>> result = runOnTestThread(new CompletionStageFallback<>(invocation,
                "test invocation", e -> failedStage(new TestException()),
                SetOfThrowables.ALL, SetOfThrowables.EMPTY, testExecutor, null));
        assertThat(result.await().toCompletableFuture().get()).isEqualTo("foobar");
    }

    @Test
    public void immediatelyReturning_directExceptionThenValue() throws Exception {
        TestInvocation<CompletionStage<String>> invocation = TestInvocation.immediatelyReturning(TestException::doThrow);
        TestThread<CompletionStage<String>> result = runOnTestThread(new CompletionStageFallback<>(invocation,
                "test invocation", e -> completedStage("fallback"),
                SetOfThrowables.ALL, SetOfThrowables.EMPTY, testExecutor, null));
        assertThat(result.await().toCompletableFuture().get()).isEqualTo("fallback");
    }

    @Test
    public void immediatelyReturning_completionStageExceptionThenValue() throws Exception {
        TestInvocation<CompletionStage<String>> invocation = TestInvocation
                .immediatelyReturning(() -> failedStage(new TestException()));
        TestThread<CompletionStage<String>> result = runOnTestThread(new CompletionStageFallback<>(invocation,
                "test invocation", e -> completedStage("fallback"),
                SetOfThrowables.ALL, SetOfThrowables.EMPTY, testExecutor, null));
        assertThat(result.await().toCompletableFuture().get()).isEqualTo("fallback");
    }

    @Test
    public void immediatelyReturning_directExceptionThenDirectException() throws Exception {
        TestInvocation<CompletionStage<Void>> invocation = TestInvocation.immediatelyReturning(TestException::doThrow);
        TestThread<CompletionStage<Void>> result = runOnTestThread(new CompletionStageFallback<>(invocation,
                "test invocation", e -> {
                    throw new RuntimeException();
                }, SetOfThrowables.ALL, SetOfThrowables.EMPTY, testExecutor, null));
        assertThatThrownBy(result.await().toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(RuntimeException.class);
    }

    @Test
    public void immediatelyReturning_directExceptionThenCompletionStageException() throws Exception {
        TestInvocation<CompletionStage<Void>> invocation = TestInvocation.immediatelyReturning(TestException::doThrow);
        TestThread<CompletionStage<Void>> result = runOnTestThread(new CompletionStageFallback<>(invocation,
                "test invocation", e -> failedStage(new RuntimeException()),
                SetOfThrowables.ALL, SetOfThrowables.EMPTY, testExecutor, null));
        assertThatThrownBy(result.await().toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(RuntimeException.class);
    }

    @Test
    public void immediatelyReturning_completionStageExceptionThenDirectException() throws Exception {
        TestInvocation<CompletionStage<Void>> invocation = TestInvocation
                .immediatelyReturning(() -> failedStage(new TestException()));
        TestThread<CompletionStage<Void>> result = runOnTestThread(new CompletionStageFallback<>(invocation,
                "test invocation", e -> {
                    throw new RuntimeException();
                }, SetOfThrowables.ALL, SetOfThrowables.EMPTY, testExecutor, null));
        assertThatThrownBy(result.await().toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(RuntimeException.class);
    }

    @Test
    public void immediatelyReturning_completionStageExceptionThenCompletionStageException() throws Exception {
        TestInvocation<CompletionStage<Void>> invocation = TestInvocation
                .immediatelyReturning(() -> failedStage(new TestException()));
        TestThread<CompletionStage<Void>> result = runOnTestThread(new CompletionStageFallback<>(invocation,
                "test invocation", e -> failedStage(new RuntimeException()),
                SetOfThrowables.ALL, SetOfThrowables.EMPTY, testExecutor, null));
        assertThatThrownBy(result.await().toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(RuntimeException.class);
    }

    // testing interruption and especially self-interruption isn't exactly meaningful,
    // the tests just codify existing behavior

    @Test
    public void waitingOnBarrier_interruptedInInvocation() throws Exception {
        Barrier startBarrier = Barrier.interruptible();
        Barrier endBarrier = Barrier.interruptible();
        TestInvocation<CompletionStage<String>> invocation = TestInvocation.waitingOnBarrier(startBarrier, endBarrier,
                () -> completedStage("foobar"));
        TestThread<CompletionStage<String>> executingThread = runOnTestThread(new CompletionStageFallback<>(invocation,
                "test invocation", e -> completedStage("fallback"),
                SetOfThrowables.ALL, SetOfThrowables.EMPTY, testExecutor, null));
        startBarrier.await();
        testExecutor.interruptExecutingThread();
        assertThatThrownBy(executingThread.await().toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(InterruptedException.class);
    }

    @Test
    public void waitingOnBarrier_interruptedInFallback_directException() throws Exception {
        TestInvocation<CompletionStage<String>> invocation = TestInvocation.immediatelyReturning(TestException::doThrow);
        Barrier startBarrier = Barrier.interruptible();
        Barrier endBarrier = Barrier.interruptible();
        FallbackFunction<CompletionStage<String>> fallback = e -> {
            startBarrier.open();
            endBarrier.await();
            return completedStage("fallback");
        };
        TestThread<CompletionStage<String>> executingThread = runOnTestThread(new CompletionStageFallback<>(invocation,
                "test invocation", fallback,
                SetOfThrowables.ALL, SetOfThrowables.EMPTY, testExecutor, null));
        startBarrier.await();
        testExecutor.interruptExecutingThread();
        assertThatThrownBy(executingThread.await().toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(InterruptedException.class);
    }

    @Test
    public void waitingOnBarrier_interruptedInFallback_completionStageException() throws Exception {
        TestInvocation<CompletionStage<String>> invocation = TestInvocation
                .immediatelyReturning(() -> failedStage(new TestException()));
        Barrier startBarrier = Barrier.interruptible();
        Barrier endBarrier = Barrier.interruptible();
        FallbackFunction<CompletionStage<String>> fallback = e -> {
            startBarrier.open();
            endBarrier.await();
            return completedStage("fallback");
        };
        TestThread<CompletionStage<String>> executingThread = runOnTestThread(new CompletionStageFallback<>(invocation,
                "test invocation", fallback,
                SetOfThrowables.ALL, SetOfThrowables.EMPTY, testExecutor, null));
        startBarrier.await();
        testExecutor.interruptExecutingThread();
        assertThatThrownBy(executingThread.await().toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(InterruptedException.class);
    }

    @Test
    public void selfInterruptedInInvocation_value() throws Exception {
        FaultToleranceStrategy<CompletionStage<String>> invocation = (ignored) -> {
            Thread.currentThread().interrupt();
            return completedStage("foobar");
        };
        TestThread<CompletionStage<String>> result = runOnTestThread(new CompletionStageFallback<>(invocation,
                "test invocation", e -> completedStage("fallback"),
                SetOfThrowables.ALL, SetOfThrowables.EMPTY, testExecutor, null));
        assertThat(result.await().toCompletableFuture().get()).isEqualTo("foobar");
    }

    @Test
    public void selfInterruptedInInvocation_directException() throws Exception {
        FaultToleranceStrategy<CompletionStage<String>> invocation = (ignored) -> {
            Thread.currentThread().interrupt();
            throw new RuntimeException();
        };
        TestThread<CompletionStage<String>> executingThread = runOnTestThread(new CompletionStageFallback<>(invocation,
                "test invocation", e -> completedStage("fallback"),
                SetOfThrowables.ALL, SetOfThrowables.EMPTY, testExecutor, null));
        assertThatThrownBy(executingThread.await().toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(InterruptedException.class);
    }

    @Test
    public void selfInterruptedInInvocation_completionStageException() throws Exception {
        FaultToleranceStrategy<CompletionStage<String>> invocation = (ignored) -> {
            Thread.currentThread().interrupt();
            return failedStage(new RuntimeException());
        };
        TestThread<CompletionStage<String>> executingThread = runOnTestThread(new CompletionStageFallback<>(invocation,
                "test invocation", e -> completedStage("fallback"),
                SetOfThrowables.ALL, SetOfThrowables.EMPTY, testExecutor, null));
        assertThatThrownBy(executingThread.await().toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(InterruptedException.class);
    }

    @Test
    public void selfInterruptedInFallback_value() throws Exception {
        TestInvocation<CompletionStage<String>> invocation = TestInvocation.immediatelyReturning(TestException::doThrow);
        FallbackFunction<CompletionStage<String>> fallback = e -> {
            Thread.currentThread().interrupt();
            return completedStage("fallback");
        };
        TestThread<CompletionStage<String>> result = runOnTestThread(new CompletionStageFallback<>(invocation,
                "test invocation", fallback,
                SetOfThrowables.ALL, SetOfThrowables.EMPTY, testExecutor, null));
        assertThat(result.await().toCompletableFuture().get()).isEqualTo("fallback");
    }

    @Test
    public void selfInterruptedInFallback_directException() throws Exception {
        TestInvocation<CompletionStage<String>> invocation = TestInvocation.immediatelyReturning(TestException::doThrow);
        FallbackFunction<CompletionStage<String>> fallback = e -> {
            Thread.currentThread().interrupt();
            throw new RuntimeException();
        };
        TestThread<CompletionStage<String>> executingThread = runOnTestThread(new CompletionStageFallback<>(invocation,
                "test invocation", fallback,
                SetOfThrowables.ALL, SetOfThrowables.EMPTY, testExecutor, null));
        assertThatThrownBy(executingThread.await().toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(RuntimeException.class);
    }

    @Test
    public void selfInterruptedInFallback_completionStageException() throws Exception {
        TestInvocation<CompletionStage<String>> invocation = TestInvocation.immediatelyReturning(TestException::doThrow);
        FallbackFunction<CompletionStage<String>> fallback = e -> {
            Thread.currentThread().interrupt();
            return failedStage(new RuntimeException());
        };
        TestThread<CompletionStage<String>> executingThread = runOnTestThread(new CompletionStageFallback<>(invocation,
                "test invocation", fallback,
                SetOfThrowables.ALL, SetOfThrowables.EMPTY, testExecutor, null));
        assertThatThrownBy(executingThread.await().toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(RuntimeException.class);
    }
}
