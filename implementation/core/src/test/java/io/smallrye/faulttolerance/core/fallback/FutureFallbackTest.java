package io.smallrye.faulttolerance.core.fallback;

import static io.smallrye.faulttolerance.core.util.CompletionStages.failedFuture;
import static io.smallrye.faulttolerance.core.util.TestThread.runOnTestThread;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.Future;

import org.junit.Test;

import io.smallrye.faulttolerance.core.util.SetOfThrowables;
import io.smallrye.faulttolerance.core.util.TestException;
import io.smallrye.faulttolerance.core.util.TestThread;

public class FutureFallbackTest {
    @Test
    public void shouldNotFallBackOnFailingFuture() throws Exception {
        RuntimeException forcedException = new RuntimeException();
        TestInvocation<Future<String>> invocation = TestInvocation.immediatelyReturning(
                () -> failedFuture(forcedException));
        TestThread<Future<String>> result = runOnTestThread(new Fallback<>(invocation, "test invocation",
                this::fallback, SetOfThrowables.ALL, SetOfThrowables.EMPTY, null));
        Future<String> future = result.await();
        assertThatThrownBy(future::get).hasCause(forcedException);
    }

    @Test
    public void shouldFallbackOnFailureToCreateFuture() throws Exception {
        RuntimeException forcedException = new RuntimeException();
        TestInvocation<Future<String>> invocation = TestInvocation.immediatelyReturning(() -> {
            throw forcedException;
        });
        TestThread<Future<String>> result = runOnTestThread(new Fallback<>(invocation, "test invocation",
                this::fallback, SetOfThrowables.ALL, SetOfThrowables.EMPTY, null));
        Future<String> await = result.await();
        assertThat(await.get()).isEqualTo("fallback");
    }

    @Test
    public void shouldSucceed() throws Exception {
        TestInvocation<Future<String>> invocation = TestInvocation.immediatelyReturning(() -> completedFuture("invocation"));
        TestThread<Future<String>> result = runOnTestThread(new Fallback<>(invocation, "test invocation",
                this::fallback, SetOfThrowables.ALL, SetOfThrowables.EMPTY, null));
        Future<String> future = result.await();
        assertThat(future.get()).isEqualTo("invocation");
    }

    @Test
    public void immediatelyReturning_exceptionThenException() {
        TestInvocation<Void> invocation = TestInvocation.immediatelyReturning(TestException::doThrow);
        TestThread<Void> result = runOnTestThread(new Fallback<>(invocation, "test invocation", e -> {
            throw new RuntimeException();
        }, SetOfThrowables.ALL, SetOfThrowables.EMPTY, null));
        assertThatThrownBy(result::await).isExactlyInstanceOf(RuntimeException.class);
    }

    private Future<String> fallback(Throwable e) {
        return completedFuture("fallback");
    }
}
