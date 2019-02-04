package com.github.ladicek.oaken_ocean.core.retry;

import com.github.ladicek.oaken_ocean.core.stopwatch.TestStopwatch;
import com.github.ladicek.oaken_ocean.core.util.SetOfThrowables;
import com.github.ladicek.oaken_ocean.core.util.TestException;
import com.github.ladicek.oaken_ocean.core.util.barrier.Barrier;
import com.github.ladicek.oaken_ocean.core.util.TestThread;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceException;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static com.github.ladicek.oaken_ocean.core.util.TestThread.runOnTestThread;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RetryTest {
    private static final SetOfThrowables exception = SetOfThrowables.withoutCustomThrowables(Collections.singletonList(Exception.class));
    private static final SetOfThrowables testException = SetOfThrowables.withoutCustomThrowables(Collections.singletonList(TestException.class));

    private TestStopwatch stopwatch;

    @Before
    public void setUp() {
        stopwatch = new TestStopwatch();
    }

    @Test
    public void immediatelyReturning_value() throws Exception {
        TestAction<String> action = TestAction.immediatelyReturning(() -> "foobar");
        TestThread<String> result = runOnTestThread(new Retry<>(action, "test action",
                SetOfThrowables.EMPTY, SetOfThrowables.EMPTY, 3, 1000, NoDelay.INSTANCE, stopwatch));
        assertThat(result.await()).isEqualTo("foobar");
        assertThat(action.numberOfInvocations()).isEqualTo(1);
    }

    @Test
    public void immediatelyReturning_retriedException() {
        TestAction<Void> action = TestAction.immediatelyReturning(TestException::doThrow);
        TestThread<Void> result = runOnTestThread(new Retry<>(action, "test action",
                exception, SetOfThrowables.EMPTY, 3, 1000, NoDelay.INSTANCE, stopwatch));
        assertThatThrownBy(result::await)
                .isExactlyInstanceOf(FaultToleranceException.class)
                .hasMessage("test action reached max retries or max retry duration");
        assertThat(action.numberOfInvocations()).isEqualTo(4);
    }

    @Test
    public void immediatelyReturning_abortingException() {
        TestAction<Void> action = TestAction.immediatelyReturning(TestException::doThrow);
        TestThread<Void> result = runOnTestThread(new Retry<>(action, "test action",
                exception, testException, 3, 1000, NoDelay.INSTANCE, stopwatch));
        assertThatThrownBy(result::await).isExactlyInstanceOf(TestException.class);
        assertThat(action.numberOfInvocations()).isEqualTo(1);
    }

    @Test
    public void immediatelyReturning_unknownException() {
        TestAction<Void> action = TestAction.immediatelyReturning(TestException::doThrow);
        TestThread<Void> result = runOnTestThread(new Retry<>(action, "test action",
                SetOfThrowables.EMPTY, SetOfThrowables.EMPTY, 3, 1000, NoDelay.INSTANCE, stopwatch));
        assertThatThrownBy(result::await).isExactlyInstanceOf(TestException.class);
        assertThat(action.numberOfInvocations()).isEqualTo(1);
    }

    @Test
    public void immediatelyReturning_interruptedInAction() throws InterruptedException {
        Barrier startActionBarrier = Barrier.interruptible();
        Barrier endActionBarrier = Barrier.interruptible();
        TestAction<String> action = TestAction.immediatelyReturning(() -> {
            startActionBarrier.open();
            endActionBarrier.await();
            return "foobar";
        });
        TestThread<String> executingThread = runOnTestThread(new Retry<>(action, "test action",
                SetOfThrowables.EMPTY, SetOfThrowables.EMPTY, 3, 1000, NoDelay.INSTANCE, stopwatch));
        startActionBarrier.await();
        executingThread.interrupt();
        assertThatThrownBy(executingThread::await).isInstanceOf(InterruptedException.class);
        assertThat(action.numberOfInvocations()).isEqualTo(1);
    }

    @Test
    public void immediatelyReturning_selfInterruptedInAction() throws InterruptedException {
        Barrier startActionBarrier = Barrier.interruptible();
        Barrier endActionBarrier = Barrier.interruptible();
        TestAction<Void> action = TestAction.immediatelyReturning(() -> {
            startActionBarrier.open();
            endActionBarrier.await();
            Thread.currentThread().interrupt();
            throw new RuntimeException();
        });
        TestThread<Void> executingThread = runOnTestThread(new Retry<>(action, "test action",
                SetOfThrowables.EMPTY, SetOfThrowables.EMPTY, 3, 1000, NoDelay.INSTANCE, stopwatch));
        startActionBarrier.await();
        endActionBarrier.open();
        assertThatThrownBy(executingThread::await).isInstanceOf(InterruptedException.class);
        assertThat(action.numberOfInvocations()).isEqualTo(1);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenValue_lessThanMaxRetries() throws Exception {
        TestAction<String> action = TestAction.initiallyFailing(2, RuntimeException::new, () -> "foobar");
        TestThread<String> result = runOnTestThread(new Retry<>(action, "test action",
                exception, SetOfThrowables.EMPTY, 3, 1000, NoDelay.INSTANCE, stopwatch));
        assertThat(result.await()).isEqualTo("foobar");
        assertThat(action.numberOfInvocations()).isEqualTo(3);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenValue_equalToMaxRetries() throws Exception {
        TestAction<String> action = TestAction.initiallyFailing(3, RuntimeException::new, () -> "foobar");
        TestThread<String> result = runOnTestThread(new Retry<>(action, "test action",
                exception, SetOfThrowables.EMPTY, 3, 1000, NoDelay.INSTANCE, stopwatch));
        assertThat(result.await()).isEqualTo("foobar");
        assertThat(action.numberOfInvocations()).isEqualTo(4);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenValue_moreThanMaxRetries() {
        TestAction<String> action = TestAction.initiallyFailing(4, RuntimeException::new, () -> "foobar");
        TestThread<String> result = runOnTestThread(new Retry<>(action, "test action",
                exception, SetOfThrowables.EMPTY, 3, 1000, NoDelay.INSTANCE, stopwatch));
        assertThatThrownBy(result::await)
                .isExactlyInstanceOf(FaultToleranceException.class)
                .hasMessage("test action reached max retries or max retry duration");
        assertThat(action.numberOfInvocations()).isEqualTo(4);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenRetriedException_lessThanMaxRetries() {
        TestAction<Void> action = TestAction.initiallyFailing(2, RuntimeException::new, TestException::doThrow);
        TestThread<Void> result = runOnTestThread(new Retry<>(action, "test action",
                exception, SetOfThrowables.EMPTY, 3, 1000, NoDelay.INSTANCE, stopwatch));
        assertThatThrownBy(result::await)
                .isExactlyInstanceOf(FaultToleranceException.class)
                .hasMessage("test action reached max retries or max retry duration");
        assertThat(action.numberOfInvocations()).isEqualTo(4);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenRetriedException_equalToMaxRetries() {
        TestAction<Void> action = TestAction.initiallyFailing(3, RuntimeException::new, TestException::doThrow);
        TestThread<Void> result = runOnTestThread(new Retry<>(action, "test action",
                exception, SetOfThrowables.EMPTY, 3, 1000, NoDelay.INSTANCE, stopwatch));
        assertThatThrownBy(result::await)
                .isExactlyInstanceOf(FaultToleranceException.class)
                .hasMessage("test action reached max retries or max retry duration");
        assertThat(action.numberOfInvocations()).isEqualTo(4);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenRetriedException_moreThanMaxRetries() {
        TestAction<Void> action = TestAction.initiallyFailing(4, RuntimeException::new, TestException::doThrow);
        TestThread<Void> result = runOnTestThread(new Retry<>(action, "test action",
                exception, SetOfThrowables.EMPTY, 3, 1000, NoDelay.INSTANCE, stopwatch));
        assertThatThrownBy(result::await)
                .isExactlyInstanceOf(FaultToleranceException.class)
                .hasMessage("test action reached max retries or max retry duration");
        assertThat(action.numberOfInvocations()).isEqualTo(4);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenAbortingException_lessThanMaxRetries() {
        TestAction<Void> action = TestAction.initiallyFailing(2, RuntimeException::new, TestException::doThrow);
        TestThread<Void> result = runOnTestThread(new Retry<>(action, "test action",
                exception, testException, 3, 1000, NoDelay.INSTANCE, stopwatch));
        assertThatThrownBy(result::await).isExactlyInstanceOf(TestException.class);
        assertThat(action.numberOfInvocations()).isEqualTo(3);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenAbortingException_equalToMaxRetries() {
        TestAction<Void> action = TestAction.initiallyFailing(3, RuntimeException::new, TestException::doThrow);
        TestThread<Void> result = runOnTestThread(new Retry<>(action, "test action",
                exception, testException, 3, 1000, NoDelay.INSTANCE, stopwatch));
        assertThatThrownBy(result::await).isExactlyInstanceOf(TestException.class);
        assertThat(action.numberOfInvocations()).isEqualTo(4);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenAbortingException_moreThanMaxRetries() {
        TestAction<Void> action = TestAction.initiallyFailing(4, RuntimeException::new, TestException::doThrow);
        TestThread<Void> result = runOnTestThread(new Retry<>(action, "test action",
                exception, testException, 3, 1000, NoDelay.INSTANCE, stopwatch));
        assertThatThrownBy(result::await)
                .isExactlyInstanceOf(FaultToleranceException.class)
                .hasMessage("test action reached max retries or max retry duration");
        assertThat(action.numberOfInvocations()).isEqualTo(4);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenValue_totalDelayLessThanMaxDuration() throws Exception {
        Barrier startDelayBarrier = Barrier.interruptible();
        Barrier endDelayBarrier = Barrier.interruptible();
        TestDelay delay = TestDelay.normal(startDelayBarrier, endDelayBarrier);
        TestAction<String> action = TestAction.initiallyFailing(3, RuntimeException::new, () -> "foobar");
        TestThread<String> result = runOnTestThread(new Retry<>(action, "test action",
                exception, SetOfThrowables.EMPTY, 3, 1000, delay, stopwatch));
        startDelayBarrier.await();
        stopwatch.setCurrentValue(500);
        endDelayBarrier.open();
        assertThat(result.await()).isEqualTo("foobar");
        assertThat(action.numberOfInvocations()).isEqualTo(4);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenValue_totalDelayEqualToMaxDuration() throws InterruptedException {
        Barrier startDelayBarrier = Barrier.interruptible();
        Barrier endDelayBarrier = Barrier.interruptible();
        TestDelay delay = TestDelay.normal(startDelayBarrier, endDelayBarrier);
        TestAction<String> action = TestAction.initiallyFailing(3, RuntimeException::new, () -> "foobar");
        TestThread<String> result = runOnTestThread(new Retry<>(action, "test action",
                exception, SetOfThrowables.EMPTY, 3, 1000, delay, stopwatch));
        startDelayBarrier.await();
        stopwatch.setCurrentValue(1000);
        endDelayBarrier.open();
        assertThatThrownBy(result::await)
                .isExactlyInstanceOf(FaultToleranceException.class)
                .hasMessage("test action reached max retries or max retry duration");
        assertThat(action.numberOfInvocations()).isEqualTo(1);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenValue_totalDelayMoreThanMaxDuration() throws InterruptedException {
        Barrier startDelayBarrier = Barrier.interruptible();
        Barrier endDelayBarrier = Barrier.interruptible();
        TestDelay delay = TestDelay.normal(startDelayBarrier, endDelayBarrier);
        TestAction<String> action = TestAction.initiallyFailing(3, RuntimeException::new, () -> "foobar");
        TestThread<String> result = runOnTestThread(new Retry<>(action, "test action",
                exception, SetOfThrowables.EMPTY, 3, 1000, delay, stopwatch));
        startDelayBarrier.await();
        stopwatch.setCurrentValue(1500);
        endDelayBarrier.open();
        assertThatThrownBy(result::await)
                .isExactlyInstanceOf(FaultToleranceException.class)
                .hasMessage("test action reached max retries or max retry duration");
        assertThat(action.numberOfInvocations()).isEqualTo(1);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenRetriedException_totalDelayLessThanMaxDuration() throws InterruptedException {
        Barrier startDelayBarrier = Barrier.interruptible();
        Barrier endDelayBarrier = Barrier.interruptible();
        TestDelay delay = TestDelay.normal(startDelayBarrier, endDelayBarrier);
        TestAction<Void> action = TestAction.initiallyFailing(3, RuntimeException::new, TestException::doThrow);
        TestThread<Void> result = runOnTestThread(new Retry<>(action, "test action",
                exception, SetOfThrowables.EMPTY, 3, 1000, delay, stopwatch));
        startDelayBarrier.await();
        stopwatch.setCurrentValue(500);
        endDelayBarrier.open();
        assertThatThrownBy(result::await)
                .isExactlyInstanceOf(FaultToleranceException.class)
                .hasMessage("test action reached max retries or max retry duration");
        assertThat(action.numberOfInvocations()).isEqualTo(4);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenRetriedException_totalDelayEqualToMaxDuration() throws InterruptedException {
        Barrier startDelayBarrier = Barrier.interruptible();
        Barrier endDelayBarrier = Barrier.interruptible();
        TestDelay delay = TestDelay.normal(startDelayBarrier, endDelayBarrier);
        TestAction<Void> action = TestAction.initiallyFailing(3, RuntimeException::new, TestException::doThrow);
        TestThread<Void> result = runOnTestThread(new Retry<>(action, "test action",
                exception, SetOfThrowables.EMPTY, 3, 1000, delay, stopwatch));
        startDelayBarrier.await();
        stopwatch.setCurrentValue(1000);
        endDelayBarrier.open();
        assertThatThrownBy(result::await)
                .isExactlyInstanceOf(FaultToleranceException.class)
                .hasMessage("test action reached max retries or max retry duration");
        assertThat(action.numberOfInvocations()).isEqualTo(1);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenRetriedException_totalDelayMoreThanMaxDuration() throws InterruptedException {
        Barrier startDelayBarrier = Barrier.interruptible();
        Barrier endDelayBarrier = Barrier.interruptible();
        TestDelay delay = TestDelay.normal(startDelayBarrier, endDelayBarrier);
        TestAction<Void> action = TestAction.initiallyFailing(3, RuntimeException::new, TestException::doThrow);
        TestThread<Void> result = runOnTestThread(new Retry<>(action, "test action",
                exception, SetOfThrowables.EMPTY, 3, 1000, delay, stopwatch));
        startDelayBarrier.await();
        stopwatch.setCurrentValue(1500);
        endDelayBarrier.open();
        assertThatThrownBy(result::await)
                .isExactlyInstanceOf(FaultToleranceException.class)
                .hasMessage("test action reached max retries or max retry duration");
        assertThat(action.numberOfInvocations()).isEqualTo(1);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenAbortingException_totalDelayLessThanMaxDuration() throws InterruptedException {
        Barrier startDelayBarrier = Barrier.interruptible();
        Barrier endDelayBarrier = Barrier.interruptible();
        TestDelay delay = TestDelay.normal(startDelayBarrier, endDelayBarrier);
        TestAction<Void> action = TestAction.initiallyFailing(3, RuntimeException::new, TestException::doThrow);
        TestThread<Void> result = runOnTestThread(new Retry<>(action, "test action",
                exception, testException, 3, 1000, delay, stopwatch));
        startDelayBarrier.await();
        stopwatch.setCurrentValue(500);
        endDelayBarrier.open();
        assertThatThrownBy(result::await).isExactlyInstanceOf(TestException.class);
        assertThat(action.numberOfInvocations()).isEqualTo(4);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenAbortingException_totalDelayEqualToMaxDuration() throws InterruptedException {
        Barrier startDelayBarrier = Barrier.interruptible();
        Barrier endDelayBarrier = Barrier.interruptible();
        TestDelay delay = TestDelay.normal(startDelayBarrier, endDelayBarrier);
        TestAction<Void> action = TestAction.initiallyFailing(3, RuntimeException::new, TestException::doThrow);
        TestThread<Void> result = runOnTestThread(new Retry<>(action, "test action",
                exception, testException, 3, 1000, delay, stopwatch));
        startDelayBarrier.await();
        stopwatch.setCurrentValue(1000);
        endDelayBarrier.open();
        assertThatThrownBy(result::await)
                .isExactlyInstanceOf(FaultToleranceException.class)
                .hasMessage("test action reached max retries or max retry duration");
        assertThat(action.numberOfInvocations()).isEqualTo(1);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenAbortingException_totalDelayMoreThanMaxDuration() throws InterruptedException {
        Barrier startDelayBarrier = Barrier.interruptible();
        Barrier endDelayBarrier = Barrier.interruptible();
        TestDelay delay = TestDelay.normal(startDelayBarrier, endDelayBarrier);
        TestAction<Void> action = TestAction.initiallyFailing(3, RuntimeException::new, TestException::doThrow);
        TestThread<Void> result = runOnTestThread(new Retry<>(action, "test action",
                exception, testException, 3, 1000, delay, stopwatch));
        startDelayBarrier.await();
        stopwatch.setCurrentValue(1500);
        endDelayBarrier.open();
        assertThatThrownBy(result::await)
                .isExactlyInstanceOf(FaultToleranceException.class)
                .hasMessage("test action reached max retries or max retry duration");
        assertThat(action.numberOfInvocations()).isEqualTo(1);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenValue_infiniteRetries() throws Exception {
        TestAction<String> action = TestAction.initiallyFailing(10, RuntimeException::new, () -> "foobar");
        TestThread<String> result = runOnTestThread(new Retry<>(action, "test action",
                exception, SetOfThrowables.EMPTY, -1, 1000, NoDelay.INSTANCE, stopwatch));
        assertThat(result.await()).isEqualTo("foobar");
        assertThat(action.numberOfInvocations()).isEqualTo(11);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenAbortingException_infiniteRetries() {
        TestAction<Void> action = TestAction.initiallyFailing(10, RuntimeException::new, TestException::doThrow);
        TestThread<Void> result = runOnTestThread(new Retry<>(action, "test action",
                exception, testException, -1, 1000, NoDelay.INSTANCE, stopwatch));
        assertThatThrownBy(result::await).isExactlyInstanceOf(TestException.class);
        assertThat(action.numberOfInvocations()).isEqualTo(11);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenValue_infiniteDuration() throws Exception {
        Barrier startDelayBarrier = Barrier.interruptible();
        Barrier endDelayBarrier = Barrier.interruptible();
        TestDelay delay = TestDelay.normal(startDelayBarrier, endDelayBarrier);
        TestAction<String> action = TestAction.initiallyFailing(3, RuntimeException::new, () -> "foobar");
        TestThread<String> result = runOnTestThread(new Retry<>(action, "test action",
                exception, SetOfThrowables.EMPTY, 3, -1, delay, stopwatch));
        startDelayBarrier.await();
        stopwatch.setCurrentValue(1_000_000_000L);
        endDelayBarrier.open();
        assertThat(result.await()).isEqualTo("foobar");
        assertThat(action.numberOfInvocations()).isEqualTo(4);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenRetriedException_infiniteDuration() throws Exception {
        Barrier startDelayBarrier = Barrier.interruptible();
        Barrier endDelayBarrier = Barrier.interruptible();
        TestDelay delay = TestDelay.normal(startDelayBarrier, endDelayBarrier);
        TestAction<Void> action = TestAction.initiallyFailing(3, RuntimeException::new, TestException::doThrow);
        TestThread<Void> result = runOnTestThread(new Retry<>(action, "test action",
                exception, SetOfThrowables.EMPTY, 3, -1, delay, stopwatch));
        startDelayBarrier.await();
        stopwatch.setCurrentValue(1_000_000_000L);
        endDelayBarrier.open();
        assertThatThrownBy(result::await)
                .isExactlyInstanceOf(FaultToleranceException.class)
                .hasMessage("test action reached max retries or max retry duration");
        assertThat(action.numberOfInvocations()).isEqualTo(4);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenAbortingException_infiniteDuration() throws InterruptedException {
        Barrier startDelayBarrier = Barrier.interruptible();
        Barrier endDelayBarrier = Barrier.interruptible();
        TestDelay delay = TestDelay.normal(startDelayBarrier, endDelayBarrier);
        TestAction<Void> action = TestAction.initiallyFailing(3, RuntimeException::new, TestException::doThrow);
        TestThread<Void> result = runOnTestThread(new Retry<>(action, "test action",
                exception, testException, 3, -1, delay, stopwatch));
        startDelayBarrier.await();
        stopwatch.setCurrentValue(1_000_000_000L);
        endDelayBarrier.open();
        assertThatThrownBy(result::await).isExactlyInstanceOf(TestException.class);
        assertThat(action.numberOfInvocations()).isEqualTo(4);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenValue_interruptedInAction() throws InterruptedException {
        Barrier startActionBarrier = Barrier.interruptible();
        Barrier endActionBarrier = Barrier.interruptible();
        TestAction<String> action = TestAction.initiallyFailing(3, RuntimeException::new, () -> {
            startActionBarrier.open();
            endActionBarrier.await();
            return "foobar";
        });
        TestThread<String> executingThread = runOnTestThread(new Retry<>(action, "test action",
                exception, testException, 3, 1000, NoDelay.INSTANCE, stopwatch));
        startActionBarrier.await();
        executingThread.interrupt();
        assertThatThrownBy(executingThread::await).isInstanceOf(InterruptedException.class);
        assertThat(action.numberOfInvocations()).isEqualTo(4);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenRetriedException_interruptedInAction() throws InterruptedException {
        Barrier startActionBarrier = Barrier.interruptible();
        Barrier endActionBarrier = Barrier.interruptible();
        TestAction<Void> action = TestAction.initiallyFailing(3, RuntimeException::new, () -> {
            startActionBarrier.open();
            endActionBarrier.await();
            throw new TestException();
        });
        TestThread<Void> executingThread = runOnTestThread(new Retry<>(action, "test action",
                exception, SetOfThrowables.EMPTY, 3, 1000, NoDelay.INSTANCE, stopwatch));
        startActionBarrier.await();
        executingThread.interrupt();
        assertThatThrownBy(executingThread::await).isInstanceOf(InterruptedException.class);
        assertThat(action.numberOfInvocations()).isEqualTo(4);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenAbortingException_interruptedInAction() throws InterruptedException {
        Barrier startActionBarrier = Barrier.interruptible();
        Barrier endActionBarrier = Barrier.interruptible();
        TestAction<Void> action = TestAction.initiallyFailing(3, RuntimeException::new, () -> {
            startActionBarrier.open();
            endActionBarrier.await();
            throw new TestException();
        });
        TestThread<Void> executingThread = runOnTestThread(new Retry<>(action, "test action",
                exception, testException, 3, 1000, NoDelay.INSTANCE, stopwatch));
        startActionBarrier.await();
        executingThread.interrupt();
        assertThatThrownBy(executingThread::await).isInstanceOf(InterruptedException.class);
        assertThat(action.numberOfInvocations()).isEqualTo(4);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenValue_interruptedInDelay() throws InterruptedException {
        Barrier startDelayBarrier = Barrier.interruptible();
        Barrier endDelayBarrier = Barrier.interruptible();
        TestDelay delay = TestDelay.normal(startDelayBarrier, endDelayBarrier);
        TestAction<String> action = TestAction.initiallyFailing(3, RuntimeException::new, () -> "foobar");
        TestThread<String> executingThread = runOnTestThread(new Retry<>(action, "test action",
                exception, testException, 3, 1000, delay, stopwatch));
        startDelayBarrier.await();
        executingThread.interrupt();
        assertThatThrownBy(executingThread::await).isInstanceOf(InterruptedException.class);
        assertThat(action.numberOfInvocations()).isEqualTo(1);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenRetriedException_interruptedInDelay() throws InterruptedException {
        Barrier startDelayBarrier = Barrier.interruptible();
        Barrier endDelayBarrier = Barrier.interruptible();
        TestDelay delay = TestDelay.normal(startDelayBarrier, endDelayBarrier);
        TestAction<Void> action = TestAction.initiallyFailing(3, RuntimeException::new, TestException::doThrow);
        TestThread<Void> executingThread = runOnTestThread(new Retry<>(action, "test action",
                exception, SetOfThrowables.EMPTY, 3, 1000, delay, stopwatch));
        startDelayBarrier.await();
        executingThread.interrupt();
        assertThatThrownBy(executingThread::await).isInstanceOf(InterruptedException.class);
        assertThat(action.numberOfInvocations()).isEqualTo(1);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenAbortingException_interruptedInDelay() throws InterruptedException {
        Barrier startDelayBarrier = Barrier.interruptible();
        Barrier endDelayBarrier = Barrier.interruptible();
        TestDelay delay = TestDelay.normal(startDelayBarrier, endDelayBarrier);
        TestAction<Void> action = TestAction.initiallyFailing(3, RuntimeException::new, TestException::doThrow);
        TestThread<Void> executingThread = runOnTestThread(new Retry<>(action, "test action",
                exception, testException, 3, 1000, delay, stopwatch));
        startDelayBarrier.await();
        executingThread.interrupt();
        assertThatThrownBy(executingThread::await).isInstanceOf(InterruptedException.class);
        assertThat(action.numberOfInvocations()).isEqualTo(1);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenValue_unexpectedExceptionInDelay() throws InterruptedException {
        Barrier startDelayBarrier = Barrier.interruptible();
        Barrier endDelayBarrier = Barrier.interruptible();
        TestDelay delay = TestDelay.exceptionThrowing(startDelayBarrier, endDelayBarrier, RuntimeException::new);
        TestAction<String> action = TestAction.initiallyFailing(3, RuntimeException::new, () -> "foobar");
        TestThread<String> executingThread = runOnTestThread(new Retry<>(action, "test action",
                exception, testException, 3, 1000, delay, stopwatch));
        startDelayBarrier.await();
        endDelayBarrier.open();
        assertThatThrownBy(executingThread::await).isInstanceOf(RuntimeException.class);
        assertThat(action.numberOfInvocations()).isEqualTo(1);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenRetriedException_unexpectedExceptionInDelay() throws InterruptedException {
        Barrier startDelayBarrier = Barrier.interruptible();
        Barrier endDelayBarrier = Barrier.interruptible();
        TestDelay delay = TestDelay.exceptionThrowing(startDelayBarrier, endDelayBarrier, RuntimeException::new);
        TestAction<Void> action = TestAction.initiallyFailing(3, RuntimeException::new, TestException::doThrow);
        TestThread<Void> executingThread = runOnTestThread(new Retry<>(action, "test action",
                exception, SetOfThrowables.EMPTY, 3, 1000, delay, stopwatch));
        startDelayBarrier.await();
        endDelayBarrier.open();
        assertThatThrownBy(executingThread::await).isInstanceOf(RuntimeException.class);
        assertThat(action.numberOfInvocations()).isEqualTo(1);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenAbortingException_unexpectedExceptionInDelay() throws InterruptedException {
        Barrier startDelayBarrier = Barrier.interruptible();
        Barrier endDelayBarrier = Barrier.interruptible();
        TestDelay delay = TestDelay.exceptionThrowing(startDelayBarrier, endDelayBarrier, RuntimeException::new);
        TestAction<Void> action = TestAction.initiallyFailing(3, RuntimeException::new, TestException::doThrow);
        TestThread<Void> executingThread = runOnTestThread(new Retry<>(action, "test action",
                exception, testException, 3, 1000, delay, stopwatch));
        startDelayBarrier.await();
        endDelayBarrier.open();
        assertThatThrownBy(executingThread::await).isInstanceOf(RuntimeException.class);
        assertThat(action.numberOfInvocations()).isEqualTo(1);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenSelfInterrupt() throws InterruptedException {
        Barrier startActionBarrier = Barrier.interruptible();
        Barrier endActionBarrier = Barrier.interruptible();
        TestAction<Void> action = TestAction.initiallyFailing(3, RuntimeException::new, () -> {
            startActionBarrier.open();
            endActionBarrier.await();
            Thread.currentThread().interrupt();
            throw new RuntimeException();
        });
        TestThread<Void> executingThread = runOnTestThread(new Retry<>(action, "test action",
                exception, SetOfThrowables.EMPTY, 3, 1000, NoDelay.INSTANCE, stopwatch));
        startActionBarrier.await();
        endActionBarrier.open();
        assertThatThrownBy(executingThread::await).isInstanceOf(InterruptedException.class);
        assertThat(action.numberOfInvocations()).isEqualTo(4);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenValue_selfInterruptedInDelay() throws InterruptedException {
        Barrier startDelayBarrier = Barrier.interruptible();
        Barrier endDelayBarrier = Barrier.interruptible();
        TestDelay delay = TestDelay.selfInterrupting(startDelayBarrier, endDelayBarrier);
        TestAction<String> action = TestAction.initiallyFailing(3, RuntimeException::new, () -> "foobar");
        TestThread<String> executingThread = runOnTestThread(new Retry<>(action, "test action",
                exception, testException, 3, 1000, delay, stopwatch));
        startDelayBarrier.await();
        endDelayBarrier.open();
        assertThatThrownBy(executingThread::await).isInstanceOf(InterruptedException.class);
        assertThat(action.numberOfInvocations()).isEqualTo(1);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenRetriedException_selfInterruptedInDelay() throws InterruptedException {
        Barrier startDelayBarrier = Barrier.interruptible();
        Barrier endDelayBarrier = Barrier.interruptible();
        TestDelay delay = TestDelay.selfInterrupting(startDelayBarrier, endDelayBarrier);
        TestAction<Void> action = TestAction.initiallyFailing(3, RuntimeException::new, TestException::doThrow);
        TestThread<Void> executingThread = runOnTestThread(new Retry<>(action, "test action",
                exception, SetOfThrowables.EMPTY, 3, 1000, delay, stopwatch));
        startDelayBarrier.await();
        endDelayBarrier.open();
        assertThatThrownBy(executingThread::await).isInstanceOf(InterruptedException.class);
        assertThat(action.numberOfInvocations()).isEqualTo(1);
    }

    @Test
    public void initiallyFailing_retriedExceptionThenAbortingException_selfInterruptedInDelay() throws InterruptedException {
        Barrier startDelayBarrier = Barrier.interruptible();
        Barrier endDelayBarrier = Barrier.interruptible();
        TestDelay delay = TestDelay.selfInterrupting(startDelayBarrier, endDelayBarrier);
        TestAction<Void> action = TestAction.initiallyFailing(3, RuntimeException::new, TestException::doThrow);
        TestThread<Void> executingThread = runOnTestThread(new Retry<>(action, "test action",
                exception, testException, 3, 1000, delay, stopwatch));
        startDelayBarrier.await();
        endDelayBarrier.open();
        assertThatThrownBy(executingThread::await).isInstanceOf(InterruptedException.class);
        assertThat(action.numberOfInvocations()).isEqualTo(1);
    }
}
