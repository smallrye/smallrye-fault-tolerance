package io.smallrye.faulttolerance.core.fallback;

import static io.smallrye.faulttolerance.core.util.TestThread.runOnTestThread;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.util.SetOfThrowables;
import io.smallrye.faulttolerance.core.util.TestException;
import io.smallrye.faulttolerance.core.util.TestInvocation;
import io.smallrye.faulttolerance.core.util.TestThread;
import io.smallrye.faulttolerance.core.util.party.Party;

public class FallbackTest {
    @Test
    public void immediatelyReturning_allExceptionsSupported_valueThenValue() throws Exception {
        TestInvocation<String> invocation = TestInvocation.of(() -> "foobar");
        TestThread<String> result = runOnTestThread(new Fallback<>(invocation, "test invocation",
                ctx -> "fallback", SetOfThrowables.ALL, SetOfThrowables.EMPTY));
        assertThat(result.await()).isEqualTo("foobar");
    }

    @Test
    public void immediatelyReturning_allExceptionsSupported_valueThenException() throws Exception {
        TestInvocation<String> invocation = TestInvocation.of(() -> "foobar");
        TestThread<String> result = runOnTestThread(new Fallback<>(invocation, "test invocation",
                ctx -> TestException.doThrow(), SetOfThrowables.ALL, SetOfThrowables.EMPTY));
        assertThat(result.await()).isEqualTo("foobar");
    }

    @Test
    public void immediatelyReturning_allExceptionsSupported_exceptionThenValue() throws Exception {
        TestInvocation<String> invocation = TestInvocation.of(TestException::doThrow);
        TestThread<String> result = runOnTestThread(new Fallback<>(invocation, "test invocation",
                ctx -> "fallback", SetOfThrowables.ALL, SetOfThrowables.EMPTY));
        assertThat(result.await()).isEqualTo("fallback");
    }

    @Test
    public void immediatelyReturning_allExceptionsSupported_exceptionThenException() {
        TestInvocation<Void> invocation = TestInvocation.of(TestException::doThrow);
        TestThread<Void> result = runOnTestThread(new Fallback<>(invocation, "test invocation", ctx -> {
            throw new RuntimeException();
        }, SetOfThrowables.ALL, SetOfThrowables.EMPTY));
        assertThatThrownBy(result::await).isExactlyInstanceOf(RuntimeException.class);
    }

    @Test
    public void immediatelyReturning_noExceptionSupported_valueThenValue() throws Exception {
        TestInvocation<String> invocation = TestInvocation.of(() -> "foobar");
        TestThread<String> result = runOnTestThread(new Fallback<>(invocation, "test invocation",
                ctx -> "fallback", SetOfThrowables.EMPTY, SetOfThrowables.ALL));
        assertThat(result.await()).isEqualTo("foobar");
    }

    @Test
    public void immediatelyReturning_noExceptionSupported_valueThenException() throws Exception {
        TestInvocation<String> invocation = TestInvocation.of(() -> "foobar");
        TestThread<String> result = runOnTestThread(new Fallback<>(invocation, "test invocation",
                ctx -> TestException.doThrow(), SetOfThrowables.EMPTY, SetOfThrowables.ALL));
        assertThat(result.await()).isEqualTo("foobar");
    }

    @Test
    public void immediatelyReturning_noExceptionSupported_exceptionThenValue() throws Exception {
        TestInvocation<String> invocation = TestInvocation.of(TestException::doThrow);
        TestThread<String> result = runOnTestThread(new Fallback<>(invocation, "test invocation",
                ctx -> "fallback", SetOfThrowables.EMPTY, SetOfThrowables.ALL));
        assertThatThrownBy(result::await).isExactlyInstanceOf(TestException.class);
    }

    @Test
    public void immediatelyReturning_noExceptionSupported_exceptionThenException() {
        TestInvocation<Void> invocation = TestInvocation.of(TestException::doThrow);
        TestThread<Void> result = runOnTestThread(new Fallback<>(invocation, "test invocation", ctx -> {
            throw new RuntimeException();
        }, SetOfThrowables.EMPTY, SetOfThrowables.ALL));
        assertThatThrownBy(result::await).isExactlyInstanceOf(TestException.class);
    }

    // testing interruption and especially self-interruption isn't exactly meaningful,
    // the tests just codify existing behavior

    @Test
    public void waitingOnParty_interruptedInInvocation() throws InterruptedException {
        Party party = Party.create(1);
        TestInvocation<String> invocation = TestInvocation.of(() -> {
            party.participant().attend();
            return "foobar";
        });
        TestThread<String> executingThread = runOnTestThread(new Fallback<>(invocation, "test invocation",
                ctx -> "fallback", SetOfThrowables.ALL, SetOfThrowables.EMPTY));
        party.organizer().waitForAll();
        executingThread.interrupt();
        assertThatThrownBy(executingThread::await).isExactlyInstanceOf(InterruptedException.class);
    }

    @Test
    public void waitingOnParty_interruptedInFallback() throws InterruptedException {
        TestInvocation<String> invocation = TestInvocation.of(TestException::doThrow);
        Party party = Party.create(1);
        FallbackFunction<String> fallback = ctx -> {
            party.participant().attend();
            return "fallback";
        };
        TestThread<String> executingThread = runOnTestThread(new Fallback<>(invocation, "test invocation",
                fallback, SetOfThrowables.ALL, SetOfThrowables.EMPTY));
        party.organizer().waitForAll();
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
                ctx -> "fallback", SetOfThrowables.ALL, SetOfThrowables.EMPTY));
        assertThat(result.await()).isEqualTo("foobar");
    }

    @Test
    public void selfInterruptedInInvocation_exception() {
        FaultToleranceStrategy<String> invocation = (ignored) -> {
            Thread.currentThread().interrupt();
            throw new RuntimeException();
        };
        TestThread<String> executingThread = runOnTestThread(new Fallback<>(invocation, "test invocation",
                ctx -> "fallback", SetOfThrowables.ALL, SetOfThrowables.EMPTY));
        assertThatThrownBy(executingThread::await).isExactlyInstanceOf(InterruptedException.class);
    }

    @Test
    public void selfInterruptedInFallback_value() throws Exception {
        TestInvocation<String> invocation = TestInvocation.of(TestException::doThrow);
        FallbackFunction<String> fallback = ctx -> {
            Thread.currentThread().interrupt();
            return "fallback";
        };
        TestThread<String> result = runOnTestThread(new Fallback<>(invocation, "test invocation",
                fallback, SetOfThrowables.ALL, SetOfThrowables.EMPTY));
        assertThat(result.await()).isEqualTo("fallback");
    }

    @Test
    public void selfInterruptedInFallback_exception() {
        TestInvocation<String> invocation = TestInvocation.of(TestException::doThrow);
        FallbackFunction<String> fallback = ctx -> {
            Thread.currentThread().interrupt();
            throw new RuntimeException();
        };
        TestThread<String> executingThread = runOnTestThread(new Fallback<>(invocation, "test invocation",
                fallback, SetOfThrowables.ALL, SetOfThrowables.EMPTY));
        assertThatThrownBy(executingThread::await).isExactlyInstanceOf(RuntimeException.class);
    }
}
