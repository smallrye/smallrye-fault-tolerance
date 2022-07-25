package io.smallrye.faulttolerance.core.rate.limit;

import static io.smallrye.faulttolerance.core.util.CompletionStages.completedStage;
import static io.smallrye.faulttolerance.core.util.TestThread.runOnTestThread;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.api.RateLimitException;
import io.smallrye.faulttolerance.api.RateLimitType;
import io.smallrye.faulttolerance.core.InvocationContext;
import io.smallrye.faulttolerance.core.async.CompletionStageExecution;
import io.smallrye.faulttolerance.core.stopwatch.TestStopwatch;
import io.smallrye.faulttolerance.core.util.TestInvocation;
import io.smallrye.faulttolerance.core.util.TestThread;

public class CompletionStageRateLimitTest {
    private TestStopwatch stopwatch;
    private ExecutorService executor;

    @BeforeEach
    public void setUp() {
        stopwatch = new TestStopwatch();
        executor = new ThreadPoolExecutor(4, 4, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        executor.shutdownNow();
        executor.awaitTermination(1, TimeUnit.SECONDS);
    }

    @Test
    public void fixed_singleThreaded() throws Exception {
        AtomicInteger counter = new AtomicInteger();
        TestInvocation<CompletionStage<String>> invocation = TestInvocation
                .of(() -> completedStage("" + counter.incrementAndGet()));
        CompletionStageExecution<String> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageRateLimit<String> rateLimit = new CompletionStageRateLimit<>(execution, "test invocation",
                2, 100, 0, RateLimitType.FIXED, stopwatch);

        CompletionStage<String> result = rateLimit.apply(new InvocationContext<>(null));
        assertThat(result.toCompletableFuture().get()).isEqualTo("1");
        result = rateLimit.apply(new InvocationContext<>(null));
        assertThat(result.toCompletableFuture().get()).isEqualTo("2");
        result = rateLimit.apply(new InvocationContext<>(null));
        assertThatThrownBy(result.toCompletableFuture()::get)
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(RateLimitException.class);

        stopwatch.setCurrentValue(50);

        result = rateLimit.apply(new InvocationContext<>(null));
        assertThatThrownBy(result.toCompletableFuture()::get)
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(RateLimitException.class);

        stopwatch.setCurrentValue(100);

        result = rateLimit.apply(new InvocationContext<>(null));
        assertThat(result.toCompletableFuture().get()).isEqualTo("3");
        result = rateLimit.apply(new InvocationContext<>(null));
        assertThat(result.toCompletableFuture().get()).isEqualTo("4");
        result = rateLimit.apply(new InvocationContext<>(null));
        assertThatThrownBy(result.toCompletableFuture()::get)
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(RateLimitException.class);
    }

    @Test
    public void fixed_multiThreaded() throws Exception {
        AtomicInteger counter = new AtomicInteger();
        TestInvocation<CompletionStage<String>> invocation = TestInvocation
                .of(() -> completedStage("" + counter.incrementAndGet()));
        CompletionStageExecution<String> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageRateLimit<String> rateLimit = new CompletionStageRateLimit<>(execution, "test invocation",
                2, 100, 0, RateLimitType.FIXED, stopwatch);

        List<TestThread<CompletionStage<String>>> threads = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            threads.add(runOnTestThread(rateLimit));
        }

        Set<String> results = new HashSet<>();
        Set<Exception> exceptions = new HashSet<>();
        for (TestThread<CompletionStage<String>> thread : threads) {
            try {
                results.add(thread.await().toCompletableFuture().get());
            } catch (Exception e) {
                exceptions.add(e);
            }
        }

        assertThat(results).containsExactlyInAnyOrder("1", "2");
        assertThat(exceptions).hasSize(2);
        assertThat(exceptions).allSatisfy(e -> {
            assertThat(e)
                    .isExactlyInstanceOf(ExecutionException.class)
                    .hasCauseExactlyInstanceOf(RateLimitException.class);
        });

        stopwatch.setCurrentValue(50);

        CompletionStage<String> result = rateLimit.apply(new InvocationContext<>(null));
        assertThatThrownBy(result.toCompletableFuture()::get)
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(RateLimitException.class);

        stopwatch.setCurrentValue(100);
        threads.clear();
        results.clear();
        exceptions.clear();

        for (int i = 0; i < 2; i++) {
            threads.add(runOnTestThread(rateLimit));
        }

        for (TestThread<CompletionStage<String>> thread : threads) {
            try {
                results.add(thread.await().toCompletableFuture().get());
            } catch (Exception e) {
                exceptions.add(e);
            }
        }

        assertThat(results).containsExactlyInAnyOrder("3", "4");
        assertThat(exceptions).isEmpty();
    }

    @Test
    public void fixed_multiThreaded_withMinSpacing() throws Exception {
        AtomicInteger counter = new AtomicInteger();
        TestInvocation<CompletionStage<String>> invocation = TestInvocation
                .of(() -> completedStage("" + counter.incrementAndGet()));
        CompletionStageExecution<String> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageRateLimit<String> rateLimit = new CompletionStageRateLimit<>(execution, "test invocation",
                2, 100, 10, RateLimitType.FIXED, stopwatch);

        List<TestThread<CompletionStage<String>>> threads = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            threads.add(runOnTestThread(rateLimit));
        }

        Set<String> results = new HashSet<>();
        Set<Exception> exceptions = new HashSet<>();
        for (TestThread<CompletionStage<String>> thread : threads) {
            try {
                results.add(thread.await().toCompletableFuture().get());
            } catch (Exception e) {
                exceptions.add(e);
            }
        }

        assertThat(results).containsExactlyInAnyOrder("1");
        assertThat(exceptions).hasSize(3);
        assertThat(exceptions).allSatisfy(e -> {
            assertThat(e)
                    .isExactlyInstanceOf(ExecutionException.class)
                    .hasCauseExactlyInstanceOf(RateLimitException.class);
        });

        stopwatch.setCurrentValue(50);

        CompletionStage<String> result = rateLimit.apply(new InvocationContext<>(null));
        assertThatThrownBy(result.toCompletableFuture()::get)
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(RateLimitException.class);

        stopwatch.setCurrentValue(100);
        threads.clear();
        results.clear();
        exceptions.clear();

        for (int i = 0; i < 2; i++) {
            threads.add(runOnTestThread(rateLimit));
        }

        for (TestThread<CompletionStage<String>> thread : threads) {
            try {
                results.add(thread.await().toCompletableFuture().get());
            } catch (Exception e) {
                exceptions.add(e);
            }
        }

        assertThat(results).containsExactlyInAnyOrder("2");
        assertThat(exceptions).hasSize(1);
        assertThat(exceptions).allSatisfy(e -> {
            assertThat(e)
                    .isExactlyInstanceOf(ExecutionException.class)
                    .hasCauseExactlyInstanceOf(RateLimitException.class);
        });
    }

    @Test
    public void rolling_singleThreaded() throws Exception {
        AtomicInteger counter = new AtomicInteger();
        TestInvocation<CompletionStage<String>> invocation = TestInvocation
                .of(() -> completedStage("" + counter.incrementAndGet()));
        CompletionStageExecution<String> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageRateLimit<String> rateLimit = new CompletionStageRateLimit<>(execution, "test invocation",
                2, 100, 0, RateLimitType.ROLLING, stopwatch);

        CompletionStage<String> result = rateLimit.apply(new InvocationContext<>(null));
        assertThat(result.toCompletableFuture().get()).isEqualTo("1");
        result = rateLimit.apply(new InvocationContext<>(null));
        assertThat(result.toCompletableFuture().get()).isEqualTo("2");
        result = rateLimit.apply(new InvocationContext<>(null));
        assertThatThrownBy(result.toCompletableFuture()::get)
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(RateLimitException.class);

        stopwatch.setCurrentValue(50);

        result = rateLimit.apply(new InvocationContext<>(null));
        assertThatThrownBy(result.toCompletableFuture()::get)
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(RateLimitException.class);

        stopwatch.setCurrentValue(100);

        result = rateLimit.apply(new InvocationContext<>(null));
        assertThat(result.toCompletableFuture().get()).isEqualTo("3");
        result = rateLimit.apply(new InvocationContext<>(null));
        assertThatThrownBy(result.toCompletableFuture()::get)
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(RateLimitException.class);
    }

    @Test
    public void rolling_multiThreaded() throws Exception {
        AtomicInteger counter = new AtomicInteger();
        TestInvocation<CompletionStage<String>> invocation = TestInvocation
                .of(() -> completedStage("" + counter.incrementAndGet()));
        CompletionStageExecution<String> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageRateLimit<String> rateLimit = new CompletionStageRateLimit<>(execution, "test invocation",
                2, 100, 0, RateLimitType.ROLLING, stopwatch);

        List<TestThread<CompletionStage<String>>> threads = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            threads.add(runOnTestThread(rateLimit));
        }

        Set<String> results = new HashSet<>();
        Set<Exception> exceptions = new HashSet<>();
        for (TestThread<CompletionStage<String>> thread : threads) {
            try {
                results.add(thread.await().toCompletableFuture().get());
            } catch (Exception e) {
                exceptions.add(e);
            }
        }

        assertThat(results).containsExactlyInAnyOrder("1", "2");
        assertThat(exceptions).hasSize(2);
        assertThat(exceptions).allSatisfy(e -> {
            assertThat(e)
                    .isExactlyInstanceOf(ExecutionException.class)
                    .hasCauseExactlyInstanceOf(RateLimitException.class);
        });

        stopwatch.setCurrentValue(50);

        CompletionStage<String> result = rateLimit.apply(new InvocationContext<>(null));
        assertThatThrownBy(result.toCompletableFuture()::get)
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(RateLimitException.class);

        stopwatch.setCurrentValue(100);
        threads.clear();
        results.clear();
        exceptions.clear();

        for (int i = 0; i < 2; i++) {
            threads.add(runOnTestThread(rateLimit));
        }

        for (TestThread<CompletionStage<String>> thread : threads) {
            try {
                results.add(thread.await().toCompletableFuture().get());
            } catch (Exception e) {
                exceptions.add(e);
            }
        }

        assertThat(results).containsExactlyInAnyOrder("3");
        assertThat(exceptions).hasSize(1);
        assertThat(exceptions).allSatisfy(e -> {
            assertThat(e)
                    .isExactlyInstanceOf(ExecutionException.class)
                    .hasCauseExactlyInstanceOf(RateLimitException.class);
        });
    }

    @Test
    public void rolling_multiThreaded_withMinSpacing() throws Exception {
        AtomicInteger counter = new AtomicInteger();
        TestInvocation<CompletionStage<String>> invocation = TestInvocation
                .of(() -> completedStage("" + counter.incrementAndGet()));
        CompletionStageExecution<String> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageRateLimit<String> rateLimit = new CompletionStageRateLimit<>(execution, "test invocation",
                2, 100, 10, RateLimitType.ROLLING, stopwatch);

        List<TestThread<CompletionStage<String>>> threads = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            threads.add(runOnTestThread(rateLimit));
        }

        Set<String> results = new HashSet<>();
        Set<Exception> exceptions = new HashSet<>();
        for (TestThread<CompletionStage<String>> thread : threads) {
            try {
                results.add(thread.await().toCompletableFuture().get());
            } catch (Exception e) {
                exceptions.add(e);
            }
        }

        assertThat(results).containsExactlyInAnyOrder("1");
        assertThat(exceptions).hasSize(3);
        assertThat(exceptions).allSatisfy(e -> {
            assertThat(e)
                    .isExactlyInstanceOf(ExecutionException.class)
                    .hasCauseExactlyInstanceOf(RateLimitException.class);
        });

        stopwatch.setCurrentValue(50);

        CompletionStage<String> result = rateLimit.apply(new InvocationContext<>(null));
        assertThatThrownBy(result.toCompletableFuture()::get)
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(RateLimitException.class);

        stopwatch.setCurrentValue(150);
        threads.clear();
        results.clear();
        exceptions.clear();

        for (int i = 0; i < 2; i++) {
            threads.add(runOnTestThread(rateLimit));
        }

        for (TestThread<CompletionStage<String>> thread : threads) {
            try {
                results.add(thread.await().toCompletableFuture().get());
            } catch (Exception e) {
                exceptions.add(e);
            }
        }

        assertThat(results).containsExactlyInAnyOrder("2");
        assertThat(exceptions).hasSize(1);
        assertThat(exceptions).allSatisfy(e -> {
            assertThat(e)
                    .isExactlyInstanceOf(ExecutionException.class)
                    .hasCauseExactlyInstanceOf(RateLimitException.class);
        });
    }
}
