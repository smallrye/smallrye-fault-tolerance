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
        TestThread<String> result = TestThread.runOnTestThread(new Fallback<>(action, "test action", e -> "fallback"));
        assertThat(result.await()).isEqualTo("foobar");
    }

    @Test
    public void immediatelyReturning_valueThenException() throws Exception {
        TestAction<String> action = TestAction.immediatelyReturning(() -> "foobar");
        TestThread<String> result = TestThread.runOnTestThread(new Fallback<>(action, "test action", e -> TestException.doThrow()));
        assertThat(result.await()).isEqualTo("foobar");
    }

    @Test
    public void immediatelyReturning_exceptionThenValue() throws Exception {
        TestAction<String> action = TestAction.immediatelyReturning(TestException::doThrow);
        TestThread<String> result = TestThread.runOnTestThread(new Fallback<>(action, "test action", e -> "fallback"));
        assertThat(result.await()).isEqualTo("fallback");
    }

    @Test
    public void immediatelyReturning_exceptionThenException() {
        TestAction<Void> action = TestAction.immediatelyReturning(TestException::doThrow);
        TestThread<Void> result = TestThread.runOnTestThread(new Fallback<>(action, "test action", e -> { throw new RuntimeException(); }));
        assertThatThrownBy(result::await).isExactlyInstanceOf(RuntimeException.class);
    }

    // testing interruption and especially self-interruption isn't exactly meaningful,
    // the tests just codify existing behavior

    @Test
    public void waitingOnBarrier_interruptedInAction() throws InterruptedException {
        Barrier startBarrier = Barrier.interruptible();
        Barrier endBarrier = Barrier.interruptible();
        TestAction<String> action = TestAction.waitingOnBarrier(startBarrier, endBarrier, () -> "foobar");
        TestThread<String> executingThread = TestThread.runOnTestThread(new Fallback<>(action, "test action", e -> "fallback"));
        startBarrier.await();
        executingThread.interrupt();
        assertThatThrownBy(executingThread::await).isExactlyInstanceOf(InterruptedException.class);
    }

    @Test
    public void waitingOnBarrier_interruptedInFallback() throws InterruptedException {
        TestAction<String> action = TestAction.immediatelyReturning(TestException::doThrow);
        Barrier startBarrier = Barrier.interruptible();
        Barrier endBarrier = Barrier.interruptible();
        FallbackFunction<String> fallback = e -> {
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
    public void selfInterruptedInAction_value() throws Exception {
        Callable<String> action = () -> {
            Thread.currentThread().interrupt();
            return "foobar";
        };
        TestThread<String> result = TestThread.runOnTestThread(new Fallback<>(action, "test action", e -> "fallback"));
        assertThat(result.await()).isEqualTo("foobar");
    }

    @Test
    public void selfInterruptedInAction_exception() {
        Callable<String> action = () -> {
            Thread.currentThread().interrupt();
            throw new RuntimeException();
        };
        TestThread<String> executingThread = TestThread.runOnTestThread(new Fallback<>(action, "test action", e -> "fallback"));
        assertThatThrownBy(executingThread::await).isExactlyInstanceOf(InterruptedException.class);
    }


    @Test
    public void selfInterruptedInFallback_value() throws Exception {
        TestAction<String> action = TestAction.immediatelyReturning(TestException::doThrow);
        FallbackFunction<String> fallback = e -> {
            Thread.currentThread().interrupt();
            return "fallback";
        };
        TestThread<String> result = TestThread.runOnTestThread(new Fallback<>(action, "test action", fallback));
        assertThat(result.await()).isEqualTo("fallback");
    }

    @Test
    public void selfInterruptedInFallback_exception() {
        TestAction<String> action = TestAction.immediatelyReturning(TestException::doThrow);
        FallbackFunction<String> fallback = e -> {
            Thread.currentThread().interrupt();
            throw new RuntimeException();
        };
        TestThread<String> executingThread = TestThread.runOnTestThread(new Fallback<>(action, "test action", fallback));
        assertThatThrownBy(executingThread::await).isExactlyInstanceOf(RuntimeException.class);
    }
}
