package com.github.ladicek.oaken_ocean.core.fallback;

import com.github.ladicek.oaken_ocean.core.util.TestException;
import com.github.ladicek.oaken_ocean.core.util.TestThread;
import com.github.ladicek.oaken_ocean.core.util.barrier.Barrier;
import org.junit.Test;

import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class FallbackTest {
    @Test
    public void immediatelyReturning_valueThenValue() throws Exception {
        TestAction<String> action = TestAction.immediatelyReturning(() -> "foobar");
        TestThread<String> result = TestThread.runOnTestThread(new Fallback<>(action, "test action", () -> "fallback"));
        assertThat(result.await()).isEqualTo("foobar");
    }

    @Test
    public void immediatelyReturning_valueThenException() throws Exception {
        TestAction<String> action = TestAction.immediatelyReturning(() -> "foobar");
        TestThread<String> result = TestThread.runOnTestThread(new Fallback<>(action, "test action", TestException::doThrow));
        assertThat(result.await()).isEqualTo("foobar");
    }

    @Test
    public void immediatelyReturning_exceptionThenValue() throws Exception {
        TestAction<String> action = TestAction.immediatelyReturning(TestException::doThrow);
        TestThread<String> result = TestThread.runOnTestThread(new Fallback<>(action, "test action", () -> "fallback"));
        assertThat(result.await()).isEqualTo("fallback");
    }

    @Test
    public void immediatelyReturning_exceptionThenException() {
        TestAction<Void> action = TestAction.immediatelyReturning(TestException::doThrow);
        TestThread<Void> result = TestThread.runOnTestThread(new Fallback<>(action, "test action", () -> { throw new RuntimeException(); }));
        assertThatThrownBy(result::await).isExactlyInstanceOf(RuntimeException.class);
    }

    @Test
    public void waitingOnBarrier_interruptedInAction() throws InterruptedException {
        Barrier startBarrier = Barrier.interruptible();
        Barrier endBarrier = Barrier.interruptible();
        TestAction<String> action = TestAction.waitingOnBarrier(startBarrier, endBarrier, () -> "foobar");
        TestThread<String> executingThread = TestThread.runOnTestThread(new Fallback<>(action, "test action", () -> "fallback"));
        startBarrier.await();
        executingThread.interrupt();
        assertThatThrownBy(executingThread::await).isExactlyInstanceOf(InterruptedException.class);
    }

    @Test
    public void waitingOnBarrier_interruptedInFallback() throws InterruptedException {
        TestAction<String> action = TestAction.immediatelyReturning(TestException::doThrow);
        Barrier startBarrier = Barrier.interruptible();
        Barrier endBarrier = Barrier.interruptible();
        Callable<String> fallback = () -> {
            startBarrier.open();
            endBarrier.await();
            return "fallback";
        };
        TestThread<String> executingThread = TestThread.runOnTestThread(new Fallback<>(action, "test action", fallback));
        startBarrier.await();
        executingThread.interrupt();
        assertThatThrownBy(executingThread::await).isExactlyInstanceOf(InterruptedException.class);
    }

    @Test
    public void waitingOnBarrier_selfInterruptedInAction() throws InterruptedException {
        Barrier startBarrier = Barrier.interruptible();
        Barrier endBarrier = Barrier.interruptible();
        Callable<String> action = () -> {
            startBarrier.open();
            endBarrier.await();
            Thread.currentThread().interrupt();
            throw new RuntimeException();
        };
        TestThread<String> executingThread = TestThread.runOnTestThread(new Fallback<>(action, "test action", () -> "fallback"));
        startBarrier.await();
        endBarrier.open();
        assertThatThrownBy(executingThread::await).isExactlyInstanceOf(InterruptedException.class);
    }

    @Test
    public void waitingOnBarrier_selfInterruptedInFallback() throws InterruptedException {
        TestAction<String> action = TestAction.immediatelyReturning(TestException::doThrow);
        Barrier startBarrier = Barrier.interruptible();
        Barrier endBarrier = Barrier.interruptible();
        Callable<String> fallback = () -> {
            startBarrier.open();
            endBarrier.await();
            Thread.currentThread().interrupt();
            throw new RuntimeException();
        };
        TestThread<String> executingThread = TestThread.runOnTestThread(new Fallback<>(action, "test action", fallback));
        startBarrier.await();
        endBarrier.open();
        assertThatThrownBy(executingThread::await).isExactlyInstanceOf(InterruptedException.class);
    }
}
