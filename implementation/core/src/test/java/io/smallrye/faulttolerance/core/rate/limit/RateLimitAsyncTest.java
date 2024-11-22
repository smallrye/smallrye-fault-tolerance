package io.smallrye.faulttolerance.core.rate.limit;

import static io.smallrye.faulttolerance.core.FaultToleranceContextUtil.async;
import static io.smallrye.faulttolerance.core.util.TestThread.runOnTestThread;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.api.RateLimitException;
import io.smallrye.faulttolerance.api.RateLimitType;
import io.smallrye.faulttolerance.core.Future;
import io.smallrye.faulttolerance.core.async.ThreadOffload;
import io.smallrye.faulttolerance.core.stopwatch.TestStopwatch;
import io.smallrye.faulttolerance.core.util.TestInvocation;
import io.smallrye.faulttolerance.core.util.TestThread;

public class RateLimitAsyncTest {
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
    public void fixed_singleThreaded() throws Throwable {
        AtomicInteger counter = new AtomicInteger();
        TestInvocation<String> invocation = TestInvocation.of(() -> "" + counter.incrementAndGet());
        ThreadOffload<String> execution = new ThreadOffload<>(invocation, executor, true);
        RateLimit<String> rateLimit = new RateLimit<>(execution, "test invocation",
                2, 100, 0, RateLimitType.FIXED, stopwatch);

        Future<String> result = rateLimit.apply(async(null));
        assertThat(result.awaitBlocking()).isEqualTo("1");
        result = rateLimit.apply(async(null));
        assertThat(result.awaitBlocking()).isEqualTo("2");
        result = rateLimit.apply(async(null));
        assertThatThrownBy(result::awaitBlocking)
                .isInstanceOf(RateLimitException.class)
                .asInstanceOf(InstanceOfAssertFactories.throwable(RateLimitException.class))
                .extracting(RateLimitException::getRetryAfterMillis)
                .isEqualTo(100L);

        stopwatch.setCurrentValue(50);

        result = rateLimit.apply(async(null));
        assertThatThrownBy(result::awaitBlocking)
                .isInstanceOf(RateLimitException.class)
                .asInstanceOf(InstanceOfAssertFactories.throwable(RateLimitException.class))
                .extracting(RateLimitException::getRetryAfterMillis)
                .isEqualTo(50L);

        stopwatch.setCurrentValue(100);

        result = rateLimit.apply(async(null));
        assertThat(result.awaitBlocking()).isEqualTo("3");
        result = rateLimit.apply(async(null));
        assertThat(result.awaitBlocking()).isEqualTo("4");
        result = rateLimit.apply(async(null));
        assertThatThrownBy(result::awaitBlocking)
                .isInstanceOf(RateLimitException.class)
                .asInstanceOf(InstanceOfAssertFactories.throwable(RateLimitException.class))
                .extracting(RateLimitException::getRetryAfterMillis)
                .isEqualTo(100L);
    }

    @Test
    public void fixed_multiThreaded() {
        AtomicInteger counter = new AtomicInteger();
        TestInvocation<String> invocation = TestInvocation.of(() -> "" + counter.incrementAndGet());
        ThreadOffload<String> execution = new ThreadOffload<>(invocation, executor, true);
        RateLimit<String> rateLimit = new RateLimit<>(execution, "test invocation",
                2, 100, 0, RateLimitType.FIXED, stopwatch);

        List<TestThread<String>> threads = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            threads.add(runOnTestThread(rateLimit, true));
        }

        Set<String> results = new HashSet<>();
        Set<Exception> exceptions = new HashSet<>();
        for (TestThread<String> thread : threads) {
            try {
                results.add(thread.await());
            } catch (Exception e) {
                exceptions.add(e);
            }
        }

        assertThat(results).containsExactlyInAnyOrder("1", "2");
        assertThat(exceptions).hasSize(2);
        assertThat(exceptions).allSatisfy(e -> {
            assertThat(e)
                    .isExactlyInstanceOf(RateLimitException.class)
                    .asInstanceOf(InstanceOfAssertFactories.throwable(RateLimitException.class))
                    .extracting(RateLimitException::getRetryAfterMillis)
                    .isEqualTo(100L);
        });

        stopwatch.setCurrentValue(50);

        Future<String> result = rateLimit.apply(async(null));
        assertThatThrownBy(result::awaitBlocking)
                .isInstanceOf(RateLimitException.class)
                .asInstanceOf(InstanceOfAssertFactories.throwable(RateLimitException.class))
                .extracting(RateLimitException::getRetryAfterMillis)
                .isEqualTo(50L);

        stopwatch.setCurrentValue(100);
        threads.clear();
        results.clear();
        exceptions.clear();

        for (int i = 0; i < 2; i++) {
            threads.add(runOnTestThread(rateLimit, true));
        }

        for (TestThread<String> thread : threads) {
            try {
                results.add(thread.await());
            } catch (Exception e) {
                exceptions.add(e);
            }
        }

        assertThat(results).containsExactlyInAnyOrder("3", "4");
        assertThat(exceptions).isEmpty();
    }

    @Test
    public void fixed_multiThreaded_withMinSpacing() {
        AtomicInteger counter = new AtomicInteger();
        TestInvocation<String> invocation = TestInvocation.of(() -> "" + counter.incrementAndGet());
        ThreadOffload<String> execution = new ThreadOffload<>(invocation, executor, true);
        RateLimit<String> rateLimit = new RateLimit<>(execution, "test invocation",
                2, 100, 10, RateLimitType.FIXED, stopwatch);

        List<TestThread<String>> threads = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            threads.add(runOnTestThread(rateLimit, true));
        }

        Set<String> results = new HashSet<>();
        Set<Exception> exceptions = new HashSet<>();
        for (TestThread<String> thread : threads) {
            try {
                results.add(thread.await());
            } catch (Exception e) {
                exceptions.add(e);
            }
        }

        assertThat(results).containsExactlyInAnyOrder("1");
        assertThat(exceptions).hasSize(3);
        assertThat(exceptions).allSatisfy(e -> {
            assertThat(e).isExactlyInstanceOf(RateLimitException.class);
        });
        // 1 x 10: min spacing violated (but rate limit not exceeded yet)
        // 2 x 100: rate limit exceeded
        assertThat(exceptions.stream().mapToLong(it -> ((RateLimitException) it).getRetryAfterMillis()).sum())
                .isEqualTo(210);

        stopwatch.setCurrentValue(50);

        Future<String> result = rateLimit.apply(async(null));
        assertThatThrownBy(result::awaitBlocking)
                .isInstanceOf(RateLimitException.class)
                .asInstanceOf(InstanceOfAssertFactories.throwable(RateLimitException.class))
                .extracting(RateLimitException::getRetryAfterMillis)
                .isEqualTo(50L);

        stopwatch.setCurrentValue(100);
        threads.clear();
        results.clear();
        exceptions.clear();

        for (int i = 0; i < 2; i++) {
            threads.add(runOnTestThread(rateLimit, true));
        }

        for (TestThread<String> thread : threads) {
            try {
                results.add(thread.await());
            } catch (Exception e) {
                exceptions.add(e);
            }
        }

        assertThat(results).containsExactlyInAnyOrder("2");
        assertThat(exceptions).hasSize(1);
        assertThat(exceptions).allSatisfy(e -> {
            assertThat(e)
                    .isExactlyInstanceOf(RateLimitException.class)
                    .asInstanceOf(InstanceOfAssertFactories.throwable(RateLimitException.class))
                    .extracting(RateLimitException::getRetryAfterMillis)
                    .isEqualTo(10L);
        });
    }

    @Test
    public void rolling_singleThreaded() throws Throwable {
        AtomicInteger counter = new AtomicInteger();
        TestInvocation<String> invocation = TestInvocation.of(() -> "" + counter.incrementAndGet());
        ThreadOffload<String> execution = new ThreadOffload<>(invocation, executor, true);
        RateLimit<String> rateLimit = new RateLimit<>(execution, "test invocation",
                2, 100, 0, RateLimitType.ROLLING, stopwatch);

        Future<String> result = rateLimit.apply(async(null));
        assertThat(result.awaitBlocking()).isEqualTo("1");
        result = rateLimit.apply(async(null));
        assertThat(result.awaitBlocking()).isEqualTo("2");
        result = rateLimit.apply(async(null));
        assertThatThrownBy(result::awaitBlocking)
                .isInstanceOf(RateLimitException.class)
                .asInstanceOf(InstanceOfAssertFactories.throwable(RateLimitException.class))
                .extracting(RateLimitException::getRetryAfterMillis)
                .isEqualTo(100L);

        stopwatch.setCurrentValue(50);

        result = rateLimit.apply(async(null));
        assertThatThrownBy(result::awaitBlocking)
                .isInstanceOf(RateLimitException.class)
                .asInstanceOf(InstanceOfAssertFactories.throwable(RateLimitException.class))
                .extracting(RateLimitException::getRetryAfterMillis)
                .isEqualTo(50L);

        stopwatch.setCurrentValue(100);

        result = rateLimit.apply(async(null));
        assertThat(result.awaitBlocking()).isEqualTo("3");
        result = rateLimit.apply(async(null));
        assertThatThrownBy(result::awaitBlocking)
                .isInstanceOf(RateLimitException.class)
                .asInstanceOf(InstanceOfAssertFactories.throwable(RateLimitException.class))
                .extracting(RateLimitException::getRetryAfterMillis)
                .isEqualTo(50L);
    }

    @Test
    public void rolling_multiThreaded() {
        AtomicInteger counter = new AtomicInteger();
        TestInvocation<String> invocation = TestInvocation.of(() -> "" + counter.incrementAndGet());
        ThreadOffload<String> execution = new ThreadOffload<>(invocation, executor, true);
        RateLimit<String> rateLimit = new RateLimit<>(execution, "test invocation",
                2, 100, 0, RateLimitType.ROLLING, stopwatch);

        List<TestThread<String>> threads = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            threads.add(runOnTestThread(rateLimit, true));
        }

        Set<String> results = new HashSet<>();
        Set<Exception> exceptions = new HashSet<>();
        for (TestThread<String> thread : threads) {
            try {
                results.add(thread.await());
            } catch (Exception e) {
                exceptions.add(e);
            }
        }

        assertThat(results).containsExactlyInAnyOrder("1", "2");
        assertThat(exceptions).hasSize(2);
        assertThat(exceptions).allSatisfy(e -> {
            assertThat(e)
                    .isExactlyInstanceOf(RateLimitException.class)
                    .asInstanceOf(InstanceOfAssertFactories.throwable(RateLimitException.class))
                    .extracting(RateLimitException::getRetryAfterMillis)
                    .isEqualTo(100L);
        });

        stopwatch.setCurrentValue(50);

        Future<String> result = rateLimit.apply(async(null));
        assertThatThrownBy(result::awaitBlocking)
                .isInstanceOf(RateLimitException.class)
                .asInstanceOf(InstanceOfAssertFactories.throwable(RateLimitException.class))
                .extracting(RateLimitException::getRetryAfterMillis)
                .isEqualTo(50L);

        stopwatch.setCurrentValue(100);
        threads.clear();
        results.clear();
        exceptions.clear();

        for (int i = 0; i < 2; i++) {
            threads.add(runOnTestThread(rateLimit, true));
        }

        for (TestThread<String> thread : threads) {
            try {
                results.add(thread.await());
            } catch (Exception e) {
                exceptions.add(e);
            }
        }

        assertThat(results).containsExactlyInAnyOrder("3");
        assertThat(exceptions).hasSize(1);
        assertThat(exceptions).allSatisfy(e -> {
            assertThat(e)
                    .isExactlyInstanceOf(RateLimitException.class)
                    .asInstanceOf(InstanceOfAssertFactories.throwable(RateLimitException.class))
                    .extracting(RateLimitException::getRetryAfterMillis)
                    .isEqualTo(50L);
        });
    }

    @Test
    public void rolling_multiThreaded_withMinSpacing() {
        AtomicInteger counter = new AtomicInteger();
        TestInvocation<String> invocation = TestInvocation.of(() -> "" + counter.incrementAndGet());
        ThreadOffload<String> execution = new ThreadOffload<>(invocation, executor, true);
        RateLimit<String> rateLimit = new RateLimit<>(execution, "test invocation",
                2, 100, 10, RateLimitType.ROLLING, stopwatch);

        List<TestThread<String>> threads = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            threads.add(runOnTestThread(rateLimit, true));
        }

        Set<String> results = new HashSet<>();
        Set<Exception> exceptions = new HashSet<>();
        for (TestThread<String> thread : threads) {
            try {
                results.add(thread.await());
            } catch (Exception e) {
                exceptions.add(e);
            }
        }

        assertThat(results).containsExactlyInAnyOrder("1");
        assertThat(exceptions).hasSize(3);
        assertThat(exceptions).allSatisfy(e -> {
            assertThat(e).isExactlyInstanceOf(RateLimitException.class);
        });
        // 1 x 10: min spacing violated (but rate limit not exceeded yet)
        // 2 x 100: rate limit exceeded
        assertThat(exceptions.stream().mapToLong(it -> ((RateLimitException) it).getRetryAfterMillis()).sum())
                .isEqualTo(210);

        stopwatch.setCurrentValue(50);

        Future<String> result = rateLimit.apply(async(null));
        assertThatThrownBy(result::awaitBlocking)
                .isInstanceOf(RateLimitException.class)
                .asInstanceOf(InstanceOfAssertFactories.throwable(RateLimitException.class))
                .extracting(RateLimitException::getRetryAfterMillis)
                .isEqualTo(50L);

        stopwatch.setCurrentValue(150);
        threads.clear();
        results.clear();
        exceptions.clear();

        for (int i = 0; i < 2; i++) {
            threads.add(runOnTestThread(rateLimit, true));
        }

        for (TestThread<String> thread : threads) {
            try {
                results.add(thread.await());
            } catch (Exception e) {
                exceptions.add(e);
            }
        }

        assertThat(results).containsExactlyInAnyOrder("2");
        assertThat(exceptions).hasSize(1);
        assertThat(exceptions).allSatisfy(e -> {
            assertThat(e)
                    .isExactlyInstanceOf(RateLimitException.class)
                    .asInstanceOf(InstanceOfAssertFactories.throwable(RateLimitException.class))
                    .extracting(RateLimitException::getRetryAfterMillis)
                    .isEqualTo(10L);
        });
    }

    @Test
    public void smooth_singleThreaded() throws Throwable {
        AtomicInteger counter = new AtomicInteger();
        TestInvocation<String> invocation = TestInvocation.of(() -> "" + counter.incrementAndGet());
        ThreadOffload<String> execution = new ThreadOffload<>(invocation, executor, true);
        RateLimit<String> rateLimit = new RateLimit<>(execution, "test invocation",
                2, 100, 0, RateLimitType.SMOOTH, stopwatch);

        Future<String> result = rateLimit.apply(async(null));
        assertThat(result.awaitBlocking()).isEqualTo("1");
        result = rateLimit.apply(async(null));
        assertThatThrownBy(result::awaitBlocking)
                .isInstanceOf(RateLimitException.class)
                .asInstanceOf(InstanceOfAssertFactories.throwable(RateLimitException.class))
                .extracting(RateLimitException::getRetryAfterMillis)
                .isEqualTo(50L);

        stopwatch.setCurrentValue(50);

        result = rateLimit.apply(async(null));
        assertThat(result.awaitBlocking()).isEqualTo("2");

        stopwatch.setCurrentValue(100);

        result = rateLimit.apply(async(null));
        assertThat(result.awaitBlocking()).isEqualTo("3");

        stopwatch.setCurrentValue(150);

        result = rateLimit.apply(async(null));
        assertThat(result.awaitBlocking()).isEqualTo("4");
        result = rateLimit.apply(async(null));
        assertThatThrownBy(result::awaitBlocking)
                .isInstanceOf(RateLimitException.class)
                .asInstanceOf(InstanceOfAssertFactories.throwable(RateLimitException.class))
                .extracting(RateLimitException::getRetryAfterMillis)
                .isEqualTo(50L);
    }

    @Test
    public void smooth_multiThreaded() throws Throwable {
        AtomicInteger counter = new AtomicInteger();
        TestInvocation<String> invocation = TestInvocation.of(() -> "" + counter.incrementAndGet());
        ThreadOffload<String> execution = new ThreadOffload<>(invocation, executor, true);
        RateLimit<String> rateLimit = new RateLimit<>(execution, "test invocation",
                2, 100, 0, RateLimitType.SMOOTH, stopwatch);

        List<TestThread<String>> threads = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            threads.add(runOnTestThread(rateLimit, true));
        }

        Set<String> results = new HashSet<>();
        Set<Exception> exceptions = new HashSet<>();
        for (TestThread<String> thread : threads) {
            try {
                results.add(thread.await());
            } catch (Exception e) {
                exceptions.add(e);
            }
        }

        assertThat(results).containsExactlyInAnyOrder("1");
        assertThat(exceptions).hasSize(3);
        assertThat(exceptions).allSatisfy(e -> {
            assertThat(e)
                    .isExactlyInstanceOf(RateLimitException.class)
                    .asInstanceOf(InstanceOfAssertFactories.throwable(RateLimitException.class))
                    .extracting(RateLimitException::getRetryAfterMillis)
                    .isEqualTo(50L);
        });

        stopwatch.setCurrentValue(50);

        Future<String> result = rateLimit.apply(async(null));
        assertThat(result.awaitBlocking()).isEqualTo("2");

        stopwatch.setCurrentValue(100);
        threads.clear();
        results.clear();
        exceptions.clear();

        for (int i = 0; i < 2; i++) {
            threads.add(runOnTestThread(rateLimit, true));
        }

        for (TestThread<String> thread : threads) {
            try {
                results.add(thread.await());
            } catch (Exception e) {
                exceptions.add(e);
            }
        }

        assertThat(results).containsExactlyInAnyOrder("3");
        assertThat(exceptions).hasSize(1);
        assertThat(exceptions).allSatisfy(e -> {
            assertThat(e)
                    .isExactlyInstanceOf(RateLimitException.class)
                    .asInstanceOf(InstanceOfAssertFactories.throwable(RateLimitException.class))
                    .extracting(RateLimitException::getRetryAfterMillis)
                    .isEqualTo(50L);
        });
    }

    @Test
    public void smooth_multiThreaded_withMinSpacing() throws Throwable {
        AtomicInteger counter = new AtomicInteger();
        TestInvocation<String> invocation = TestInvocation.of(() -> "" + counter.incrementAndGet());
        ThreadOffload<String> execution = new ThreadOffload<>(invocation, executor, true);
        RateLimit<String> rateLimit = new RateLimit<>(execution, "test invocation",
                1000, 100, 10, RateLimitType.SMOOTH, stopwatch);

        List<TestThread<String>> threads = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            threads.add(runOnTestThread(rateLimit, true));
        }

        Set<String> results = new HashSet<>();
        Set<Exception> exceptions = new HashSet<>();
        for (TestThread<String> thread : threads) {
            try {
                results.add(thread.await());
            } catch (Exception e) {
                exceptions.add(e);
            }
        }

        assertThat(results).containsExactlyInAnyOrder("1");
        assertThat(exceptions).hasSize(3);
        assertThat(exceptions).allSatisfy(e -> {
            assertThat(e)
                    .isExactlyInstanceOf(RateLimitException.class)
                    .asInstanceOf(InstanceOfAssertFactories.throwable(RateLimitException.class))
                    .extracting(RateLimitException::getRetryAfterMillis)
                    .isEqualTo(10L);
        });

        stopwatch.setCurrentValue(50);

        Future<String> result = rateLimit.apply(async(null));
        assertThat(result.awaitBlocking()).isEqualTo("2");

        stopwatch.setCurrentValue(100);
        threads.clear();
        results.clear();
        exceptions.clear();

        for (int i = 0; i < 2; i++) {
            threads.add(runOnTestThread(rateLimit, true));
        }

        for (TestThread<String> thread : threads) {
            try {
                results.add(thread.await());
            } catch (Exception e) {
                exceptions.add(e);
            }
        }

        assertThat(results).containsExactlyInAnyOrder("3");
        assertThat(exceptions).hasSize(1);
        assertThat(exceptions).allSatisfy(e -> {
            assertThat(e)
                    .isExactlyInstanceOf(RateLimitException.class)
                    .asInstanceOf(InstanceOfAssertFactories.throwable(RateLimitException.class))
                    .extracting(RateLimitException::getRetryAfterMillis)
                    .isEqualTo(10L);
        });
    }
}
