package io.smallrye.faulttolerance.core.fallback;

import static io.smallrye.faulttolerance.core.util.TestThread.runOnTestThread;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.core.FailureContext;
import io.smallrye.faulttolerance.core.util.ExceptionDecision;
import io.smallrye.faulttolerance.core.util.TestException;
import io.smallrye.faulttolerance.core.util.TestInvocation;
import io.smallrye.faulttolerance.core.util.TestThread;

public class FutureFallbackTest {
    @Test
    public void shouldNotFallBackOnFailingFuture() throws Exception {
        RuntimeException forcedException = new RuntimeException();
        TestInvocation<Future<String>> invocation = TestInvocation.of(
                () -> failedFuture(forcedException));
        TestThread<Future<String>> result = runOnTestThread(new Fallback<>(invocation, "test invocation",
                this::fallback, ExceptionDecision.ALWAYS_FAILURE));
        Future<String> future = result.await();
        assertThatThrownBy(future::get).hasCause(forcedException);
    }

    @Test
    public void shouldFallbackOnFailureToCreateFuture() throws Exception {
        RuntimeException forcedException = new RuntimeException();
        TestInvocation<Future<String>> invocation = TestInvocation.of(() -> {
            throw forcedException;
        });
        TestThread<Future<String>> result = runOnTestThread(new Fallback<>(invocation, "test invocation",
                this::fallback, ExceptionDecision.ALWAYS_FAILURE));
        Future<String> await = result.await();
        assertThat(await.get()).isEqualTo("fallback");
    }

    @Test
    public void shouldSucceed() throws Exception {
        TestInvocation<Future<String>> invocation = TestInvocation.of(() -> completedFuture("invocation"));
        TestThread<Future<String>> result = runOnTestThread(new Fallback<>(invocation, "test invocation",
                this::fallback, ExceptionDecision.ALWAYS_FAILURE));
        Future<String> future = result.await();
        assertThat(future.get()).isEqualTo("invocation");
    }

    @Test
    public void immediatelyReturning_exceptionThenException() {
        TestInvocation<Void> invocation = TestInvocation.of(TestException::doThrow);
        TestThread<Void> result = runOnTestThread(new Fallback<>(invocation, "test invocation", e -> {
            throw new RuntimeException();
        }, ExceptionDecision.ALWAYS_FAILURE));
        assertThatThrownBy(result::await).isExactlyInstanceOf(RuntimeException.class);
    }

    private Future<String> fallback(FailureContext ctx) {
        return completedFuture("fallback");
    }
}
