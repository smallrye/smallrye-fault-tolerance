package com.github.ladicek.oaken_ocean.core.fallback;

import static com.github.ladicek.oaken_ocean.core.util.TestThread.runOnTestThread;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.junit.Test;

import com.github.ladicek.oaken_ocean.core.util.TestException;
import com.github.ladicek.oaken_ocean.core.util.TestThread;

public class FutureFallbackTest {
    @Test
    public void shouldNotFallBackOnFailingFuture() throws Exception {
        RuntimeException forcedException = new RuntimeException();
        FutureTestInvocation<String> invocation = FutureTestInvocation.immediatelyReturning(
                () -> CompletableFuture.<String> supplyAsync(() -> {
                    throw forcedException;
                }).toCompletableFuture());
        TestThread<Future<String>> result = runOnTestThread(
                new FutureFallback<>(invocation, "test invocation", this::fallback, null));
        Future<String> future = result.await();
        assertThatThrownBy(future::get).hasCause(forcedException);
    }

    @Test
    public void shouldFallbackOnFailureToCreateFuture() throws Exception {
        RuntimeException forcedException = new RuntimeException();
        FutureTestInvocation<String> invocation = FutureTestInvocation.immediatelyReturning(() -> {
            throw forcedException;
        });
        TestThread<Future<String>> result = runOnTestThread(
                new FutureFallback<>(invocation, "test invocation", this::fallback, null));
        Future<String> await = result.await();
        assertThat(await.get()).isEqualTo("fallback");
    }

    @Test
    public void shouldSucceed() throws Exception {
        FutureTestInvocation<String> invocation = FutureTestInvocation
                .immediatelyReturning(() -> CompletableFuture.completedFuture("invocation"));
        TestThread<Future<String>> result = runOnTestThread(
                new FutureFallback<>(invocation, "test invocation", this::fallback, null));
        Future<String> future = result.await();
        assertThat(future.get()).isEqualTo("invocation");
    }

    @Test
    public void immediatelyReturning_exceptionThenException() {
        TestInvocation<Void> invocation = TestInvocation.immediatelyReturning(TestException::doThrow);
        TestThread<Void> result = runOnTestThread(new SyncFallback<>(invocation, "test invocation", e -> {
            throw new RuntimeException();
        }, null));
        assertThatThrownBy(result::await).isExactlyInstanceOf(RuntimeException.class);
    }

    private Future<String> fallback(Throwable e) {
        return CompletableFuture.completedFuture("fallback");
    }
}
