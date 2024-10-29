package io.smallrye.faulttolerance.core.rate.limit;

import static io.smallrye.faulttolerance.core.FaultToleranceContextUtil.sync;
import static io.smallrye.faulttolerance.core.util.TestThread.runOnTestThread;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.api.RateLimitException;
import io.smallrye.faulttolerance.api.RateLimitType;
import io.smallrye.faulttolerance.core.stopwatch.TestStopwatch;
import io.smallrye.faulttolerance.core.util.TestInvocation;
import io.smallrye.faulttolerance.core.util.TestThread;

public class RateLimitSyncTest {
    private TestStopwatch stopwatch;

    @BeforeEach
    public void setUp() {
        stopwatch = new TestStopwatch();
    }

    @Test
    public void fixed_singleThreaded() throws Throwable {
        AtomicInteger counter = new AtomicInteger();
        TestInvocation<String> invocation = TestInvocation.of(() -> "" + counter.incrementAndGet());
        RateLimit<String> rateLimit = new RateLimit<>(invocation, "test invocation", 2, 100, 0,
                RateLimitType.FIXED, stopwatch);

        assertThat(rateLimit.apply(sync(null)).awaitBlocking()).isEqualTo("1");
        assertThat(rateLimit.apply(sync(null)).awaitBlocking()).isEqualTo("2");
        assertThatThrownBy(rateLimit.apply(sync(null))::awaitBlocking)
                .isExactlyInstanceOf(RateLimitException.class)
                .asInstanceOf(InstanceOfAssertFactories.throwable(RateLimitException.class))
                .extracting(RateLimitException::getRetryAfterMillis)
                .isEqualTo(100L);

        stopwatch.setCurrentValue(50);

        assertThatThrownBy(rateLimit.apply(sync(null))::awaitBlocking)
                .isExactlyInstanceOf(RateLimitException.class)
                .asInstanceOf(InstanceOfAssertFactories.throwable(RateLimitException.class))
                .extracting(RateLimitException::getRetryAfterMillis)
                .isEqualTo(50L);

        stopwatch.setCurrentValue(100);

        assertThat(rateLimit.apply(sync(null)).awaitBlocking()).isEqualTo("3");
        assertThat(rateLimit.apply(sync(null)).awaitBlocking()).isEqualTo("4");
        assertThatThrownBy(rateLimit.apply(sync(null))::awaitBlocking)
                .isExactlyInstanceOf(RateLimitException.class)
                .asInstanceOf(InstanceOfAssertFactories.throwable(RateLimitException.class))
                .extracting(RateLimitException::getRetryAfterMillis)
                .isEqualTo(100L);
    }

    @Test
    public void fixed_multiThreaded() {
        AtomicInteger counter = new AtomicInteger();
        TestInvocation<String> invocation = TestInvocation.of(() -> "" + counter.incrementAndGet());
        RateLimit<String> rateLimit = new RateLimit<>(invocation, "test invocation", 2, 100, 0,
                RateLimitType.FIXED, stopwatch);

        List<TestThread<String>> threads = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            threads.add(runOnTestThread(rateLimit, false));
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
            assertThat(e).isExactlyInstanceOf(RateLimitException.class)
                    .asInstanceOf(InstanceOfAssertFactories.throwable(RateLimitException.class))
                    .extracting(RateLimitException::getRetryAfterMillis)
                    .isEqualTo(100L);
        });

        stopwatch.setCurrentValue(50);

        assertThatThrownBy(rateLimit.apply(sync(null))::awaitBlocking)
                .isExactlyInstanceOf(RateLimitException.class)
                .asInstanceOf(InstanceOfAssertFactories.throwable(RateLimitException.class))
                .extracting(RateLimitException::getRetryAfterMillis)
                .isEqualTo(50L);

        stopwatch.setCurrentValue(100);
        threads.clear();
        results.clear();
        exceptions.clear();

        for (int i = 0; i < 2; i++) {
            threads.add(runOnTestThread(rateLimit, false));
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
        RateLimit<String> rateLimit = new RateLimit<>(invocation, "test invocation", 2, 100, 10,
                RateLimitType.FIXED, stopwatch);

        List<TestThread<String>> threads = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            threads.add(runOnTestThread(rateLimit, false));
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

        assertThatThrownBy(rateLimit.apply(sync(null))::awaitBlocking)
                .isExactlyInstanceOf(RateLimitException.class)
                .asInstanceOf(InstanceOfAssertFactories.throwable(RateLimitException.class))
                .extracting(RateLimitException::getRetryAfterMillis)
                .isEqualTo(50L);

        stopwatch.setCurrentValue(100);
        threads.clear();
        results.clear();
        exceptions.clear();

        for (int i = 0; i < 2; i++) {
            threads.add(runOnTestThread(rateLimit, false));
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
            assertThat(e).isExactlyInstanceOf(RateLimitException.class)
                    .asInstanceOf(InstanceOfAssertFactories.throwable(RateLimitException.class))
                    .extracting(RateLimitException::getRetryAfterMillis)
                    .isEqualTo(10L);
        });
    }

    @Test
    public void rolling_singleThreaded() throws Throwable {
        AtomicInteger counter = new AtomicInteger();
        TestInvocation<String> invocation = TestInvocation.of(() -> "" + counter.incrementAndGet());
        RateLimit<String> rateLimit = new RateLimit<>(invocation, "test invocation", 2, 100, 0,
                RateLimitType.ROLLING, stopwatch);

        assertThat(rateLimit.apply(sync(null)).awaitBlocking()).isEqualTo("1");
        assertThat(rateLimit.apply(sync(null)).awaitBlocking()).isEqualTo("2");
        assertThatThrownBy(rateLimit.apply(sync(null))::awaitBlocking)
                .isExactlyInstanceOf(RateLimitException.class)
                .asInstanceOf(InstanceOfAssertFactories.throwable(RateLimitException.class))
                .extracting(RateLimitException::getRetryAfterMillis)
                .isEqualTo(100L);

        stopwatch.setCurrentValue(50);

        assertThatThrownBy(rateLimit.apply(sync(null))::awaitBlocking)
                .isExactlyInstanceOf(RateLimitException.class)
                .asInstanceOf(InstanceOfAssertFactories.throwable(RateLimitException.class))
                .extracting(RateLimitException::getRetryAfterMillis)
                .isEqualTo(50L);

        stopwatch.setCurrentValue(100);

        assertThat(rateLimit.apply(sync(null)).awaitBlocking()).isEqualTo("3");
        assertThatThrownBy(rateLimit.apply(sync(null))::awaitBlocking)
                .isExactlyInstanceOf(RateLimitException.class)
                .asInstanceOf(InstanceOfAssertFactories.throwable(RateLimitException.class))
                .extracting(RateLimitException::getRetryAfterMillis)
                .isEqualTo(50L);
    }

    @Test
    public void rolling_multiThreaded() {
        AtomicInteger counter = new AtomicInteger();
        TestInvocation<String> invocation = TestInvocation.of(() -> "" + counter.incrementAndGet());
        RateLimit<String> rateLimit = new RateLimit<>(invocation, "test invocation", 2, 100, 0,
                RateLimitType.ROLLING, stopwatch);

        List<TestThread<String>> threads = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            threads.add(runOnTestThread(rateLimit, false));
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
            assertThat(e).isExactlyInstanceOf(RateLimitException.class)
                    .asInstanceOf(InstanceOfAssertFactories.throwable(RateLimitException.class))
                    .extracting(RateLimitException::getRetryAfterMillis)
                    .isEqualTo(100L);
        });

        stopwatch.setCurrentValue(50);

        assertThatThrownBy(rateLimit.apply(sync(null))::awaitBlocking)
                .isExactlyInstanceOf(RateLimitException.class)
                .asInstanceOf(InstanceOfAssertFactories.throwable(RateLimitException.class))
                .extracting(RateLimitException::getRetryAfterMillis)
                .isEqualTo(50L);

        stopwatch.setCurrentValue(100);
        threads.clear();
        results.clear();
        exceptions.clear();

        for (int i = 0; i < 2; i++) {
            threads.add(runOnTestThread(rateLimit, false));
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
            assertThat(e).isExactlyInstanceOf(RateLimitException.class)
                    .asInstanceOf(InstanceOfAssertFactories.throwable(RateLimitException.class))
                    .extracting(RateLimitException::getRetryAfterMillis)
                    .isEqualTo(50L);
        });
    }

    @Test
    public void rolling_multiThreaded_withMinSpacing() {
        AtomicInteger counter = new AtomicInteger();
        TestInvocation<String> invocation = TestInvocation.of(() -> "" + counter.incrementAndGet());
        RateLimit<String> rateLimit = new RateLimit<>(invocation, "test invocation", 2, 100, 10,
                RateLimitType.ROLLING, stopwatch);

        List<TestThread<String>> threads = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            threads.add(runOnTestThread(rateLimit, false));
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

        assertThatThrownBy(rateLimit.apply(sync(null))::awaitBlocking)
                .isExactlyInstanceOf(RateLimitException.class)
                .asInstanceOf(InstanceOfAssertFactories.throwable(RateLimitException.class))
                .extracting(RateLimitException::getRetryAfterMillis)
                .isEqualTo(50L);

        stopwatch.setCurrentValue(150);
        threads.clear();
        results.clear();
        exceptions.clear();

        for (int i = 0; i < 2; i++) {
            threads.add(runOnTestThread(rateLimit, false));
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
            assertThat(e).isExactlyInstanceOf(RateLimitException.class)
                    .asInstanceOf(InstanceOfAssertFactories.throwable(RateLimitException.class))
                    .extracting(RateLimitException::getRetryAfterMillis)
                    .isEqualTo(10L);
        });
    }

    @Test
    public void smooth_singleThreaded() throws Throwable {
        AtomicInteger counter = new AtomicInteger();
        TestInvocation<String> invocation = TestInvocation.of(() -> "" + counter.incrementAndGet());
        RateLimit<String> rateLimit = new RateLimit<>(invocation, "test invocation", 2, 100, 0,
                RateLimitType.SMOOTH, stopwatch);

        assertThat(rateLimit.apply(sync(null)).awaitBlocking()).isEqualTo("1");
        assertThatThrownBy(rateLimit.apply(sync(null))::awaitBlocking)
                .isExactlyInstanceOf(RateLimitException.class)
                .asInstanceOf(InstanceOfAssertFactories.throwable(RateLimitException.class))
                .extracting(RateLimitException::getRetryAfterMillis)
                .isEqualTo(50L);

        stopwatch.setCurrentValue(50);

        assertThat(rateLimit.apply(sync(null)).awaitBlocking()).isEqualTo("2");

        stopwatch.setCurrentValue(100);

        assertThat(rateLimit.apply(sync(null)).awaitBlocking()).isEqualTo("3");

        stopwatch.setCurrentValue(150);

        assertThat(rateLimit.apply(sync(null)).awaitBlocking()).isEqualTo("4");
        assertThatThrownBy(rateLimit.apply(sync(null))::awaitBlocking)
                .isExactlyInstanceOf(RateLimitException.class)
                .asInstanceOf(InstanceOfAssertFactories.throwable(RateLimitException.class))
                .extracting(RateLimitException::getRetryAfterMillis)
                .isEqualTo(50L);
    }

    @Test
    public void smooth_multiThreaded() throws Throwable {
        AtomicInteger counter = new AtomicInteger();
        TestInvocation<String> invocation = TestInvocation.of(() -> "" + counter.incrementAndGet());
        RateLimit<String> rateLimit = new RateLimit<>(invocation, "test invocation", 2, 100, 0,
                RateLimitType.SMOOTH, stopwatch);

        List<TestThread<String>> threads = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            threads.add(runOnTestThread(rateLimit, false));
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
            assertThat(e).isExactlyInstanceOf(RateLimitException.class)
                    .asInstanceOf(InstanceOfAssertFactories.throwable(RateLimitException.class))
                    .extracting(RateLimitException::getRetryAfterMillis)
                    .isEqualTo(50L);
        });

        stopwatch.setCurrentValue(50);

        assertThat(rateLimit.apply(sync(null)).awaitBlocking()).isEqualTo("2");

        stopwatch.setCurrentValue(100);
        threads.clear();
        results.clear();
        exceptions.clear();

        for (int i = 0; i < 2; i++) {
            threads.add(runOnTestThread(rateLimit, false));
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
            assertThat(e).isExactlyInstanceOf(RateLimitException.class)
                    .asInstanceOf(InstanceOfAssertFactories.throwable(RateLimitException.class))
                    .extracting(RateLimitException::getRetryAfterMillis)
                    .isEqualTo(50L);
        });
    }

    @Test
    public void smooth_multiThreaded_withMinSpacing() throws Throwable {
        AtomicInteger counter = new AtomicInteger();
        TestInvocation<String> invocation = TestInvocation.of(() -> "" + counter.incrementAndGet());
        RateLimit<String> rateLimit = new RateLimit<>(invocation, "test invocation", 1000, 100, 10,
                RateLimitType.SMOOTH, stopwatch);

        List<TestThread<String>> threads = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            threads.add(runOnTestThread(rateLimit, false));
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
            assertThat(e).isExactlyInstanceOf(RateLimitException.class)
                    .asInstanceOf(InstanceOfAssertFactories.throwable(RateLimitException.class))
                    .extracting(RateLimitException::getRetryAfterMillis)
                    .isEqualTo(10L);
        });

        stopwatch.setCurrentValue(50);

        assertThat(rateLimit.apply(sync(null)).awaitBlocking()).isEqualTo("2");

        stopwatch.setCurrentValue(100);
        threads.clear();
        results.clear();
        exceptions.clear();

        for (int i = 0; i < 2; i++) {
            threads.add(runOnTestThread(rateLimit, false));
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
            assertThat(e).isExactlyInstanceOf(RateLimitException.class)
                    .asInstanceOf(InstanceOfAssertFactories.throwable(RateLimitException.class))
                    .extracting(RateLimitException::getRetryAfterMillis)
                    .isEqualTo(10L);
        });
    }
}
