package io.smallrye.faulttolerance.core.timeout;

import static io.smallrye.faulttolerance.core.FaultToleranceContextUtil.async;
import static io.smallrye.faulttolerance.core.Invocation.invocation;
import static io.smallrye.faulttolerance.core.util.SneakyThrow.sneakyThrow;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.Percentage.withPercentage;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.assertj.core.data.Percentage;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.async.ThreadOffload;
import io.smallrye.faulttolerance.core.stopwatch.RunningStopwatch;
import io.smallrye.faulttolerance.core.stopwatch.Stopwatch;
import io.smallrye.faulttolerance.core.stopwatch.SystemStopwatch;
import io.smallrye.faulttolerance.core.timer.ThreadTimer;
import io.smallrye.faulttolerance.core.timer.Timer;
import io.smallrye.faulttolerance.core.util.TestException;

public class RealWorldAsyncTimeoutTest {
    // for some reason, one of the tests takes slightly longer than the others, even though they should all
    // take cca 300 ms; I guess it's because of some initialization in the JVM or JDK
    // that one test can be `shouldTimeOut`, which is the only thing that uses the `tolerance` here
    // because of that initialization cost, the tolerance is unreasonably high (should be cca 25)
    private static final Percentage tolerance = withPercentage(50);

    // TODO if we really need something like `slowMachine` (which we shouldn't in pure unit tests),
    //  then it should be a multiplier that affects all values, not a simple boolean with yet another hardcoded value
    private static final int SLEEP_TIME = System.getProperty("slowMachine") != null ? 1000 : 300;
    private static final int TIMEOUT = System.getProperty("slowMachine") != null ? 2000 : 1000;

    private ExecutorService executor;

    private ExecutorService timerExecutor;
    private Timer timer;

    private Stopwatch stopwatch = SystemStopwatch.INSTANCE;

    @BeforeEach
    public void setUp() {
        executor = Executors.newSingleThreadExecutor();

        timerExecutor = Executors.newSingleThreadExecutor();
        timer = new ThreadTimer(timerExecutor);
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        timer.shutdown();
        timerExecutor.shutdownNow();
        timerExecutor.awaitTermination(1, TimeUnit.SECONDS);

        executor.shutdownNow();
        executor.awaitTermination(1, TimeUnit.SECONDS);
    }

    @Test
    public void shouldReturnValue() throws Throwable {
        RunningStopwatch runningStopwatch = stopwatch.start();

        ThreadOffload<String> execution = new ThreadOffload<>(invocation(), executor);
        FaultToleranceStrategy<String> timeout = new Timeout<>(execution,
                "completion stage timeout", TIMEOUT, timer);

        assertThat(timeout.apply(async(() -> {
            try {
                Thread.sleep(SLEEP_TIME);
            } catch (InterruptedException e) {
                throw sneakyThrow(e);
            }
            return "foobar";
        })).awaitBlocking()).isEqualTo("foobar");
        assertThat(runningStopwatch.elapsedTimeInMillis()).isCloseTo(SLEEP_TIME, tolerance);
    }

    @Test
    public void shouldThrowException() {
        RunningStopwatch runningStopwatch = stopwatch.start();

        ThreadOffload<String> execution = new ThreadOffload<>(invocation(), executor);
        Timeout<String> timeout = new Timeout<>(execution, "completion stage timeout", TIMEOUT, timer);

        assertThatThrownBy(timeout.apply(async(() -> {
            try {
                Thread.sleep(SLEEP_TIME);
            } catch (Exception e) {
                throw sneakyThrow(e);
            }
            throw new TestException();
        }))::awaitBlocking).isExactlyInstanceOf(TestException.class);
        assertThat(runningStopwatch.elapsedTimeInMillis()).isCloseTo(SLEEP_TIME, tolerance);
    }

    @Test
    public void shouldTimeOut() {
        RunningStopwatch runningStopwatch = stopwatch.start();

        ThreadOffload<String> execution = new ThreadOffload<>(invocation(), executor);
        Timeout<String> timeout = new Timeout<>(execution,
                "completion stage timeout", SLEEP_TIME, timer);

        assertThatThrownBy(timeout.apply(async(() -> {
            try {
                Thread.sleep(TIMEOUT);
            } catch (InterruptedException e) {
                throw sneakyThrow(e);
            }
            return "foobar";
        }))::awaitBlocking).isExactlyInstanceOf(TimeoutException.class);
        assertThat(runningStopwatch.elapsedTimeInMillis()).isCloseTo(SLEEP_TIME, tolerance);
    }
}
