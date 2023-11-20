package io.smallrye.faulttolerance.core.timeout;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.core.InvocationContext;
import io.smallrye.faulttolerance.core.async.CompletionStageExecution;
import io.smallrye.faulttolerance.core.util.TestException;
import io.smallrye.faulttolerance.core.util.TestExecutor;
import io.smallrye.faulttolerance.core.util.TestInvocation;
import io.smallrye.faulttolerance.core.util.barrier.Barrier;
import io.smallrye.faulttolerance.core.util.party.Party;

public class CompletionStageTimeoutTest {
    private Barrier timerElapsedBarrier;
    private Barrier timerTaskFinishedBarrier;

    private TestTimer timer;

    private TestExecutor executor;

    @BeforeEach
    public void setUp() {
        timerElapsedBarrier = Barrier.interruptible();
        timerTaskFinishedBarrier = Barrier.interruptible();

        timer = new TestTimer(timerElapsedBarrier, timerTaskFinishedBarrier);

        executor = new TestExecutor();
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        executor.shutdown();
        timer.shutdown();
    }

    @Test
    public void negativeTimeout() {
        TestInvocation<CompletionStage<String>> invocation = TestInvocation.of(() -> completedFuture("foobar"));
        CompletionStageExecution<String> execution = new CompletionStageExecution<>(invocation, executor);
        assertThatThrownBy(() -> new CompletionStageTimeout<>(execution, "test invocation", -1, timer))
                .isExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void zeroTimeout() {
        TestInvocation<CompletionStage<String>> invocation = TestInvocation.of(() -> completedFuture("foobar"));
        CompletionStageExecution<String> execution = new CompletionStageExecution<>(invocation, executor);
        assertThatThrownBy(() -> new CompletionStageTimeout<>(execution, "test invocation", 0, timer))
                .isExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void immediatelyReturning_value() throws Exception {
        TestInvocation<CompletionStage<String>> invocation = TestInvocation.of(() -> completedFuture("foobar"));
        CompletionStageExecution<String> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageTimeout<String> timeout = new CompletionStageTimeout<>(execution,
                "test invocation", 1000, timer);
        CompletionStage<String> result = timeout.apply(new InvocationContext<>(null));
        assertThat(result.toCompletableFuture().get()).isEqualTo("foobar");
        assertThat(timer.timerTaskCancelled()).isTrue();
    }

    @Test
    public void immediatelyReturning_directException() {
        TestInvocation<CompletionStage<Void>> invocation = TestInvocation.of(TestException::doThrow);
        CompletionStageExecution<Void> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageTimeout<Void> timeout = new CompletionStageTimeout<>(execution,
                "test invocation", 1000, timer);
        CompletionStage<Void> result = timeout.apply(new InvocationContext<>(null));
        assertThatThrownBy(result.toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(TestException.class);
        assertThat(timer.timerTaskCancelled()).isTrue();
    }

    @Test
    public void immediatelyReturning_completionStageException() {
        TestInvocation<CompletionStage<Void>> invocation = TestInvocation.of(() -> failedFuture(new TestException()));
        CompletionStageExecution<Void> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageTimeout<Void> timeout = new CompletionStageTimeout<>(execution,
                "test invocation", 1000, timer);
        CompletionStage<Void> result = timeout.apply(new InvocationContext<>(null));
        assertThatThrownBy(result.toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(TestException.class);
        assertThat(timer.timerTaskCancelled()).isTrue();
    }

    @Test
    public void delayed_value_notTimedOut() throws Exception {
        Barrier delayBarrier = Barrier.interruptible();

        TestInvocation<CompletionStage<String>> invocation = TestInvocation.of(() -> {
            delayBarrier.await();
            return completedFuture("foobar");
        });
        CompletionStageExecution<String> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageTimeout<String> timeout = new CompletionStageTimeout<>(execution,
                "test invocation", 1000, timer);
        CompletionStage<String> result = timeout.apply(new InvocationContext<>(null));
        delayBarrier.open();
        assertThat(result.toCompletableFuture().get()).isEqualTo("foobar");
        assertThat(timer.timerTaskCancelled()).isTrue();
    }

    @Test
    public void delayed_value_timedOut() throws Exception {
        Barrier delayBarrier = Barrier.interruptible();

        TestInvocation<CompletionStage<String>> invocation = TestInvocation.of(() -> {
            delayBarrier.await();
            return completedFuture("foobar");
        });
        CompletionStageExecution<String> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageTimeout<String> timeout = new CompletionStageTimeout<>(execution,
                "test invocation", 1000, timer);
        CompletionStage<String> result = timeout.apply(new InvocationContext<>(null));
        timerElapsedBarrier.open();
        timerTaskFinishedBarrier.await();
        assertThatThrownBy(result.toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(TimeoutException.class)
                .hasMessageContaining("test invocation timed out");
        assertThat(timer.timerTaskCancelled()).isFalse();
    }

    @Test
    public void delayed_value_timedOutNoninterruptibly() throws Exception {
        Barrier delayBarrier = Barrier.noninterruptible();

        TestInvocation<CompletionStage<String>> invocation = TestInvocation.of(() -> {
            delayBarrier.await();
            return completedFuture("foobar");
        });
        CompletionStageExecution<String> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageTimeout<String> timeout = new CompletionStageTimeout<>(execution,
                "test invocation", 1000, timer);
        CompletionStage<String> result = timeout.apply(new InvocationContext<>(null));
        timerElapsedBarrier.open();
        timerTaskFinishedBarrier.await();
        delayBarrier.open();
        assertThatThrownBy(result.toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(TimeoutException.class)
                .hasMessageContaining("test invocation timed out");
        assertThat(timer.timerTaskCancelled()).isFalse();
    }

    @Test
    public void delayed_value_interruptedEarly() throws Exception {
        Party party = Party.create(1);

        TestInvocation<CompletionStage<String>> invocation = TestInvocation.of(() -> {
            party.participant().attend();
            return completedFuture("foobar");
        });
        CompletionStageExecution<String> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageTimeout<String> timeout = new CompletionStageTimeout<>(execution,
                "test invocation", 1000, timer);
        CompletionStage<String> result = timeout.apply(new InvocationContext<>(null));
        party.organizer().waitForAll();
        executor.interruptExecutingThread();
        assertThatThrownBy(result.toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(InterruptedException.class);
        assertThat(timer.timerTaskCancelled()).isTrue();
    }

    @Test
    public void delayed_exception_notTimedOut() {
        Barrier delayBarrier = Barrier.interruptible();

        TestInvocation<CompletionStage<Void>> invocation = TestInvocation.of(() -> {
            delayBarrier.await();
            return failedFuture(new TestException());
        });
        CompletionStageExecution<Void> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageTimeout<Void> timeout = new CompletionStageTimeout<>(execution,
                "test invocation", 1000, timer);
        CompletionStage<Void> result = timeout.apply(new InvocationContext<>(null));
        delayBarrier.open();
        assertThatThrownBy(result.toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(TestException.class);
        assertThat(timer.timerTaskCancelled()).isTrue();
    }

    @Test
    public void delayed_exception_timedOut() throws Exception {
        Barrier delayBarrier = Barrier.interruptible();

        TestInvocation<CompletionStage<Void>> invocation = TestInvocation.of(() -> {
            delayBarrier.await();
            return failedFuture(new TestException());
        });
        CompletionStageExecution<Void> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageTimeout<Void> timeout = new CompletionStageTimeout<>(execution,
                "test invocation", 1000, timer);
        CompletionStage<Void> result = timeout.apply(new InvocationContext<>(null));
        timerElapsedBarrier.open();
        timerTaskFinishedBarrier.await();
        assertThatThrownBy(result.toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(TimeoutException.class)
                .hasMessageContaining("test invocation timed out");
        assertThat(timer.timerTaskCancelled()).isFalse();
    }

    @Test
    public void delayed_exception_timedOutNoninterruptibly() throws Exception {
        Barrier delayBarrier = Barrier.noninterruptible();

        TestInvocation<CompletionStage<Void>> invocation = TestInvocation.of(() -> {
            delayBarrier.await();
            return failedFuture(new TestException());
        });
        CompletionStageExecution<Void> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageTimeout<Void> timeout = new CompletionStageTimeout<>(execution,
                "test invocation", 1000, timer);
        CompletionStage<Void> result = timeout.apply(new InvocationContext<>(null));
        timerElapsedBarrier.open();
        timerTaskFinishedBarrier.await();
        delayBarrier.open();
        assertThatThrownBy(result.toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(TimeoutException.class)
                .hasMessageContaining("test invocation timed out");
        assertThat(timer.timerTaskCancelled()).isFalse();
    }

    @Test
    public void delayed_exception_interruptedEarly() throws Exception {
        Party party = Party.create(1);

        TestInvocation<CompletionStage<Void>> invocation = TestInvocation.of(() -> {
            party.participant().attend();
            return failedFuture(new TestException());
        });
        CompletionStageExecution<Void> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageTimeout<Void> timeout = new CompletionStageTimeout<>(execution,
                "test invocation", 1000, timer);
        CompletionStage<Void> result = timeout.apply(new InvocationContext<>(null));
        party.organizer().waitForAll();
        executor.interruptExecutingThread();
        assertThatThrownBy(result.toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(InterruptedException.class);
        assertThat(timer.timerTaskCancelled()).isTrue();
    }

    @Test
    public void immediatelyReturning_completionStageTimedOut() throws Exception {
        Barrier barrier = Barrier.interruptible();

        TestInvocation<CompletionStage<Void>> invocation = TestInvocation.of(
                () -> CompletableFuture.supplyAsync(() -> {
                    try {
                        barrier.await();
                    } catch (InterruptedException e) {
                        throw new RuntimeException("interrupted");
                    }
                    return null;
                }));
        CompletionStageExecution<Void> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageTimeout<Void> timeout = new CompletionStageTimeout<>(execution,
                "test invocation", 1000, timer);
        CompletionStage<Void> result = timeout.apply(new InvocationContext<>(null));
        timerElapsedBarrier.open();
        timerTaskFinishedBarrier.await();
        assertThatThrownBy(result.toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(TimeoutException.class);
        barrier.open();
    }
}
