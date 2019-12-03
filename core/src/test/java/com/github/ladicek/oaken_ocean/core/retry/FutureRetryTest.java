package com.github.ladicek.oaken_ocean.core.retry;

import com.github.ladicek.oaken_ocean.core.stopwatch.TestStopwatch;
import com.github.ladicek.oaken_ocean.core.util.SetOfThrowables;
import com.github.ladicek.oaken_ocean.core.util.TestException;
import com.github.ladicek.oaken_ocean.core.util.TestThread;
import com.github.ladicek.oaken_ocean.core.util.barrier.Barrier;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import static com.github.ladicek.oaken_ocean.core.util.TestThread.runOnTestThread;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests a subset of {@link FutureRetryTest} because the underlying logic is the same
 */
public class FutureRetryTest {
    private static final SetOfThrowables exception = SetOfThrowables.withoutCustomThrowables(Collections.singletonList(Exception.class));
    private static final SetOfThrowables testException = SetOfThrowables.withoutCustomThrowables(Collections.singletonList(TestException.class));

    private TestStopwatch stopwatch;

    @Before
    public void setUp() {
        stopwatch = new TestStopwatch();
    }

    @Test
    public void noRetryOnFailedFuture() throws Exception {
        RuntimeException exception = new RuntimeException();
        FutureTestInvocation<String> invocation = FutureTestInvocation.eventuallyFailing(() -> exception);
        FutureRetry<String> futureRetry = new FutureRetry<>(invocation, "test invocation", SetOfThrowables.EMPTY, SetOfThrowables.EMPTY, 3, 1000, Delay.NONE, stopwatch, null);
        Future<String> result = runOnTestThread(
              futureRetry
        ).await();

        assertThat(invocation.numberOfInvocations()).isEqualTo(1);

        assertThatThrownBy(result::get).hasCause(exception);
    }

    @Test
    public void immediatelyReturning_value() throws Exception {
        FutureTestInvocation<String> invocation = FutureTestInvocation.immediatelyReturning(() -> CompletableFuture.completedFuture("foobar"));
        Future<String> result = runOnTestThread(
              new FutureRetry<>(invocation, "test invocation", SetOfThrowables.EMPTY, SetOfThrowables.EMPTY, 3, 1000, Delay.NONE, stopwatch, null)
        ).await();
        assertThat(result.get()).isEqualTo("foobar");
        assertThat(invocation.numberOfInvocations()).isEqualTo(1);
    }

    @Test
    public void immediatelyReturning_interruptedInInvocation() throws InterruptedException {
        Barrier startInvocationBarrier = Barrier.interruptible();
        Barrier endInvocationBarrier = Barrier.interruptible();
        FutureTestInvocation<String> invocation = FutureTestInvocation.immediatelyReturning(() -> {
            startInvocationBarrier.open();
            endInvocationBarrier.await();
            return CompletableFuture.completedFuture("foobar");
        });
        TestThread<Future<String>> executingThread = runOnTestThread(
              new FutureRetry<>(invocation, "test invocation",
                SetOfThrowables.EMPTY, SetOfThrowables.EMPTY, 3, 1000, Delay.NONE, stopwatch, null)
        );
        startInvocationBarrier.await();
        executingThread.interrupt();
        assertThatThrownBy(executingThread::await).isInstanceOf(InterruptedException.class);
        assertThat(invocation.numberOfInvocations()).isEqualTo(1);
    }

    @Test
    public void immediatelyReturning_selfInterruptedInInvocation() throws InterruptedException {
        Barrier startInvocationBarrier = Barrier.interruptible();
        Barrier endInvocationBarrier = Barrier.interruptible();
        FutureTestInvocation<Void> invocation = FutureTestInvocation.immediatelyReturning(() -> {
            startInvocationBarrier.open();
            endInvocationBarrier.await();
            Thread.currentThread().interrupt();
            throw new RuntimeException();
        });
        TestThread<Future<Void>> executingThread = runOnTestThread(new FutureRetry<>(invocation, "test invocation",
                SetOfThrowables.EMPTY, SetOfThrowables.EMPTY, 3, 1000, Delay.NONE, stopwatch, null));
        startInvocationBarrier.await();
        endInvocationBarrier.open();
        assertThatThrownBy(executingThread::await).isInstanceOf(InterruptedException.class);
        assertThat(invocation.numberOfInvocations()).isEqualTo(1);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenValue_equalToMaxRetries() throws Exception {
        FutureTestInvocation<String> invocation =
              FutureTestInvocation.initiallyFailing(3, RuntimeException::new, () -> CompletableFuture.completedFuture("foobar"));
        TestThread<Future<String>> result = runOnTestThread(new FutureRetry<>(invocation, "test invocation",
                exception, SetOfThrowables.EMPTY, 3, 1000, Delay.NONE, stopwatch, null));
        assertThat(result.await().get()).isEqualTo("foobar");
        assertThat(invocation.numberOfInvocations()).isEqualTo(4);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenValue_moreThanMaxRetries() {
        FutureTestInvocation<String> invocation = FutureTestInvocation.initiallyFailing(4, RuntimeException::new, () -> CompletableFuture.completedFuture("foobar"));
        TestThread<Future<String>> result = runOnTestThread(new FutureRetry<>(invocation, "test invocation",
                exception, SetOfThrowables.EMPTY, 3, 1000, Delay.NONE, stopwatch, null));
        assertThatThrownBy(result::await).isExactlyInstanceOf(RuntimeException.class);
        assertThat(invocation.numberOfInvocations()).isEqualTo(4);
    }
    @Test
    public void initiallyFailing_retriedExceptionThenRetriedException_equalToMaxRetries() {
        FutureTestInvocation<Void> invocation = FutureTestInvocation.initiallyFailing(3, RuntimeException::new, TestException::<Future<Void>>doThrow);
        TestThread<Future<Void>> result = runOnTestThread(new FutureRetry<>(invocation, "test invocation",
                exception, SetOfThrowables.EMPTY, 3, 1000, Delay.NONE, stopwatch, null));
        assertThatThrownBy(result::await).isExactlyInstanceOf(TestException.class);
        assertThat(invocation.numberOfInvocations()).isEqualTo(4);
    }


    @Test
    public void initiallyFailing_retriedExceptionThenValue_interruptedInInvocation() throws InterruptedException {
        Barrier startInvocationBarrier = Barrier.interruptible();
        Barrier endInvocationBarrier = Barrier.interruptible();
        FutureTestInvocation<String> invocation = FutureTestInvocation.initiallyFailing(3, RuntimeException::new, () -> {
            startInvocationBarrier.open();
            endInvocationBarrier.await();
            return CompletableFuture.completedFuture("foobar");
        });
        TestThread<Future<String>> executingThread = runOnTestThread(new FutureRetry<>(invocation, "test invocation",
                exception, testException, 3, 1000, Delay.NONE, stopwatch, null));
        startInvocationBarrier.await();
        executingThread.interrupt();
        assertThatThrownBy(executingThread::await).isInstanceOf(InterruptedException.class);
        assertThat(invocation.numberOfInvocations()).isEqualTo(4);
    }
}
