package io.smallrye.faulttolerance.core.fallback;

import static io.smallrye.faulttolerance.core.util.TestThread.runOnTestThread;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.Test;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.util.SetOfThrowables;
import io.smallrye.faulttolerance.core.util.TestException;
import io.smallrye.faulttolerance.core.util.TestThread;
import io.smallrye.faulttolerance.core.util.barrier.Barrier;

public class FallbackTest {
    @Test
    public void immediatelyReturning_valueThenValue() throws Exception {
        TestInvocation<String> invocation = TestInvocation.immediatelyReturning(() -> "foobar");
        TestThread<String> result = runOnTestThread(new Fallback<>(invocation, "test invocation",
                e -> "fallback", SetOfThrowables.ALL, SetOfThrowables.EMPTY, null));
        assertThat(result.await()).isEqualTo("foobar");
    }

    @Test
    public void immediatelyReturning_valueThenException() throws Exception {
        TestInvocation<String> invocation = TestInvocation.immediatelyReturning(() -> "foobar");
        TestThread<String> result = runOnTestThread(new Fallback<>(invocation, "test invocation",
                e -> TestException.doThrow(), SetOfThrowables.ALL, SetOfThrowables.EMPTY, null));
        assertThat(result.await()).isEqualTo("foobar");
    }

    @Test
    public void immediatelyReturning_exceptionThenValue() throws Exception {
        TestInvocation<String> invocation = TestInvocation.immediatelyReturning(TestException::doThrow);
        TestThread<String> result = runOnTestThread(new Fallback<>(invocation, "test invocation",
                e -> "fallback", SetOfThrowables.ALL, SetOfThrowables.EMPTY, null));
        assertThat(result.await()).isEqualTo("fallback");
    }

    @Test
    public void immediatelyReturning_exceptionThenException() {
        TestInvocation<Void> invocation = TestInvocation.immediatelyReturning(TestException::doThrow);
        TestThread<Void> result = runOnTestThread(new Fallback<>(invocation, "test invocation", e -> {
            throw new RuntimeException();
        }, SetOfThrowables.ALL, SetOfThrowables.EMPTY, null));
        assertThatThrownBy(result::await).isExactlyInstanceOf(RuntimeException.class);
    }

    // testing interruption and especially self-interruption isn't exactly meaningful,
    // the tests just codify existing behavior

    @Test
    public void waitingOnBarrier_interruptedInInvocation() throws InterruptedException {
        Barrier startBarrier = Barrier.interruptible();
        Barrier endBarrier = Barrier.interruptible();
        TestInvocation<String> invocation = TestInvocation.waitingOnBarrier(startBarrier, endBarrier, () -> "foobar");
        TestThread<String> executingThread = runOnTestThread(new Fallback<>(invocation, "test invocation",
                e -> "fallback", SetOfThrowables.ALL, SetOfThrowables.EMPTY, null));
        startBarrier.await();
        executingThread.interrupt();
        assertThatThrownBy(executingThread::await).isExactlyInstanceOf(InterruptedException.class);
    }

    @Test
    public void waitingOnBarrier_interruptedInFallback() throws InterruptedException {
        TestInvocation<String> invocation = TestInvocation.immediatelyReturning(TestException::doThrow);
        Barrier startBarrier = Barrier.interruptible();
        Barrier endBarrier = Barrier.interruptible();
        FallbackFunction<String> fallback = e -> {
            startBarrier.open();
            endBarrier.await();
            return "fallback";
        };
        TestThread<String> executingThread = runOnTestThread(new Fallback<>(invocation, "test invocation",
                fallback, SetOfThrowables.ALL, SetOfThrowables.EMPTY, null));
        startBarrier.await();
        executingThread.interrupt();
        assertThatThrownBy(executingThread::await).isExactlyInstanceOf(InterruptedException.class);
    }

    @Test
    public void selfInterruptedInInvocation_value() throws Exception {
        FaultToleranceStrategy<String> invocation = (ignored) -> {
            Thread.currentThread().interrupt();
            return "foobar";
        };
        TestThread<String> result = runOnTestThread(new Fallback<>(invocation, "test invocation",
                e -> "fallback", SetOfThrowables.ALL, SetOfThrowables.EMPTY, null));
        assertThat(result.await()).isEqualTo("foobar");
    }

    @Test
    public void selfInterruptedInInvocation_exception() {
        FaultToleranceStrategy<String> invocation = (ignored) -> {
            Thread.currentThread().interrupt();
            throw new RuntimeException();
        };
        TestThread<String> executingThread = runOnTestThread(new Fallback<>(invocation, "test invocation",
                e -> "fallback", SetOfThrowables.ALL, SetOfThrowables.EMPTY, null));
        assertThatThrownBy(executingThread::await).isExactlyInstanceOf(InterruptedException.class);
    }

    @Test
    public void selfInterruptedInFallback_value() throws Exception {
        TestInvocation<String> invocation = TestInvocation.immediatelyReturning(TestException::doThrow);
        FallbackFunction<String> fallback = e -> {
            Thread.currentThread().interrupt();
            return "fallback";
        };
        TestThread<String> result = runOnTestThread(new Fallback<>(invocation, "test invocation",
                fallback, SetOfThrowables.ALL, SetOfThrowables.EMPTY, null));
        assertThat(result.await()).isEqualTo("fallback");
    }

    @Test
    public void selfInterruptedInFallback_exception() {
        TestInvocation<String> invocation = TestInvocation.immediatelyReturning(TestException::doThrow);
        FallbackFunction<String> fallback = e -> {
            Thread.currentThread().interrupt();
            throw new RuntimeException();
        };
        TestThread<String> executingThread = runOnTestThread(new Fallback<>(invocation, "test invocation",
                fallback, SetOfThrowables.ALL, SetOfThrowables.EMPTY, null));
        assertThatThrownBy(executingThread::await).isExactlyInstanceOf(RuntimeException.class);
    }
}
