package io.smallrye.faulttolerance.core.retry;

import static io.smallrye.faulttolerance.core.util.TestThread.runOnTestThread;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.Future;

import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.core.stopwatch.TestStopwatch;
import io.smallrye.faulttolerance.core.util.ExceptionDecision;
import io.smallrye.faulttolerance.core.util.ResultDecision;
import io.smallrye.faulttolerance.core.util.SetBasedExceptionDecision;
import io.smallrye.faulttolerance.core.util.SetOfThrowables;
import io.smallrye.faulttolerance.core.util.TestException;
import io.smallrye.faulttolerance.core.util.TestThread;
import io.smallrye.faulttolerance.core.util.barrier.Barrier;

/**
 * Replicates a subset of {@link RetrySyncTest} because the underlying logic is the same.
 */
public class RetryFutureTest {
    private static final SetOfThrowables exception = SetOfThrowables.create(Exception.class);
    private static final SetOfThrowables testException = SetOfThrowables.create(TestException.class);

    private TestStopwatch stopwatch;

    @BeforeEach
    public void setUp() {
        stopwatch = new TestStopwatch();
    }

    @Test
    public void immediatelyReturning_failedFuture() throws Exception {
        RuntimeException exception = new RuntimeException();
        TestInvocation<Future<String>> invocation = TestInvocation.immediatelyReturning(() -> failedFuture(exception));
        Retry<Future<String>> retry = new Retry<>(invocation, "test invocation",
                ResultDecision.ALWAYS_EXPECTED, ExceptionDecision.ALWAYS_EXPECTED, 3, 1000, SyncDelay.NONE, AsyncDelay.NONE,
                stopwatch, null);
        Future<String> result = runOnTestThread(retry, false).await();
        assertThatThrownBy(result::get).hasCause(exception);
        assertThat(invocation.numberOfInvocations()).isEqualTo(1);
    }

    @Test
    public void immediatelyReturning_value() throws Exception {
        TestInvocation<Future<String>> invocation = TestInvocation.immediatelyReturning(() -> completedFuture("foobar"));
        Retry<Future<String>> retry = new Retry<>(invocation, "test invocation",
                ResultDecision.ALWAYS_EXPECTED, ExceptionDecision.ALWAYS_EXPECTED, 3, 1000, SyncDelay.NONE, AsyncDelay.NONE,
                stopwatch, null);
        Future<String> result = runOnTestThread(retry, false).await();
        assertThat(result.get()).isEqualTo("foobar");
        assertThat(invocation.numberOfInvocations()).isEqualTo(1);
    }

    @Test
    public void immediatelyReturning_retriedValue() {
        TestInvocation<Future<String>> invocation = TestInvocation.immediatelyReturning(() -> completedFuture("foobar"));
        Retry<Future<String>> retry = new Retry<>(invocation, "test invocation",
                ResultDecision.ALWAYS_FAILURE, ExceptionDecision.ALWAYS_EXPECTED, 3, 1000, SyncDelay.NONE, AsyncDelay.NONE,
                stopwatch, null);
        TestThread<Future<String>> executingThread = runOnTestThread(retry, false);
        assertThatThrownBy(executingThread::await).isExactlyInstanceOf(FaultToleranceException.class);
        assertThat(invocation.numberOfInvocations()).isEqualTo(4);
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
        Retry<Future<String>> retry = new Retry<>(invocation, "test invocation",
                ResultDecision.ALWAYS_EXPECTED, ExceptionDecision.ALWAYS_EXPECTED, 3, 1000, SyncDelay.NONE, AsyncDelay.NONE,
                stopwatch, null);
        TestThread<Future<String>> executingThread = runOnTestThread(retry, false);
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
        Retry<Future<Void>> retry = new Retry<>(invocation, "test invocation",
                ResultDecision.ALWAYS_EXPECTED, ExceptionDecision.ALWAYS_EXPECTED, 3, 1000, SyncDelay.NONE, AsyncDelay.NONE,
                stopwatch, null);
        TestThread<Future<Void>> executingThread = runOnTestThread(retry, false);
        startInvocationBarrier.await();
        endInvocationBarrier.open();
        assertThatThrownBy(executingThread::await).isInstanceOf(InterruptedException.class);
        assertThat(invocation.numberOfInvocations()).isEqualTo(1);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenValue_equalToMaxRetries() throws Exception {
        TestInvocation<Future<String>> invocation = TestInvocation.initiallyFailing(3, RuntimeException::new,
                () -> completedFuture("foobar"));
        Retry<Future<String>> retry = new Retry<>(invocation, "test invocation",
                ResultDecision.ALWAYS_EXPECTED, new SetBasedExceptionDecision(exception, SetOfThrowables.EMPTY, false),
                3, 1000, SyncDelay.NONE, AsyncDelay.NONE, stopwatch, null);
        TestThread<Future<String>> result = runOnTestThread(retry, false);
        assertThat(result.await().get()).isEqualTo("foobar");
        assertThat(invocation.numberOfInvocations()).isEqualTo(4);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenValue_moreThanMaxRetries() {
        TestInvocation<Future<String>> invocation = TestInvocation.initiallyFailing(4, RuntimeException::new,
                () -> completedFuture("foobar"));
        Retry<Future<String>> retry = new Retry<>(invocation, "test invocation",
                ResultDecision.ALWAYS_EXPECTED, new SetBasedExceptionDecision(exception, SetOfThrowables.EMPTY, false),
                3, 1000, SyncDelay.NONE, AsyncDelay.NONE, stopwatch, null);
        TestThread<Future<String>> result = runOnTestThread(retry, false);
        assertThatThrownBy(result::await).isExactlyInstanceOf(RuntimeException.class);
        assertThat(invocation.numberOfInvocations()).isEqualTo(4);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenRetriedException_equalToMaxRetries() {
        TestInvocation<Future<Void>> invocation = TestInvocation.initiallyFailing(3, RuntimeException::new,
                TestException::doThrow);
        Retry<Future<Void>> retry = new Retry<>(invocation, "test invocation",
                ResultDecision.ALWAYS_EXPECTED, new SetBasedExceptionDecision(exception, SetOfThrowables.EMPTY, false),
                3, 1000, SyncDelay.NONE, AsyncDelay.NONE, stopwatch, null);
        TestThread<Future<Void>> result = runOnTestThread(retry, false);
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
        Retry<Future<String>> retry = new Retry<>(invocation, "test invocation",
                ResultDecision.ALWAYS_EXPECTED, new SetBasedExceptionDecision(exception, testException, false),
                3, 1000, SyncDelay.NONE, AsyncDelay.NONE, stopwatch, null);
        TestThread<Future<String>> executingThread = runOnTestThread(retry, false);
        startInvocationBarrier.await();
        executingThread.interrupt();
        assertThatThrownBy(executingThread::await).isInstanceOf(InterruptedException.class);
        assertThat(invocation.numberOfInvocations()).isEqualTo(4);
    }
}
