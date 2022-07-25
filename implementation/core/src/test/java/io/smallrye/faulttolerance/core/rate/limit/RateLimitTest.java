package io.smallrye.faulttolerance.core.rate.limit;

import static io.smallrye.faulttolerance.core.util.TestThread.runOnTestThread;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.api.RateLimitException;
import io.smallrye.faulttolerance.api.RateLimitType;
import io.smallrye.faulttolerance.core.InvocationContext;
import io.smallrye.faulttolerance.core.stopwatch.TestStopwatch;
import io.smallrye.faulttolerance.core.util.TestInvocation;
import io.smallrye.faulttolerance.core.util.TestThread;

public class RateLimitTest {
    private TestStopwatch stopwatch;

    @BeforeEach
    public void setUp() {
        stopwatch = new TestStopwatch();
    }

    @Test
    public void fixed_singleThreaded() throws Exception {
        AtomicInteger counter = new AtomicInteger();
        TestInvocation<String> invocation = TestInvocation.of(() -> "" + counter.incrementAndGet());
        RateLimit<String> rateLimit = new RateLimit<>(invocation, "test invocation", 2, 100, 0,
                RateLimitType.FIXED, stopwatch);

        String result = rateLimit.apply(new InvocationContext<>(() -> "ignored"));
        assertThat(result).isEqualTo("1");
        result = rateLimit.apply(new InvocationContext<>(() -> "ignored"));
        assertThat(result).isEqualTo("2");
        assertThatCode(() -> rateLimit.apply(new InvocationContext<>(() -> "ignored")))
                .isExactlyInstanceOf(RateLimitException.class);

        stopwatch.setCurrentValue(50);

        assertThatCode(() -> rateLimit.apply(new InvocationContext<>(() -> "ignored")))
                .isExactlyInstanceOf(RateLimitException.class);

        stopwatch.setCurrentValue(100);

        result = rateLimit.apply(new InvocationContext<>(() -> "ignored"));
        assertThat(result).isEqualTo("3");
        result = rateLimit.apply(new InvocationContext<>(() -> "ignored"));
        assertThat(result).isEqualTo("4");
        assertThatCode(() -> rateLimit.apply(new InvocationContext<>(() -> "ignored")))
                .isExactlyInstanceOf(RateLimitException.class);
    }

    @Test
    public void fixed_multiThreaded() {
        AtomicInteger counter = new AtomicInteger();
        TestInvocation<String> invocation = TestInvocation.of(() -> "" + counter.incrementAndGet());
        RateLimit<String> rateLimit = new RateLimit<>(invocation, "test invocation", 2, 100, 0,
                RateLimitType.FIXED, stopwatch);

        List<TestThread<String>> threads = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            threads.add(runOnTestThread(rateLimit));
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
            assertThat(e).isExactlyInstanceOf(RateLimitException.class);
        });

        stopwatch.setCurrentValue(50);

        assertThatCode(() -> rateLimit.apply(new InvocationContext<>(() -> "ignored")))
                .isExactlyInstanceOf(RateLimitException.class);

        stopwatch.setCurrentValue(100);
        threads.clear();
        results.clear();
        exceptions.clear();

        for (int i = 0; i < 2; i++) {
            threads.add(runOnTestThread(rateLimit));
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
            threads.add(runOnTestThread(rateLimit));
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

        stopwatch.setCurrentValue(50);

        assertThatCode(() -> rateLimit.apply(new InvocationContext<>(() -> "ignored")))
                .isExactlyInstanceOf(RateLimitException.class);

        stopwatch.setCurrentValue(100);
        threads.clear();
        results.clear();
        exceptions.clear();

        for (int i = 0; i < 2; i++) {
            threads.add(runOnTestThread(rateLimit));
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
            assertThat(e).isExactlyInstanceOf(RateLimitException.class);
        });
    }

    @Test
    public void rolling_singleThreaded() throws Exception {
        AtomicInteger counter = new AtomicInteger();
        TestInvocation<String> invocation = TestInvocation.of(() -> "" + counter.incrementAndGet());
        RateLimit<String> rateLimit = new RateLimit<>(invocation, "test invocation", 2, 100, 0,
                RateLimitType.ROLLING, stopwatch);

        String result = rateLimit.apply(new InvocationContext<>(() -> "ignored"));
        assertThat(result).isEqualTo("1");
        result = rateLimit.apply(new InvocationContext<>(() -> "ignored"));
        assertThat(result).isEqualTo("2");
        assertThatCode(() -> rateLimit.apply(new InvocationContext<>(() -> "ignored")))
                .isExactlyInstanceOf(RateLimitException.class);

        stopwatch.setCurrentValue(50);

        assertThatCode(() -> rateLimit.apply(new InvocationContext<>(() -> "ignored")))
                .isExactlyInstanceOf(RateLimitException.class);

        stopwatch.setCurrentValue(100);

        result = rateLimit.apply(new InvocationContext<>(() -> "ignored"));
        assertThat(result).isEqualTo("3");
        assertThatCode(() -> rateLimit.apply(new InvocationContext<>(() -> "ignored")))
                .isExactlyInstanceOf(RateLimitException.class);
    }

    @Test
    public void rolling_multiThreaded() {
        AtomicInteger counter = new AtomicInteger();
        TestInvocation<String> invocation = TestInvocation.of(() -> "" + counter.incrementAndGet());
        RateLimit<String> rateLimit = new RateLimit<>(invocation, "test invocation", 2, 100, 0,
                RateLimitType.ROLLING, stopwatch);

        List<TestThread<String>> threads = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            threads.add(runOnTestThread(rateLimit));
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
            assertThat(e).isExactlyInstanceOf(RateLimitException.class);
        });

        stopwatch.setCurrentValue(50);

        assertThatCode(() -> rateLimit.apply(new InvocationContext<>(() -> "ignored")))
                .isExactlyInstanceOf(RateLimitException.class);

        stopwatch.setCurrentValue(100);
        threads.clear();
        results.clear();
        exceptions.clear();

        for (int i = 0; i < 2; i++) {
            threads.add(runOnTestThread(rateLimit));
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
            assertThat(e).isExactlyInstanceOf(RateLimitException.class);
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
            threads.add(runOnTestThread(rateLimit));
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

        stopwatch.setCurrentValue(50);

        assertThatCode(() -> rateLimit.apply(new InvocationContext<>(() -> "ignored")))
                .isExactlyInstanceOf(RateLimitException.class);

        stopwatch.setCurrentValue(150);
        threads.clear();
        results.clear();
        exceptions.clear();

        for (int i = 0; i < 2; i++) {
            threads.add(runOnTestThread(rateLimit));
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
            assertThat(e).isExactlyInstanceOf(RateLimitException.class);
        });
    }
}
