package io.smallrye.faulttolerance.core.retry;

import static io.smallrye.faulttolerance.core.FaultToleranceContextUtil.async;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.core.Future;
import io.smallrye.faulttolerance.core.async.ThreadOffload;
import io.smallrye.faulttolerance.core.stopwatch.TestStopwatch;
import io.smallrye.faulttolerance.core.util.ExceptionDecision;
import io.smallrye.faulttolerance.core.util.ResultDecision;
import io.smallrye.faulttolerance.core.util.SetBasedExceptionDecision;
import io.smallrye.faulttolerance.core.util.SetOfThrowables;

public class RetryAsyncTest {
    private ExecutorService executor;

    @BeforeEach
    public void setUp() {
        executor = Executors.newSingleThreadExecutor();
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        executor.shutdownNow();
        executor.awaitTermination(1, TimeUnit.SECONDS);
    }

    @Test
    public void shouldNotRetryOnSuccess() throws Throwable {
        AtomicInteger invocationCount = new AtomicInteger(0);
        TestInvocation<String> invocation = TestInvocation.immediatelyReturning(() -> {
            invocationCount.incrementAndGet();
            return "shouldNotRetryOnSuccess";
        });
        ThreadOffload<String> execution = new ThreadOffload<>(invocation, executor);
        Retry<String> retry = new Retry<>(execution, "shouldNotRetryOnSuccess",
                ResultDecision.ALWAYS_EXPECTED, ExceptionDecision.ALWAYS_FAILURE, 3, 1000L,
                SyncDelay.NONE, AsyncDelay.NONE, new TestStopwatch(), null);

        Future<String> result = retry.apply(async(null));
        assertThat(result.awaitBlocking()).isEqualTo("shouldNotRetryOnSuccess");
        assertThat(invocationCount).hasValue(1);
    }

    @Test
    public void shouldRetryOnSuccessThatMatches() {
        AtomicInteger invocationCount = new AtomicInteger(0);
        TestInvocation<String> invocation = TestInvocation.immediatelyReturning(() -> {
            invocationCount.incrementAndGet();
            return "shouldRetryOnSuccessThatMatches";
        });
        ThreadOffload<String> execution = new ThreadOffload<>(invocation, executor);
        Retry<String> retry = new Retry<>(execution, "shouldNotRetryOnSuccess",
                ResultDecision.ALWAYS_FAILURE, ExceptionDecision.ALWAYS_FAILURE, 3, 1000L,
                SyncDelay.NONE, AsyncDelay.NONE, new TestStopwatch(), null);

        Future<String> result = retry.apply(async(null));
        assertThatThrownBy(result::awaitBlocking).isExactlyInstanceOf(FaultToleranceException.class);
        assertThat(invocationCount).hasValue(4);
    }

    @Test
    public void shouldPropagateAbortOnError() {
        RuntimeException error = new RuntimeException("forced");

        AtomicInteger invocationCount = new AtomicInteger(0);
        TestInvocation<String> invocation = TestInvocation.immediatelyReturning(() -> {
            invocationCount.incrementAndGet();
            throw error;
        });
        ThreadOffload<String> execution = new ThreadOffload<>(invocation, executor);
        Retry<String> retry = new Retry<>(execution, "shouldPropagateAbortOnError",
                ResultDecision.ALWAYS_EXPECTED,
                new SetBasedExceptionDecision(SetOfThrowables.ALL, SetOfThrowables.create(RuntimeException.class), false),
                3, 1000L, SyncDelay.NONE, AsyncDelay.NONE, new TestStopwatch(), null);

        Future<String> result = retry.apply(async(null));
        assertThatThrownBy(result::awaitBlocking).isEqualTo(error);
        assertThat(invocationCount).hasValue(1);
    }

    @Test
    public void shouldPropagateAbortOnErrorInCSCreation() {
        RuntimeException error = new RuntimeException("forced");

        AtomicInteger invocationCount = new AtomicInteger(0);
        TestInvocation<String> invocation = TestInvocation.immediatelyReturning(() -> {
            invocationCount.incrementAndGet();
            throw error;
        });
        ThreadOffload<String> execution = new ThreadOffload<>(invocation, executor);
        Retry<String> retry = new Retry<>(execution, "shouldPropagateAbortOnErrorInCSCreation",
                ResultDecision.ALWAYS_EXPECTED,
                new SetBasedExceptionDecision(SetOfThrowables.ALL, SetOfThrowables.create(RuntimeException.class), false),
                3, 1000L, SyncDelay.NONE, AsyncDelay.NONE, new TestStopwatch(), null);

        Future<String> result = retry.apply(async(null));
        assertThatThrownBy(result::awaitBlocking).isEqualTo(error);
        assertThat(invocationCount).hasValue(1);
    }

    @Test
    public void shouldRetryOnce() throws Throwable {
        RuntimeException error = new RuntimeException("forced");

        AtomicInteger invocationCount = new AtomicInteger(0);
        TestInvocation<String> invocation = TestInvocation.immediatelyReturning(() -> {
            int prevInvoCnt = invocationCount.getAndIncrement();
            if (prevInvoCnt == 0) {
                throw error;
            } else {
                return "shouldRetryOnce";
            }
        });
        ThreadOffload<String> execution = new ThreadOffload<>(invocation, executor);
        Retry<String> retry = new Retry<>(execution, "shouldRetryOnce",
                ResultDecision.ALWAYS_EXPECTED,
                new SetBasedExceptionDecision(SetOfThrowables.create(RuntimeException.class), SetOfThrowables.EMPTY, false),
                3, 1000L, SyncDelay.NONE, AsyncDelay.NONE, new TestStopwatch(), null);

        Future<String> result = retry.apply(async(null));
        assertThat(result.awaitBlocking()).isEqualTo("shouldRetryOnce");
        assertThat(invocationCount).hasValue(2);
    }

    @Test
    public void shouldRetryOnceOnCsFailure() throws Throwable {
        RuntimeException error = new RuntimeException("forced");

        AtomicInteger invocationCount = new AtomicInteger(0);
        TestInvocation<String> invocation = TestInvocation.immediatelyReturning(() -> {
            int prevInvoCnt = invocationCount.getAndIncrement();
            if (prevInvoCnt == 0) {
                throw error;
            } else {
                return "shouldRetryOnceOnCsFailure";
            }
        });
        ThreadOffload<String> execution = new ThreadOffload<>(invocation, executor);
        Retry<String> retry = new Retry<>(execution, "shouldRetryOnceOnCsFailure",
                ResultDecision.ALWAYS_EXPECTED,
                new SetBasedExceptionDecision(SetOfThrowables.create(RuntimeException.class), SetOfThrowables.EMPTY, false),
                3, 1000L, SyncDelay.NONE, AsyncDelay.NONE, new TestStopwatch(), null);

        Future<String> result = retry.apply(async(null));
        assertThat(result.awaitBlocking()).isEqualTo("shouldRetryOnceOnCsFailure");
        assertThat(invocationCount).hasValue(2);
    }

    @Test
    public void shouldRetryMaxTimesAndSucceed() throws Throwable {
        RuntimeException error = new RuntimeException("forced");

        AtomicInteger invocationCount = new AtomicInteger(0);
        TestInvocation<String> invocation = TestInvocation.immediatelyReturning(() -> {
            int prevInvoCnt = invocationCount.getAndIncrement();
            if (prevInvoCnt < 3) {
                throw error;
            } else {
                return "shouldRetryMaxTimesAndSucceed";
            }
        });
        ThreadOffload<String> execution = new ThreadOffload<>(invocation, executor);
        Retry<String> retry = new Retry<>(execution, "shouldRetryMaxTimesAndSucceed",
                ResultDecision.ALWAYS_EXPECTED,
                new SetBasedExceptionDecision(SetOfThrowables.create(RuntimeException.class), SetOfThrowables.EMPTY, false),
                3, 1000L, SyncDelay.NONE, AsyncDelay.NONE, new TestStopwatch(), null);

        Future<String> result = retry.apply(async(null));
        assertThat(result.awaitBlocking()).isEqualTo("shouldRetryMaxTimesAndSucceed");
        assertThat(invocationCount).hasValue(4);
    }

    @Test
    public void shouldRetryMaxTimesAndFail() {
        RuntimeException error = new RuntimeException("forced");

        AtomicInteger invocationCount = new AtomicInteger(0);
        TestInvocation<String> invocation = TestInvocation.immediatelyReturning(() -> {
            invocationCount.incrementAndGet();
            throw error;
        });
        ThreadOffload<String> execution = new ThreadOffload<>(invocation, executor);
        Retry<String> retry = new Retry<>(execution, "shouldRetryMaxTimesAndSucceed",
                ResultDecision.ALWAYS_EXPECTED,
                new SetBasedExceptionDecision(SetOfThrowables.create(RuntimeException.class), SetOfThrowables.EMPTY, false),
                3, 1000L, SyncDelay.NONE, AsyncDelay.NONE, new TestStopwatch(), null);

        Future<String> result = retry.apply(async(null));
        assertThatThrownBy(result::awaitBlocking).isEqualTo(error);
        assertThat(invocationCount).hasValue(4);
    }
}
