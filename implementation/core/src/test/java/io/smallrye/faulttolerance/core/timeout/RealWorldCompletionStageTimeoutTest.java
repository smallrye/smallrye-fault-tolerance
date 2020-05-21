package io.smallrye.faulttolerance.core.timeout;

import static io.smallrye.faulttolerance.core.Invocation.invocation;
import static io.smallrye.faulttolerance.core.util.CompletionStages.completedStage;
import static io.smallrye.faulttolerance.core.util.CompletionStages.failedStage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.Percentage.withPercentage;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.assertj.core.data.Percentage;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.InvocationContext;
import io.smallrye.faulttolerance.core.stopwatch.RunningStopwatch;
import io.smallrye.faulttolerance.core.stopwatch.Stopwatch;
import io.smallrye.faulttolerance.core.stopwatch.SystemStopwatch;
import io.smallrye.faulttolerance.core.util.TestException;

public class RealWorldCompletionStageTimeoutTest {

    // for some reason, one of the tests takes slightly longer than the others, even though they should all
    // take cca 300 ms; I guess it's because of some initialization in the JVM or JDK
    // that one test can be `shouldTimeOut`, which is the only thing that uses the `tolerance` here
    // because of that initialization cost, the tolerance is unreasonably high (should be cca 25)
    private static final Percentage tolerance = withPercentage(50);

    // TODO if we really need something like `slowMachine` (which we shouldn't in pure unit tests),
    //  then it should be a multiplier that affects all values, not a simple boolean with yet another hardcoded value
    private static final int SLEEP_TIME = System.getProperty("slowMachine") != null ? 1000 : 300;
    private static final int TIMEOUT = System.getProperty("slowMachine") != null ? 2000 : 1000;

    private ScheduledExecutorService executor;
    private ScheduledExecutorTimeoutWatcher watcher;

    private ExecutorService timeoutActionExecutor;

    private Stopwatch stopwatch = new SystemStopwatch();

    @Before
    public void setUp() {
        executor = Executors.newSingleThreadScheduledExecutor();
        watcher = new ScheduledExecutorTimeoutWatcher(executor);

        timeoutActionExecutor = Executors.newSingleThreadExecutor();
    }

    @After
    public void tearDown() throws InterruptedException {
        executor.shutdownNow();
        timeoutActionExecutor.shutdownNow();

        executor.awaitTermination(1, TimeUnit.SECONDS);
        timeoutActionExecutor.awaitTermination(1, TimeUnit.SECONDS);
    }

    @Test
    public void shouldReturnRightAway() throws Exception {
        RunningStopwatch runningStopwatch = stopwatch.start();

        FaultToleranceStrategy<CompletionStage<String>> timeout = new CompletionStageTimeout<>(invocation(),
                "completion stage timeout", TIMEOUT, watcher, timeoutActionExecutor);

        assertThat(timeout.apply(new InvocationContext<>(() -> {
            Thread.sleep(SLEEP_TIME);
            return completedStage("foobar");
        })).toCompletableFuture().get()).isEqualTo("foobar");
        assertThat(runningStopwatch.elapsedTimeInMillis()).isCloseTo(SLEEP_TIME, tolerance);
    }

    @Test
    public void shouldPropagateMethodError() throws Exception {
        RunningStopwatch runningStopwatch = stopwatch.start();

        FaultToleranceStrategy<CompletionStage<String>> timeout = new CompletionStageTimeout<>(invocation(),
                "completion stage timeout", TIMEOUT, watcher, timeoutActionExecutor);

        assertThatThrownBy(timeout.apply(new InvocationContext<>(() -> {
            Thread.sleep(SLEEP_TIME);
            throw new TestException();
        })).toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(TestException.class);
        assertThat(runningStopwatch.elapsedTimeInMillis()).isCloseTo(SLEEP_TIME, tolerance);
    }

    @Test
    public void shouldPropagateCompletionStageError() throws Exception {
        RunningStopwatch runningStopwatch = stopwatch.start();

        FaultToleranceStrategy<CompletionStage<String>> timeout = new CompletionStageTimeout<>(invocation(),
                "completion stage timeout", TIMEOUT, watcher, timeoutActionExecutor);

        assertThatThrownBy(timeout.apply(new InvocationContext<>(() -> {
            Thread.sleep(SLEEP_TIME);
            return failedStage(new TestException());
        })).toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(TestException.class);
        assertThat(runningStopwatch.elapsedTimeInMillis()).isCloseTo(SLEEP_TIME, tolerance);
    }

    @Test
    public void shouldTimeOut() throws Exception {
        RunningStopwatch runningStopwatch = stopwatch.start();

        FaultToleranceStrategy<CompletionStage<String>> timeout = new CompletionStageTimeout<>(invocation(),
                "completion stage timeout", SLEEP_TIME, watcher, timeoutActionExecutor, true);

        assertThatThrownBy(timeout.apply(new InvocationContext<>(() -> {
            Thread.sleep(TIMEOUT);
            return completedStage("foobar");
        })).toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(TimeoutException.class);
        assertThat(runningStopwatch.elapsedTimeInMillis()).isCloseTo(SLEEP_TIME, tolerance);
    }
}
