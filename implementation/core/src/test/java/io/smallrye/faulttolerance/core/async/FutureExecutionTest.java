package io.smallrye.faulttolerance.core.async;

import static io.smallrye.faulttolerance.core.Invocation.invocation;
import static io.smallrye.faulttolerance.core.util.CompletionStages.failedFuture;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

import io.smallrye.faulttolerance.core.InvocationContext;
import io.smallrye.faulttolerance.core.util.TestException;
import io.smallrye.faulttolerance.core.util.TestExecutor;
import io.smallrye.faulttolerance.core.util.barrier.Barrier;

public class FutureExecutionTest {
    @Test
    public void successfulExecution() throws ExecutionException, InterruptedException {
        TestExecutor executor = new TestExecutor();
        FutureExecution<String> execution = new FutureExecution<>(invocation(), executor);

        Future<String> future = execution.apply(new InvocationContext<>(() -> completedFuture("foobar")));
        executor.waitUntilDone();

        assertThat(future).isDone();
        assertThat(future).isNotCancelled();
        assertThat(future.get()).isEqualTo("foobar");
    }

    @Test
    public void failingExecution() throws InterruptedException {
        TestExecutor executor = new TestExecutor();
        FutureExecution<Void> execution = new FutureExecution<>(invocation(), executor);

        Future<Void> future = execution.apply(new InvocationContext<>(() -> failedFuture(new TestException())));
        executor.waitUntilDone();

        assertThat(future).isDone();
        assertThat(future).isNotCancelled();
        assertThatThrownBy(future::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(TestException.class);
    }

    @Test
    public void cancelledExecution() throws InterruptedException {
        Barrier startInvocationBarrier = Barrier.interruptible();
        Barrier endInvocationBarrier = Barrier.interruptible();

        TestExecutor executor = new TestExecutor();
        FutureExecution<String> execution = new FutureExecution<>(invocation(), executor);

        Future<String> future = execution.apply(new InvocationContext<>(() -> {
            startInvocationBarrier.open();
            endInvocationBarrier.await();
            return completedFuture("foobar");
        }));

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
    public void getWithTimeout() throws InterruptedException, ExecutionException, TimeoutException {
        Barrier startInvocationBarrier = Barrier.interruptible();
        Barrier endInvocationBarrier = Barrier.interruptible();

        TestExecutor executor = new TestExecutor();
        FutureExecution<String> execution = new FutureExecution<>(invocation(), executor);

        Future<String> future = execution.apply(new InvocationContext<>(() -> {
            startInvocationBarrier.open();
            endInvocationBarrier.await();
            return completedFuture("foobar");
        }));

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
