package io.smallrye.faulttolerance.core.retry;

import static io.smallrye.faulttolerance.core.util.CompletionStages.failedFuture;
import static io.smallrye.faulttolerance.core.util.TestThread.runOnTestThread;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import java.util.concurrent.Future;

import org.junit.Before;
import org.junit.Test;

import io.smallrye.faulttolerance.core.stopwatch.TestStopwatch;
import io.smallrye.faulttolerance.core.util.SetOfThrowables;
import io.smallrye.faulttolerance.core.util.TestException;
import io.smallrye.faulttolerance.core.util.TestThread;
import io.smallrye.faulttolerance.core.util.barrier.Barrier;

/**
 * Tests a subset of {@link RetryTest} because the underlying logic is the same.
 */
public class FutureRetryTest {
    private static final SetOfThrowables exception = SetOfThrowables.create(Collections.singletonList(Exception.class));
    private static final SetOfThrowables testException = SetOfThrowables.create(Collections.singletonList(TestException.class));

    private TestStopwatch stopwatch;

    @Before
    public void setUp() {
        stopwatch = new TestStopwatch();
    }

    @Test
    public void immediatelyReturning_failedFuture() throws Exception {
        RuntimeException exception = new RuntimeException();
        TestInvocation<Future<String>> invocation = TestInvocation.immediatelyReturning(() -> failedFuture(exception));
        Retry<Future<String>> futureRetry = new Retry<>(invocation, "test invocation",
                SetOfThrowables.EMPTY, SetOfThrowables.EMPTY, 3, 1000, Delay.NONE, stopwatch, null);
        Future<String> result = runOnTestThread(futureRetry).await();
        assertThatThrownBy(result::get).hasCause(exception);
        assertThat(invocation.numberOfInvocations()).isEqualTo(1);
    }

    @Test
    public void immediatelyReturning_value() throws Exception {
        TestInvocation<Future<String>> invocation = TestInvocation.immediatelyReturning(() -> completedFuture("foobar"));
        Future<String> result = runOnTestThread(
                new Retry<>(invocation, "test invocation", SetOfThrowables.EMPTY, SetOfThrowables.EMPTY, 3, 1000,
                        Delay.NONE, stopwatch, null)).await();
        assertThat(result.get()).isEqualTo("foobar");
        assertThat(invocation.numberOfInvocations()).isEqualTo(1);
    }

    @Test
    public void immediatelyReturning_interruptedInInvocation() throws InterruptedException {
        Barrier startInvocationBarrier = Barrier.interruptible();
        Barrier endInvocationBarrier = Barrier.interruptible();
        TestInvocation<Future<String>> invocation = TestInvocation.immediatelyReturning(() -> {
            startInvocationBarrier.open();
            endInvocationBarrier.await();
            return completedFuture("foobar");
        });
        TestThread<Future<String>> executingThread = runOnTestThread(new Retry<>(invocation, "test invocation",
                SetOfThrowables.EMPTY, SetOfThrowables.EMPTY, 3, 1000, Delay.NONE, stopwatch, null));
        startInvocationBarrier.await();
        executingThread.interrupt();
        assertThatThrownBy(executingThread::await).isInstanceOf(InterruptedException.class);
        assertThat(invocation.numberOfInvocations()).isEqualTo(1);
    }

    @Test
    public void immediatelyReturning_selfInterruptedInInvocation() throws InterruptedException {
        Barrier startInvocationBarrier = Barrier.interruptible();
        Barrier endInvocationBarrier = Barrier.interruptible();
        TestInvocation<Future<Void>> invocation = TestInvocation.immediatelyReturning(() -> {
            startInvocationBarrier.open();
            endInvocationBarrier.await();
            Thread.currentThread().interrupt();
            throw new RuntimeException();
        });
        TestThread<Future<Void>> executingThread = runOnTestThread(new Retry<>(invocation, "test invocation",
                SetOfThrowables.EMPTY, SetOfThrowables.EMPTY, 3, 1000, Delay.NONE, stopwatch, null));
        startInvocationBarrier.await();
        endInvocationBarrier.open();
        assertThatThrownBy(executingThread::await).isInstanceOf(InterruptedException.class);
        assertThat(invocation.numberOfInvocations()).isEqualTo(1);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenValue_equalToMaxRetries() throws Exception {
        TestInvocation<Future<String>> invocation = TestInvocation.initiallyFailing(3, RuntimeException::new,
                () -> completedFuture("foobar"));
        TestThread<Future<String>> result = runOnTestThread(new Retry<>(invocation, "test invocation",
                exception, SetOfThrowables.EMPTY, 3, 1000, Delay.NONE, stopwatch, null));
        assertThat(result.await().get()).isEqualTo("foobar");
        assertThat(invocation.numberOfInvocations()).isEqualTo(4);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenValue_moreThanMaxRetries() {
        TestInvocation<Future<String>> invocation = TestInvocation.initiallyFailing(4, RuntimeException::new,
                () -> completedFuture("foobar"));
        TestThread<Future<String>> result = runOnTestThread(new Retry<>(invocation, "test invocation",
                exception, SetOfThrowables.EMPTY, 3, 1000, Delay.NONE, stopwatch, null));
        assertThatThrownBy(result::await).isExactlyInstanceOf(RuntimeException.class);
        assertThat(invocation.numberOfInvocations()).isEqualTo(4);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenRetriedException_equalToMaxRetries() {
        TestInvocation<Future<Void>> invocation = TestInvocation.initiallyFailing(3, RuntimeException::new,
                TestException::doThrow);
        TestThread<Future<Void>> result = runOnTestThread(new Retry<>(invocation, "test invocation",
                exception, SetOfThrowables.EMPTY, 3, 1000, Delay.NONE, stopwatch, null));
        assertThatThrownBy(result::await).isExactlyInstanceOf(TestException.class);
        assertThat(invocation.numberOfInvocations()).isEqualTo(4);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenValue_interruptedInInvocation() throws InterruptedException {
        Barrier startInvocationBarrier = Barrier.interruptible();
        Barrier endInvocationBarrier = Barrier.interruptible();
        TestInvocation<Future<String>> invocation = TestInvocation.initiallyFailing(3, RuntimeException::new, () -> {
            startInvocationBarrier.open();
            endInvocationBarrier.await();
            return completedFuture("foobar");
        });
        TestThread<Future<String>> executingThread = runOnTestThread(new Retry<>(invocation, "test invocation",
                exception, testException, 3, 1000, Delay.NONE, stopwatch, null));
        startInvocationBarrier.await();
        executingThread.interrupt();
        assertThatThrownBy(executingThread::await).isInstanceOf(InterruptedException.class);
        assertThat(invocation.numberOfInvocations()).isEqualTo(4);
    }
}
