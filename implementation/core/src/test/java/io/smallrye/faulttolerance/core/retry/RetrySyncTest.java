package io.smallrye.faulttolerance.core.retry;

import static io.smallrye.faulttolerance.core.util.TestThread.runOnTestThread;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

public class RetrySyncTest {
    private static final SetOfThrowables exception = SetOfThrowables.create(Exception.class);
    private static final SetOfThrowables testException = SetOfThrowables.create(TestException.class);

    private TestStopwatch stopwatch;

    @BeforeEach
    public void setUp() {
        stopwatch = new TestStopwatch();
    }

    @Test
    public void immediatelyReturning_value() throws Exception {
        TestInvocation<String> invocation = TestInvocation.immediatelyReturning(() -> "foobar");
        Retry<String> retry = new Retry<>(invocation, "test invocation",
                ResultDecision.ALWAYS_EXPECTED, ExceptionDecision.ALWAYS_EXPECTED, 3, 1000,
                SyncDelay.NONE, AsyncDelay.NONE, stopwatch, null);
        TestThread<String> result = runOnTestThread(retry, false);
        assertThat(result.await()).isEqualTo("foobar");
        assertThat(invocation.numberOfInvocations()).isEqualTo(1);
    }

    @Test
    public void immediatelyReturning_retriedValue() {
        TestInvocation<String> invocation = TestInvocation.immediatelyReturning(() -> "foobar");
        Retry<String> retry = new Retry<>(invocation, "test invocation",
                ResultDecision.ALWAYS_FAILURE, ExceptionDecision.ALWAYS_EXPECTED, 3, 1000,
                SyncDelay.NONE, AsyncDelay.NONE, stopwatch, null);
        TestThread<String> result = runOnTestThread(retry, false);
        assertThatThrownBy(result::await).isExactlyInstanceOf(FaultToleranceException.class);
        assertThat(invocation.numberOfInvocations()).isEqualTo(4);
    }

    @Test
    public void immediatelyReturning_retriedException() {
        TestInvocation<Void> invocation = TestInvocation.immediatelyReturning(TestException::doThrow);
        Retry<Void> retry = new Retry<>(invocation, "test invocation",
                ResultDecision.ALWAYS_EXPECTED, new SetBasedExceptionDecision(exception, SetOfThrowables.EMPTY, false),
                3, 1000, SyncDelay.NONE, AsyncDelay.NONE, stopwatch, null);
        TestThread<Void> result = runOnTestThread(retry, false);
        assertThatThrownBy(result::await).isExactlyInstanceOf(TestException.class);
        assertThat(invocation.numberOfInvocations()).isEqualTo(4);
    }

    @Test
    public void immediatelyReturning_abortingException() {
        TestInvocation<Void> invocation = TestInvocation.immediatelyReturning(TestException::doThrow);
        Retry<Void> retry = new Retry<>(invocation, "test invocation",
                ResultDecision.ALWAYS_EXPECTED, new SetBasedExceptionDecision(exception, testException, false),
                3, 1000, SyncDelay.NONE, AsyncDelay.NONE, stopwatch, null);
        TestThread<Void> result = runOnTestThread(retry, false);
        assertThatThrownBy(result::await).isExactlyInstanceOf(TestException.class);
        assertThat(invocation.numberOfInvocations()).isEqualTo(1);
    }

    @Test
    public void immediatelyReturning_unknownException() {
        TestInvocation<Void> invocation = TestInvocation.immediatelyReturning(TestException::doThrow);
        Retry<Void> retry = new Retry<>(invocation, "test invocation",
                ResultDecision.ALWAYS_EXPECTED, ExceptionDecision.ALWAYS_EXPECTED,
                3, 1000, SyncDelay.NONE, AsyncDelay.NONE, stopwatch, null);
        TestThread<Void> result = runOnTestThread(retry, false);
        assertThatThrownBy(result::await).isExactlyInstanceOf(TestException.class);
        assertThat(invocation.numberOfInvocations()).isEqualTo(1);
    }

    @Test
    public void immediatelyReturning_interruptedInInvocation() throws InterruptedException {
        Barrier startInvocationBarrier = Barrier.interruptible();
        Barrier endInvocationBarrier = Barrier.interruptible();
        TestInvocation<String> invocation = TestInvocation.immediatelyReturning(() -> {
            startInvocationBarrier.open();
            endInvocationBarrier.await();
            return "foobar";
        });
        Retry<String> retry = new Retry<>(invocation, "test invocation",
                ResultDecision.ALWAYS_EXPECTED, ExceptionDecision.ALWAYS_EXPECTED,
                3, 1000, SyncDelay.NONE, AsyncDelay.NONE, stopwatch, null);
        TestThread<String> executingThread = runOnTestThread(retry, false);
        startInvocationBarrier.await();
        executingThread.interrupt();
        assertThatThrownBy(executingThread::await).isInstanceOf(InterruptedException.class);
        assertThat(invocation.numberOfInvocations()).isEqualTo(1);
    }

    @Test
    public void immediatelyReturning_selfInterruptedInInvocation() throws InterruptedException {
        Barrier startInvocationBarrier = Barrier.interruptible();
        Barrier endInvocationBarrier = Barrier.interruptible();
        TestInvocation<Void> invocation = TestInvocation.immediatelyReturning(() -> {
            startInvocationBarrier.open();
            endInvocationBarrier.await();
            Thread.currentThread().interrupt();
            throw new RuntimeException();
        });
        Retry<Void> retry = new Retry<>(invocation, "test invocation",
                ResultDecision.ALWAYS_EXPECTED, ExceptionDecision.ALWAYS_EXPECTED,
                3, 1000, SyncDelay.NONE, AsyncDelay.NONE, stopwatch, null);
        TestThread<Void> executingThread = runOnTestThread(retry, false);
        startInvocationBarrier.await();
        endInvocationBarrier.open();
        assertThatThrownBy(executingThread::await).isInstanceOf(InterruptedException.class);
        assertThat(invocation.numberOfInvocations()).isEqualTo(1);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenValue_lessThanMaxRetries() throws Exception {
        TestInvocation<String> invocation = TestInvocation.initiallyFailing(2, RuntimeException::new, () -> "foobar");
        Retry<String> retry = new Retry<>(invocation, "test invocation",
                ResultDecision.ALWAYS_EXPECTED, new SetBasedExceptionDecision(exception, SetOfThrowables.EMPTY, false),
                3, 1000, SyncDelay.NONE, AsyncDelay.NONE, stopwatch, null);
        TestThread<String> result = runOnTestThread(retry, false);
        assertThat(result.await()).isEqualTo("foobar");
        assertThat(invocation.numberOfInvocations()).isEqualTo(3);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenValue_equalToMaxRetries() throws Exception {
        TestInvocation<String> invocation = TestInvocation.initiallyFailing(3, RuntimeException::new, () -> "foobar");
        Retry<String> retry = new Retry<>(invocation, "test invocation",
                ResultDecision.ALWAYS_EXPECTED, new SetBasedExceptionDecision(exception, SetOfThrowables.EMPTY, false),
                3, 1000, SyncDelay.NONE, AsyncDelay.NONE, stopwatch, null);
        TestThread<String> result = runOnTestThread(retry, false);
        assertThat(result.await()).isEqualTo("foobar");
        assertThat(invocation.numberOfInvocations()).isEqualTo(4);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenValue_moreThanMaxRetries() {
        TestInvocation<String> invocation = TestInvocation.initiallyFailing(4, RuntimeException::new, () -> "foobar");
        Retry<String> retry = new Retry<>(invocation, "test invocation",
                ResultDecision.ALWAYS_EXPECTED, new SetBasedExceptionDecision(exception, SetOfThrowables.EMPTY, false),
                3, 1000, SyncDelay.NONE, AsyncDelay.NONE, stopwatch, null);
        TestThread<String> result = runOnTestThread(retry, false);
        assertThatThrownBy(result::await).isExactlyInstanceOf(RuntimeException.class);
        assertThat(invocation.numberOfInvocations()).isEqualTo(4);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenRetriedException_lessThanMaxRetries() {
        TestInvocation<Void> invocation = TestInvocation.initiallyFailing(2, RuntimeException::new, TestException::doThrow);
        Retry<Void> retry = new Retry<>(invocation, "test invocation",
                ResultDecision.ALWAYS_EXPECTED, new SetBasedExceptionDecision(exception, SetOfThrowables.EMPTY, false),
                3, 1000, SyncDelay.NONE, AsyncDelay.NONE, stopwatch, null);
        TestThread<Void> result = runOnTestThread(retry, false);
        assertThatThrownBy(result::await).isExactlyInstanceOf(TestException.class);
        assertThat(invocation.numberOfInvocations()).isEqualTo(4);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenRetriedException_equalToMaxRetries() {
        TestInvocation<Void> invocation = TestInvocation.initiallyFailing(3, RuntimeException::new, TestException::doThrow);
        Retry<Void> retry = new Retry<>(invocation, "test invocation",
                ResultDecision.ALWAYS_EXPECTED, new SetBasedExceptionDecision(exception, SetOfThrowables.EMPTY, false),
                3, 1000, SyncDelay.NONE, AsyncDelay.NONE, stopwatch, null);
        TestThread<Void> result = runOnTestThread(retry, false);
        assertThatThrownBy(result::await).isExactlyInstanceOf(TestException.class);
        assertThat(invocation.numberOfInvocations()).isEqualTo(4);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenRetriedException_moreThanMaxRetries() {
        TestInvocation<Void> invocation = TestInvocation.initiallyFailing(4, RuntimeException::new, TestException::doThrow);
        Retry<Void> retry = new Retry<>(invocation, "test invocation",
                ResultDecision.ALWAYS_EXPECTED, new SetBasedExceptionDecision(exception, SetOfThrowables.EMPTY, false),
                3, 1000, SyncDelay.NONE, AsyncDelay.NONE, stopwatch, null);
        TestThread<Void> result = runOnTestThread(retry, false);
        assertThatThrownBy(result::await).isExactlyInstanceOf(RuntimeException.class);
        assertThat(invocation.numberOfInvocations()).isEqualTo(4);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenAbortingException_lessThanMaxRetries() {
        TestInvocation<Void> invocation = TestInvocation.initiallyFailing(2, RuntimeException::new, TestException::doThrow);
        Retry<Void> retry = new Retry<>(invocation, "test invocation",
                ResultDecision.ALWAYS_EXPECTED, new SetBasedExceptionDecision(exception, testException, false),
                3, 1000, SyncDelay.NONE, AsyncDelay.NONE, stopwatch, null);
        TestThread<Void> result = runOnTestThread(retry, false);
        assertThatThrownBy(result::await).isExactlyInstanceOf(TestException.class);
        assertThat(invocation.numberOfInvocations()).isEqualTo(3);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenAbortingException_equalToMaxRetries() {
        TestInvocation<Void> invocation = TestInvocation.initiallyFailing(3, RuntimeException::new, TestException::doThrow);
        Retry<Void> retry = new Retry<>(invocation, "test invocation",
                ResultDecision.ALWAYS_EXPECTED, new SetBasedExceptionDecision(exception, testException, false),
                3, 1000, SyncDelay.NONE, AsyncDelay.NONE, stopwatch, null);
        TestThread<Void> result = runOnTestThread(retry, false);
        assertThatThrownBy(result::await).isExactlyInstanceOf(TestException.class);
        assertThat(invocation.numberOfInvocations()).isEqualTo(4);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenAbortingException_moreThanMaxRetries() {
        TestInvocation<Void> invocation = TestInvocation.initiallyFailing(4, RuntimeException::new, TestException::doThrow);
        Retry<Void> retry = new Retry<>(invocation, "test invocation",
                ResultDecision.ALWAYS_EXPECTED, new SetBasedExceptionDecision(exception, testException, false),
                3, 1000, SyncDelay.NONE, AsyncDelay.NONE, stopwatch, null);
        TestThread<Void> result = runOnTestThread(retry, false);
        assertThatThrownBy(result::await).isExactlyInstanceOf(RuntimeException.class);
        assertThat(invocation.numberOfInvocations()).isEqualTo(4);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenValue_totalDelayLessThanMaxDuration() throws Exception {
        Barrier startDelayBarrier = Barrier.interruptible();
        Barrier endDelayBarrier = Barrier.interruptible();
        TestDelay delay = TestDelay.normal(startDelayBarrier, endDelayBarrier);
        TestInvocation<String> invocation = TestInvocation.initiallyFailing(3, RuntimeException::new, () -> "foobar");
        Retry<String> retry = new Retry<>(invocation, "test invocation",
                ResultDecision.ALWAYS_EXPECTED, new SetBasedExceptionDecision(exception, SetOfThrowables.EMPTY, false),
                3, 1000, () -> delay, AsyncDelay.NONE, stopwatch, null);
        TestThread<String> result = runOnTestThread(retry, false);
        startDelayBarrier.await();
        stopwatch.setCurrentValue(500);
        endDelayBarrier.open();
        assertThat(result.await()).isEqualTo("foobar");
        assertThat(invocation.numberOfInvocations()).isEqualTo(4);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenValue_totalDelayEqualToMaxDuration() throws InterruptedException {
        Barrier startDelayBarrier = Barrier.interruptible();
        Barrier endDelayBarrier = Barrier.interruptible();
        TestDelay delay = TestDelay.normal(startDelayBarrier, endDelayBarrier);
        TestInvocation<String> invocation = TestInvocation.initiallyFailing(3, RuntimeException::new, () -> "foobar");
        Retry<String> retry = new Retry<>(invocation, "test invocation",
                ResultDecision.ALWAYS_EXPECTED, new SetBasedExceptionDecision(exception, SetOfThrowables.EMPTY, false),
                3, 1000, () -> delay, AsyncDelay.NONE, stopwatch, null);
        TestThread<String> result = runOnTestThread(retry, false);
        startDelayBarrier.await();
        stopwatch.setCurrentValue(1000);
        endDelayBarrier.open();
        assertThatThrownBy(result::await).isExactlyInstanceOf(RuntimeException.class);
        assertThat(invocation.numberOfInvocations()).isEqualTo(1);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenValue_totalDelayMoreThanMaxDuration() throws InterruptedException {
        Barrier startDelayBarrier = Barrier.interruptible();
        Barrier endDelayBarrier = Barrier.interruptible();
        TestDelay delay = TestDelay.normal(startDelayBarrier, endDelayBarrier);
        TestInvocation<String> invocation = TestInvocation.initiallyFailing(3, RuntimeException::new, () -> "foobar");
        Retry<String> retry = new Retry<>(invocation, "test invocation",
                ResultDecision.ALWAYS_EXPECTED, new SetBasedExceptionDecision(exception, SetOfThrowables.EMPTY, false),
                3, 1000, () -> delay, AsyncDelay.NONE, stopwatch, null);
        TestThread<String> result = runOnTestThread(retry, false);
        startDelayBarrier.await();
        stopwatch.setCurrentValue(1500);
        endDelayBarrier.open();
        assertThatThrownBy(result::await).isExactlyInstanceOf(RuntimeException.class);
        assertThat(invocation.numberOfInvocations()).isEqualTo(1);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenRetriedException_totalDelayLessThanMaxDuration()
            throws InterruptedException {
        Barrier startDelayBarrier = Barrier.interruptible();
        Barrier endDelayBarrier = Barrier.interruptible();
        TestDelay delay = TestDelay.normal(startDelayBarrier, endDelayBarrier);
        TestInvocation<Void> invocation = TestInvocation.initiallyFailing(3, RuntimeException::new, TestException::doThrow);
        Retry<Void> retry = new Retry<>(invocation, "test invocation",
                ResultDecision.ALWAYS_EXPECTED, new SetBasedExceptionDecision(exception, SetOfThrowables.EMPTY, false),
                3, 1000, () -> delay, AsyncDelay.NONE, stopwatch, null);
        TestThread<Void> result = runOnTestThread(retry, false);
        startDelayBarrier.await();
        stopwatch.setCurrentValue(500);
        endDelayBarrier.open();
        assertThatThrownBy(result::await).isExactlyInstanceOf(TestException.class);
        assertThat(invocation.numberOfInvocations()).isEqualTo(4);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenRetriedException_totalDelayEqualToMaxDuration()
            throws InterruptedException {
        Barrier startDelayBarrier = Barrier.interruptible();
        Barrier endDelayBarrier = Barrier.interruptible();
        TestDelay delay = TestDelay.normal(startDelayBarrier, endDelayBarrier);
        TestInvocation<Void> invocation = TestInvocation.initiallyFailing(3, RuntimeException::new, TestException::doThrow);
        Retry<Void> retry = new Retry<>(invocation, "test invocation",
                ResultDecision.ALWAYS_EXPECTED, new SetBasedExceptionDecision(exception, SetOfThrowables.EMPTY, false),
                3, 1000, () -> delay, AsyncDelay.NONE, stopwatch, null);
        TestThread<Void> result = runOnTestThread(retry, false);
        startDelayBarrier.await();
        stopwatch.setCurrentValue(1000);
        endDelayBarrier.open();
        assertThatThrownBy(result::await).isExactlyInstanceOf(RuntimeException.class);
        assertThat(invocation.numberOfInvocations()).isEqualTo(1);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenRetriedException_totalDelayMoreThanMaxDuration()
            throws InterruptedException {
        Barrier startDelayBarrier = Barrier.interruptible();
        Barrier endDelayBarrier = Barrier.interruptible();
        TestDelay delay = TestDelay.normal(startDelayBarrier, endDelayBarrier);
        TestInvocation<Void> invocation = TestInvocation.initiallyFailing(3, RuntimeException::new, TestException::doThrow);
        Retry<Void> retry = new Retry<>(invocation, "test invocation",
                ResultDecision.ALWAYS_EXPECTED, new SetBasedExceptionDecision(exception, SetOfThrowables.EMPTY, false),
                3, 1000, () -> delay, AsyncDelay.NONE, stopwatch, null);
        TestThread<Void> result = runOnTestThread(retry, false);
        startDelayBarrier.await();
        stopwatch.setCurrentValue(1500);
        endDelayBarrier.open();
        assertThatThrownBy(result::await).isExactlyInstanceOf(RuntimeException.class);
        assertThat(invocation.numberOfInvocations()).isEqualTo(1);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenAbortingException_totalDelayLessThanMaxDuration()
            throws InterruptedException {
        Barrier startDelayBarrier = Barrier.interruptible();
        Barrier endDelayBarrier = Barrier.interruptible();
        TestDelay delay = TestDelay.normal(startDelayBarrier, endDelayBarrier);
        TestInvocation<Void> invocation = TestInvocation.initiallyFailing(3, RuntimeException::new, TestException::doThrow);
        Retry<Void> retry = new Retry<>(invocation, "test invocation",
                ResultDecision.ALWAYS_EXPECTED, new SetBasedExceptionDecision(exception, testException, false),
                3, 1000, () -> delay, AsyncDelay.NONE, stopwatch, null);
        TestThread<Void> result = runOnTestThread(retry, false);
        startDelayBarrier.await();
        stopwatch.setCurrentValue(500);
        endDelayBarrier.open();
        assertThatThrownBy(result::await).isExactlyInstanceOf(TestException.class);
        assertThat(invocation.numberOfInvocations()).isEqualTo(4);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenAbortingException_totalDelayEqualToMaxDuration()
            throws InterruptedException {
        Barrier startDelayBarrier = Barrier.interruptible();
        Barrier endDelayBarrier = Barrier.interruptible();
        TestDelay delay = TestDelay.normal(startDelayBarrier, endDelayBarrier);
        TestInvocation<Void> invocation = TestInvocation.initiallyFailing(3, RuntimeException::new, TestException::doThrow);
        Retry<Void> retry = new Retry<>(invocation, "test invocation",
                ResultDecision.ALWAYS_EXPECTED, new SetBasedExceptionDecision(exception, testException, false),
                3, 1000, () -> delay, AsyncDelay.NONE, stopwatch, null);
        TestThread<Void> result = runOnTestThread(retry, false);
        startDelayBarrier.await();
        stopwatch.setCurrentValue(1000);
        endDelayBarrier.open();
        assertThatThrownBy(result::await).isExactlyInstanceOf(RuntimeException.class);
        assertThat(invocation.numberOfInvocations()).isEqualTo(1);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenAbortingException_totalDelayMoreThanMaxDuration()
            throws InterruptedException {
        Barrier startDelayBarrier = Barrier.interruptible();
        Barrier endDelayBarrier = Barrier.interruptible();
        TestDelay delay = TestDelay.normal(startDelayBarrier, endDelayBarrier);
        TestInvocation<Void> invocation = TestInvocation.initiallyFailing(3, RuntimeException::new, TestException::doThrow);
        Retry<Void> retry = new Retry<>(invocation, "test invocation",
                ResultDecision.ALWAYS_EXPECTED, new SetBasedExceptionDecision(exception, testException, false),
                3, 1000, () -> delay, AsyncDelay.NONE, stopwatch, null);
        TestThread<Void> result = runOnTestThread(retry, false);
        startDelayBarrier.await();
        stopwatch.setCurrentValue(1500);
        endDelayBarrier.open();
        assertThatThrownBy(result::await).isExactlyInstanceOf(RuntimeException.class);
        assertThat(invocation.numberOfInvocations()).isEqualTo(1);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenValue_infiniteRetries() throws Exception {
        TestInvocation<String> invocation = TestInvocation.initiallyFailing(10, RuntimeException::new, () -> "foobar");
        Retry<String> retry = new Retry<>(invocation, "test invocation",
                ResultDecision.ALWAYS_EXPECTED, new SetBasedExceptionDecision(exception, SetOfThrowables.EMPTY, false),
                -1, 1000, SyncDelay.NONE, AsyncDelay.NONE, stopwatch, null);
        TestThread<String> result = runOnTestThread(retry, false);
        assertThat(result.await()).isEqualTo("foobar");
        assertThat(invocation.numberOfInvocations()).isEqualTo(11);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenAbortingException_infiniteRetries() {
        TestInvocation<Void> invocation = TestInvocation.initiallyFailing(10, RuntimeException::new, TestException::doThrow);
        Retry<Void> retry = new Retry<>(invocation, "test invocation",
                ResultDecision.ALWAYS_EXPECTED, new SetBasedExceptionDecision(exception, testException, false),
                -1, 1000, SyncDelay.NONE, AsyncDelay.NONE, stopwatch, null);
        TestThread<Void> result = runOnTestThread(retry, false);
        assertThatThrownBy(result::await).isExactlyInstanceOf(TestException.class);
        assertThat(invocation.numberOfInvocations()).isEqualTo(11);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenValue_infiniteDuration() throws Exception {
        Barrier startDelayBarrier = Barrier.interruptible();
        Barrier endDelayBarrier = Barrier.interruptible();
        TestDelay delay = TestDelay.normal(startDelayBarrier, endDelayBarrier);
        TestInvocation<String> invocation = TestInvocation.initiallyFailing(3, RuntimeException::new, () -> "foobar");
        Retry<String> retry = new Retry<>(invocation, "test invocation",
                ResultDecision.ALWAYS_EXPECTED, new SetBasedExceptionDecision(exception, SetOfThrowables.EMPTY, false),
                3, -1, () -> delay, AsyncDelay.NONE, stopwatch, null);
        TestThread<String> result = runOnTestThread(retry, false);
        startDelayBarrier.await();
        stopwatch.setCurrentValue(1_000_000_000L);
        endDelayBarrier.open();
        assertThat(result.await()).isEqualTo("foobar");
        assertThat(invocation.numberOfInvocations()).isEqualTo(4);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenRetriedException_infiniteDuration() throws Exception {
        Barrier startDelayBarrier = Barrier.interruptible();
        Barrier endDelayBarrier = Barrier.interruptible();
        TestDelay delay = TestDelay.normal(startDelayBarrier, endDelayBarrier);
        TestInvocation<Void> invocation = TestInvocation.initiallyFailing(3, RuntimeException::new, TestException::doThrow);
        Retry<Void> retry = new Retry<>(invocation, "test invocation",
                ResultDecision.ALWAYS_EXPECTED, new SetBasedExceptionDecision(exception, SetOfThrowables.EMPTY, false),
                3, -1, () -> delay, AsyncDelay.NONE, stopwatch, null);
        TestThread<Void> result = runOnTestThread(retry, false);
        startDelayBarrier.await();
        stopwatch.setCurrentValue(1_000_000_000L);
        endDelayBarrier.open();
        assertThatThrownBy(result::await).isExactlyInstanceOf(TestException.class);
        assertThat(invocation.numberOfInvocations()).isEqualTo(4);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenAbortingException_infiniteDuration() throws InterruptedException {
        Barrier startDelayBarrier = Barrier.interruptible();
        Barrier endDelayBarrier = Barrier.interruptible();
        TestDelay delay = TestDelay.normal(startDelayBarrier, endDelayBarrier);
        TestInvocation<Void> invocation = TestInvocation.initiallyFailing(3, RuntimeException::new, TestException::doThrow);
        Retry<Void> retry = new Retry<>(invocation, "test invocation",
                ResultDecision.ALWAYS_EXPECTED, new SetBasedExceptionDecision(exception, testException, false),
                3, -1, () -> delay, AsyncDelay.NONE, stopwatch, null);
        TestThread<Void> result = runOnTestThread(retry, false);
        startDelayBarrier.await();
        stopwatch.setCurrentValue(1_000_000_000L);
        endDelayBarrier.open();
        assertThatThrownBy(result::await).isExactlyInstanceOf(TestException.class);
        assertThat(invocation.numberOfInvocations()).isEqualTo(4);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenValue_interruptedInInvocation() throws InterruptedException {
        Barrier startInvocationBarrier = Barrier.interruptible();
        Barrier endInvocationBarrier = Barrier.interruptible();
        TestInvocation<String> invocation = TestInvocation.initiallyFailing(3, RuntimeException::new, () -> {
            startInvocationBarrier.open();
            endInvocationBarrier.await();
            return "foobar";
        });
        Retry<String> retry = new Retry<>(invocation, "test invocation",
                ResultDecision.ALWAYS_EXPECTED, new SetBasedExceptionDecision(exception, testException, false),
                3, 1000, SyncDelay.NONE, AsyncDelay.NONE, stopwatch, null);
        TestThread<String> executingThread = runOnTestThread(retry, false);
        startInvocationBarrier.await();
        executingThread.interrupt();
        assertThatThrownBy(executingThread::await).isInstanceOf(InterruptedException.class);
        assertThat(invocation.numberOfInvocations()).isEqualTo(4);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenRetriedException_interruptedInInvocation() throws InterruptedException {
        Barrier startInvocationBarrier = Barrier.interruptible();
        Barrier endInvocationBarrier = Barrier.interruptible();
        TestInvocation<Void> invocation = TestInvocation.initiallyFailing(3, RuntimeException::new, () -> {
            startInvocationBarrier.open();
            endInvocationBarrier.await();
            throw new TestException();
        });
        Retry<Void> retry = new Retry<>(invocation, "test invocation",
                ResultDecision.ALWAYS_EXPECTED, new SetBasedExceptionDecision(exception, SetOfThrowables.EMPTY, false),
                3, 1000, SyncDelay.NONE, AsyncDelay.NONE, stopwatch, null);
        TestThread<Void> executingThread = runOnTestThread(retry, false);
        startInvocationBarrier.await();
        executingThread.interrupt();
        assertThatThrownBy(executingThread::await).isInstanceOf(InterruptedException.class);
        assertThat(invocation.numberOfInvocations()).isEqualTo(4);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenAbortingException_interruptedInInvocation() throws InterruptedException {
        Barrier startInvocationBarrier = Barrier.interruptible();
        Barrier endInvocationBarrier = Barrier.interruptible();
        TestInvocation<Void> invocation = TestInvocation.initiallyFailing(3, RuntimeException::new, () -> {
            startInvocationBarrier.open();
            endInvocationBarrier.await();
            throw new TestException();
        });
        Retry<Void> retry = new Retry<>(invocation, "test invocation",
                ResultDecision.ALWAYS_EXPECTED, new SetBasedExceptionDecision(exception, testException, false),
                3, 1000, SyncDelay.NONE, AsyncDelay.NONE, stopwatch, null);
        TestThread<Void> executingThread = runOnTestThread(retry, false);
        startInvocationBarrier.await();
        executingThread.interrupt();
        assertThatThrownBy(executingThread::await).isInstanceOf(InterruptedException.class);
        assertThat(invocation.numberOfInvocations()).isEqualTo(4);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenValue_interruptedInDelay() throws InterruptedException {
        Barrier startDelayBarrier = Barrier.interruptible();
        Barrier endDelayBarrier = Barrier.interruptible();
        TestDelay delay = TestDelay.normal(startDelayBarrier, endDelayBarrier);
        TestInvocation<String> invocation = TestInvocation.initiallyFailing(3, RuntimeException::new, () -> "foobar");
        Retry<String> retry = new Retry<>(invocation, "test invocation",
                ResultDecision.ALWAYS_EXPECTED, new SetBasedExceptionDecision(exception, testException, false),
                3, 1000, () -> delay, AsyncDelay.NONE, stopwatch, null);
        TestThread<String> executingThread = runOnTestThread(retry, false);
        startDelayBarrier.await();
        executingThread.interrupt();
        assertThatThrownBy(executingThread::await).isInstanceOf(InterruptedException.class);
        assertThat(invocation.numberOfInvocations()).isEqualTo(1);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenRetriedException_interruptedInDelay() throws InterruptedException {
        Barrier startDelayBarrier = Barrier.interruptible();
        Barrier endDelayBarrier = Barrier.interruptible();
        TestDelay delay = TestDelay.normal(startDelayBarrier, endDelayBarrier);
        TestInvocation<Void> invocation = TestInvocation.initiallyFailing(3, RuntimeException::new, TestException::doThrow);
        Retry<Void> retry = new Retry<>(invocation, "test invocation",
                ResultDecision.ALWAYS_EXPECTED, new SetBasedExceptionDecision(exception, SetOfThrowables.EMPTY, false),
                3, 1000, () -> delay, AsyncDelay.NONE, stopwatch, null);
        TestThread<Void> executingThread = runOnTestThread(retry, false);
        startDelayBarrier.await();
        executingThread.interrupt();
        assertThatThrownBy(executingThread::await).isInstanceOf(InterruptedException.class);
        assertThat(invocation.numberOfInvocations()).isEqualTo(1);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenAbortingException_interruptedInDelay() throws InterruptedException {
        Barrier startDelayBarrier = Barrier.interruptible();
        Barrier endDelayBarrier = Barrier.interruptible();
        TestDelay delay = TestDelay.normal(startDelayBarrier, endDelayBarrier);
        TestInvocation<Void> invocation = TestInvocation.initiallyFailing(3, RuntimeException::new, TestException::doThrow);
        Retry<Void> retry = new Retry<>(invocation, "test invocation",
                ResultDecision.ALWAYS_EXPECTED, new SetBasedExceptionDecision(exception, testException, false),
                3, 1000, () -> delay, AsyncDelay.NONE, stopwatch, null);
        TestThread<Void> executingThread = runOnTestThread(retry, false);
        startDelayBarrier.await();
        executingThread.interrupt();
        assertThatThrownBy(executingThread::await).isInstanceOf(InterruptedException.class);
        assertThat(invocation.numberOfInvocations()).isEqualTo(1);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenValue_unexpectedExceptionInDelay() throws InterruptedException {
        Barrier startDelayBarrier = Barrier.interruptible();
        Barrier endDelayBarrier = Barrier.interruptible();
        TestDelay delay = TestDelay.exceptionThrowing(startDelayBarrier, endDelayBarrier, RuntimeException::new);
        TestInvocation<String> invocation = TestInvocation.initiallyFailing(3, RuntimeException::new, () -> "foobar");
        Retry<String> retry = new Retry<>(invocation, "test invocation",
                ResultDecision.ALWAYS_EXPECTED, new SetBasedExceptionDecision(exception, testException, false),
                3, 1000, () -> delay, AsyncDelay.NONE, stopwatch, null);
        TestThread<String> executingThread = runOnTestThread(retry, false);
        startDelayBarrier.await();
        endDelayBarrier.open();
        assertThatThrownBy(executingThread::await).isInstanceOf(RuntimeException.class);
        assertThat(invocation.numberOfInvocations()).isEqualTo(1);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenRetriedException_unexpectedExceptionInDelay() throws InterruptedException {
        Barrier startDelayBarrier = Barrier.interruptible();
        Barrier endDelayBarrier = Barrier.interruptible();
        TestDelay delay = TestDelay.exceptionThrowing(startDelayBarrier, endDelayBarrier, RuntimeException::new);
        TestInvocation<Void> invocation = TestInvocation.initiallyFailing(3, RuntimeException::new, TestException::doThrow);
        Retry<Void> retry = new Retry<>(invocation, "test invocation",
                ResultDecision.ALWAYS_EXPECTED, new SetBasedExceptionDecision(exception, SetOfThrowables.EMPTY, false),
                3, 1000, () -> delay, AsyncDelay.NONE, stopwatch, null);
        TestThread<Void> executingThread = runOnTestThread(retry, false);
        startDelayBarrier.await();
        endDelayBarrier.open();
        assertThatThrownBy(executingThread::await).isInstanceOf(RuntimeException.class);
        assertThat(invocation.numberOfInvocations()).isEqualTo(1);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenAbortingException_unexpectedExceptionInDelay()
            throws InterruptedException {
        Barrier startDelayBarrier = Barrier.interruptible();
        Barrier endDelayBarrier = Barrier.interruptible();
        TestDelay delay = TestDelay.exceptionThrowing(startDelayBarrier, endDelayBarrier, RuntimeException::new);
        TestInvocation<Void> invocation = TestInvocation.initiallyFailing(3, RuntimeException::new, TestException::doThrow);
        Retry<Void> retry = new Retry<>(invocation, "test invocation",
                ResultDecision.ALWAYS_EXPECTED, new SetBasedExceptionDecision(exception, testException, false),
                3, 1000, () -> delay, AsyncDelay.NONE, stopwatch, null);
        TestThread<Void> executingThread = runOnTestThread(retry, false);
        startDelayBarrier.await();
        endDelayBarrier.open();
        assertThatThrownBy(executingThread::await).isInstanceOf(RuntimeException.class);
        assertThat(invocation.numberOfInvocations()).isEqualTo(1);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenSelfInterrupt() throws InterruptedException {
        Barrier startInvocationBarrier = Barrier.interruptible();
        Barrier endInvocationBarrier = Barrier.interruptible();
        TestInvocation<Void> invocation = TestInvocation.initiallyFailing(3, RuntimeException::new, () -> {
            startInvocationBarrier.open();
            endInvocationBarrier.await();
            Thread.currentThread().interrupt();
            throw new RuntimeException();
        });
        Retry<Void> retry = new Retry<>(invocation, "test invocation",
                ResultDecision.ALWAYS_EXPECTED, new SetBasedExceptionDecision(exception, SetOfThrowables.EMPTY, false),
                3, 1000, SyncDelay.NONE, AsyncDelay.NONE, stopwatch, null);
        TestThread<Void> executingThread = runOnTestThread(retry, false);
        startInvocationBarrier.await();
        endInvocationBarrier.open();
        assertThatThrownBy(executingThread::await).isInstanceOf(InterruptedException.class);
        assertThat(invocation.numberOfInvocations()).isEqualTo(4);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenValue_selfInterruptedInDelay() throws InterruptedException {
        Barrier startDelayBarrier = Barrier.interruptible();
        Barrier endDelayBarrier = Barrier.interruptible();
        TestDelay delay = TestDelay.selfInterrupting(startDelayBarrier, endDelayBarrier);
        TestInvocation<String> invocation = TestInvocation.initiallyFailing(3, RuntimeException::new, () -> "foobar");
        Retry<String> retry = new Retry<>(invocation, "test invocation",
                ResultDecision.ALWAYS_EXPECTED, new SetBasedExceptionDecision(exception, testException, false),
                3, 1000, () -> delay, AsyncDelay.NONE, stopwatch, null);
        TestThread<String> executingThread = runOnTestThread(retry, false);
        startDelayBarrier.await();
        endDelayBarrier.open();
        assertThatThrownBy(executingThread::await).isInstanceOf(InterruptedException.class);
        assertThat(invocation.numberOfInvocations()).isEqualTo(1);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenRetriedException_selfInterruptedInDelay() throws InterruptedException {
        Barrier startDelayBarrier = Barrier.interruptible();
        Barrier endDelayBarrier = Barrier.interruptible();
        TestDelay delay = TestDelay.selfInterrupting(startDelayBarrier, endDelayBarrier);
        TestInvocation<Void> invocation = TestInvocation.initiallyFailing(3, RuntimeException::new, TestException::doThrow);
        Retry<Void> retry = new Retry<>(invocation, "test invocation",
                ResultDecision.ALWAYS_EXPECTED, new SetBasedExceptionDecision(exception, SetOfThrowables.EMPTY, false),
                3, 1000, () -> delay, AsyncDelay.NONE, stopwatch, null);
        TestThread<Void> executingThread = runOnTestThread(retry, false);
        startDelayBarrier.await();
        endDelayBarrier.open();
        assertThatThrownBy(executingThread::await).isInstanceOf(InterruptedException.class);
        assertThat(invocation.numberOfInvocations()).isEqualTo(1);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenAbortingException_selfInterruptedInDelay() throws InterruptedException {
        Barrier startDelayBarrier = Barrier.interruptible();
        Barrier endDelayBarrier = Barrier.interruptible();
        TestDelay delay = TestDelay.selfInterrupting(startDelayBarrier, endDelayBarrier);
        TestInvocation<Void> invocation = TestInvocation.initiallyFailing(3, RuntimeException::new, TestException::doThrow);
        Retry<Void> retry = new Retry<>(invocation, "test invocation",
                ResultDecision.ALWAYS_EXPECTED, new SetBasedExceptionDecision(exception, testException, false),
                3, 1000, () -> delay, AsyncDelay.NONE, stopwatch, null);
        TestThread<Void> executingThread = runOnTestThread(retry, false);
        startDelayBarrier.await();
        endDelayBarrier.open();
        assertThatThrownBy(executingThread::await).isInstanceOf(InterruptedException.class);
        assertThat(invocation.numberOfInvocations()).isEqualTo(1);
    }
}
