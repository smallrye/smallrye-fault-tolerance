package io.smallrye.faulttolerance.core.async;

import static io.smallrye.faulttolerance.core.FaultToleranceContextUtil.sync;
import static io.smallrye.faulttolerance.core.Invocation.invocation;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.core.util.TestException;
import io.smallrye.faulttolerance.core.util.TestExecutor;
import io.smallrye.faulttolerance.core.util.barrier.Barrier;

public class FutureExecutionTest {
    private TestExecutor executor;

    @BeforeEach
    public void setUp() {
        executor = new TestExecutor();
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        executor.shutdown();
    }

    @Test
    public void successfulExecution() throws Throwable {
        FutureExecution<String> execution = new FutureExecution<>(invocation(), executor);

        java.util.concurrent.Future<String> future = execution.apply(sync(() -> {
            return completedFuture("foobar");
        })).awaitBlocking();
        executor.waitUntilDone();

        assertThat(future).isDone();
        assertThat(future).isNotCancelled();
        assertThat(future.get()).isEqualTo("foobar");
    }

    @Test
    public void failingExecution() throws Throwable {
        TestExecutor executor = new TestExecutor();
        FutureExecution<Void> execution = new FutureExecution<>(invocation(), executor);

        java.util.concurrent.Future<Void> future = execution.apply(sync(() -> {
            return failedFuture(new TestException());
        })).awaitBlocking();
        executor.waitUntilDone();

        assertThat(future).isDone();
        assertThat(future).isNotCancelled();
        assertThatThrownBy(future::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(TestException.class);
    }

    @Test
    public void cancelledExecution() throws Throwable {
        Barrier startInvocationBarrier = Barrier.interruptible();
        Barrier endInvocationBarrier = Barrier.interruptible();

        TestExecutor executor = new TestExecutor();
        FutureExecution<String> execution = new FutureExecution<>(invocation(), executor);

        java.util.concurrent.Future<String> future = execution.apply(sync(() -> {
            startInvocationBarrier.open();
            endInvocationBarrier.await();
            return completedFuture("foobar");
        })).awaitBlocking();

        assertThat(future).isNotDone();

        startInvocationBarrier.await();
        boolean cancelled = future.cancel(true);
        assertThat(cancelled).isTrue();
        endInvocationBarrier.open();

        executor.waitUntilDone();

        assertThat(future).isDone();
        assertThat(future).isCancelled();
        assertThatThrownBy(future::get)
                .isExactlyInstanceOf(CancellationException.class);

        boolean cancelledAgain = future.cancel(true);
        assertThat(cancelledAgain).isFalse();
    }

    @Test
    public void getWithTimeout() throws Throwable {
        Barrier startInvocationBarrier = Barrier.interruptible();
        Barrier endInvocationBarrier = Barrier.interruptible();

        TestExecutor executor = new TestExecutor();
        FutureExecution<String> execution = new FutureExecution<>(invocation(), executor);

        java.util.concurrent.Future<String> future = execution.apply(sync(() -> {
            startInvocationBarrier.open();
            endInvocationBarrier.await();
            return completedFuture("foobar");
        })).awaitBlocking();

        assertThat(future).isNotDone();

        startInvocationBarrier.await();

        assertThatThrownBy(() -> future.get(100, TimeUnit.MILLISECONDS))
                .isExactlyInstanceOf(TimeoutException.class);

        endInvocationBarrier.open();

        executor.waitUntilDone();

        assertThat(future).isDone();
        assertThat(future).isNotCancelled();
        assertThat(future.get(100, TimeUnit.MILLISECONDS)).isEqualTo("foobar");
    }
}
