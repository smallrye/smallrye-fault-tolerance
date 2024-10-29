package io.smallrye.faulttolerance.core.fallback;

import static io.smallrye.faulttolerance.core.util.TestThread.runOnTestThread;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.core.FailureContext;
import io.smallrye.faulttolerance.core.Future;
import io.smallrye.faulttolerance.core.util.ExceptionDecision;
import io.smallrye.faulttolerance.core.util.TestException;
import io.smallrye.faulttolerance.core.util.TestInvocation;
import io.smallrye.faulttolerance.core.util.TestThread;

public class FallbackFutureTest {
    @Test
    public void shouldNotFallBackOnFailingFuture() throws Exception {
        TestInvocation<java.util.concurrent.Future<String>> invocation = TestInvocation.of(() -> {
            return failedFuture(new RuntimeException());
        });
        Fallback<java.util.concurrent.Future<String>> fallback = new Fallback<>(invocation, "test invocation",
                this::fallback, ExceptionDecision.ALWAYS_FAILURE);
        TestThread<java.util.concurrent.Future<String>> result = runOnTestThread(fallback, false);
        java.util.concurrent.Future<String> future = result.await();
        assertThatThrownBy(future::get).hasCauseInstanceOf(RuntimeException.class);
    }

    @Test
    public void shouldFallbackOnFailureToCreateFuture() throws Exception {
        TestInvocation<java.util.concurrent.Future<String>> invocation = TestInvocation.of(() -> {
            throw new RuntimeException();
        });
        Fallback<java.util.concurrent.Future<String>> fallback = new Fallback<>(invocation, "test invocation",
                this::fallback, ExceptionDecision.ALWAYS_FAILURE);
        TestThread<java.util.concurrent.Future<String>> result = runOnTestThread(fallback, false);
        java.util.concurrent.Future<String> await = result.await();
        assertThat(await.get()).isEqualTo("fallback");
    }

    @Test
    public void shouldSucceed() throws Exception {
        TestInvocation<java.util.concurrent.Future<String>> invocation = TestInvocation.of(() -> {
            return completedFuture("invocation");
        });
        Fallback<java.util.concurrent.Future<String>> fallback = new Fallback<>(invocation, "test invocation",
                this::fallback, ExceptionDecision.ALWAYS_FAILURE);
        TestThread<java.util.concurrent.Future<String>> result = runOnTestThread(fallback, false);
        java.util.concurrent.Future<String> future = result.await();
        assertThat(future.get()).isEqualTo("invocation");
    }

    @Test
    public void immediatelyReturning_exceptionThenException() {
        TestInvocation<Void> invocation = TestInvocation.of(TestException::doThrow);
        Fallback<Void> fallback = new Fallback<>(invocation, "test invocation", e -> {
            throw new RuntimeException();
        }, ExceptionDecision.ALWAYS_FAILURE);
        TestThread<Void> result = runOnTestThread(fallback, false);
        assertThatThrownBy(result::await).isExactlyInstanceOf(RuntimeException.class);
    }

    private Future<java.util.concurrent.Future<String>> fallback(FailureContext ctx) {
        return Future.of(completedFuture("fallback"));
    }
}
