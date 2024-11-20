package io.smallrye.faulttolerance.core.fallback;

import static io.smallrye.faulttolerance.core.util.SneakyThrow.sneakyThrow;
import static io.smallrye.faulttolerance.core.util.TestThread.runOnTestThread;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.Future;
import io.smallrye.faulttolerance.core.util.ExceptionDecision;
import io.smallrye.faulttolerance.core.util.TestException;
import io.smallrye.faulttolerance.core.util.TestInvocation;
import io.smallrye.faulttolerance.core.util.TestThread;
import io.smallrye.faulttolerance.core.util.party.Party;

public class FallbackSyncTest {
    @Test
    public void immediatelyReturning_allExceptionsSupported_valueThenValue() throws Exception {
        TestInvocation<String> invocation = TestInvocation.of(() -> "foobar");
        Fallback<String> fallback = new Fallback<>(invocation, "test invocation",
                ctx -> Future.of("fallback"), ExceptionDecision.ALWAYS_FAILURE);
        TestThread<String> result = runOnTestThread(fallback, false);
        assertThat(result.await()).isEqualTo("foobar");
    }

    @Test
    public void immediatelyReturning_allExceptionsSupported_valueThenException() throws Exception {
        TestInvocation<String> invocation = TestInvocation.of(() -> "foobar");
        Fallback<String> fallback = new Fallback<>(invocation, "test invocation", ctx -> {
            throw new RuntimeException();
        }, ExceptionDecision.ALWAYS_FAILURE);
        TestThread<String> result = runOnTestThread(fallback, false);
        assertThat(result.await()).isEqualTo("foobar");
    }

    @Test
    public void immediatelyReturning_allExceptionsSupported_exceptionThenValue() throws Exception {
        TestInvocation<String> invocation = TestInvocation.of(TestException::doThrow);
        Fallback<String> fallback = new Fallback<>(invocation, "test invocation",
                ctx -> Future.of("fallback"), ExceptionDecision.ALWAYS_FAILURE);
        TestThread<String> result = runOnTestThread(fallback, false);
        assertThat(result.await()).isEqualTo("fallback");
    }

    @Test
    public void immediatelyReturning_allExceptionsSupported_exceptionThenException() {
        TestInvocation<Void> invocation = TestInvocation.of(TestException::doThrow);
        Fallback<Void> fallback = new Fallback<>(invocation, "test invocation", ctx -> {
            throw new RuntimeException();
        }, ExceptionDecision.ALWAYS_FAILURE);
        TestThread<Void> result = runOnTestThread(fallback, false);
        assertThatThrownBy(result::await).isExactlyInstanceOf(RuntimeException.class);
    }

    @Test
    public void immediatelyReturning_noExceptionSupported_valueThenValue() throws Exception {
        TestInvocation<String> invocation = TestInvocation.of(() -> "foobar");
        Fallback<String> fallback = new Fallback<>(invocation, "test invocation",
                ctx -> Future.of("fallback"), ExceptionDecision.ALWAYS_EXPECTED);
        TestThread<String> result = runOnTestThread(fallback, false);
        assertThat(result.await()).isEqualTo("foobar");
    }

    @Test
    public void immediatelyReturning_noExceptionSupported_valueThenException() throws Exception {
        TestInvocation<String> invocation = TestInvocation.of(() -> "foobar");
        Fallback<String> fallback = new Fallback<>(invocation, "test invocation", ctx -> {
            throw sneakyThrow(new TestException());
        }, ExceptionDecision.ALWAYS_EXPECTED);
        TestThread<String> result = runOnTestThread(fallback, false);
        assertThat(result.await()).isEqualTo("foobar");
    }

    @Test
    public void immediatelyReturning_noExceptionSupported_exceptionThenValue() {
        TestInvocation<String> invocation = TestInvocation.of(TestException::doThrow);
        Fallback<String> fallback = new Fallback<>(invocation, "test invocation",
                ctx -> Future.of("fallback"), ExceptionDecision.ALWAYS_EXPECTED);
        TestThread<String> result = runOnTestThread(fallback, false);
        assertThatThrownBy(result::await).isExactlyInstanceOf(TestException.class);
    }

    @Test
    public void immediatelyReturning_noExceptionSupported_exceptionThenException() {
        TestInvocation<Void> invocation = TestInvocation.of(TestException::doThrow);
        Fallback<Void> fallback = new Fallback<>(invocation, "test invocation", ctx -> {
            throw new RuntimeException();
        }, ExceptionDecision.ALWAYS_EXPECTED);
        TestThread<Void> result = runOnTestThread(fallback, false);
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
        Fallback<String> fallback = new Fallback<>(invocation, "test invocation",
                ctx -> Future.of("fallback"), ExceptionDecision.ALWAYS_FAILURE);
        TestThread<String> executingThread = runOnTestThread(fallback, false);
        party.organizer().waitForAll();
        executingThread.interrupt();
        assertThatThrownBy(executingThread::await).isExactlyInstanceOf(InterruptedException.class);
    }

    @Test
    public void waitingOnParty_interruptedInFallback() throws InterruptedException {
        TestInvocation<String> invocation = TestInvocation.of(TestException::doThrow);
        Party party = Party.create(1);
        FallbackFunction<String> fallbackFunction = ctx -> {
            try {
                party.participant().attend();
                return Future.of("fallback");
            } catch (InterruptedException e) {
                throw sneakyThrow(e);
            }
        };
        Fallback<String> fallback = new Fallback<>(invocation, "test invocation",
                fallbackFunction, ExceptionDecision.ALWAYS_FAILURE);
        TestThread<String> executingThread = runOnTestThread(fallback, false);
        party.organizer().waitForAll();
        executingThread.interrupt();
        assertThatThrownBy(executingThread::await).isExactlyInstanceOf(InterruptedException.class);
    }

    @Test
    public void selfInterruptedInInvocation_value() throws Exception {
        FaultToleranceStrategy<String> invocation = ignored -> {
            Thread.currentThread().interrupt();
            return Future.of("foobar");
        };
        Fallback<String> fallback = new Fallback<>(invocation, "test invocation",
                ctx -> Future.of("fallback"), ExceptionDecision.ALWAYS_FAILURE);
        TestThread<String> result = runOnTestThread(fallback, false);
        assertThat(result.await()).isEqualTo("foobar");
    }

    @Test
    public void selfInterruptedInInvocation_exception() {
        FaultToleranceStrategy<String> invocation = ignored -> {
            Thread.currentThread().interrupt();
            throw new RuntimeException();
        };
        Fallback<String> fallback = new Fallback<>(invocation, "test invocation",
                ctx -> Future.of("fallback"), ExceptionDecision.ALWAYS_FAILURE);
        TestThread<String> result = runOnTestThread(fallback, false);
        assertThatThrownBy(result::await).isExactlyInstanceOf(InterruptedException.class);
    }

    @Test
    public void selfInterruptedInFallback_value() throws Exception {
        TestInvocation<String> invocation = TestInvocation.of(TestException::doThrow);
        FallbackFunction<String> fallbackFunction = ctx -> {
            Thread.currentThread().interrupt();
            return Future.of("fallback");
        };
        Fallback<String> fallback = new Fallback<>(invocation, "test invocation",
                fallbackFunction, ExceptionDecision.ALWAYS_FAILURE);
        TestThread<String> result = runOnTestThread(fallback, false);
        assertThat(result.await()).isEqualTo("fallback");
    }

    @Test
    public void selfInterruptedInFallback_exception() {
        TestInvocation<String> invocation = TestInvocation.of(TestException::doThrow);
        FallbackFunction<String> fallbackFunction = ctx -> {
            Thread.currentThread().interrupt();
            throw new RuntimeException();
        };
        Fallback<String> fallback = new Fallback<>(invocation, "test invocation",
                fallbackFunction, ExceptionDecision.ALWAYS_FAILURE);
        TestThread<String> result = runOnTestThread(fallback, false);
        assertThatThrownBy(result::await).isExactlyInstanceOf(RuntimeException.class);
    }
}
