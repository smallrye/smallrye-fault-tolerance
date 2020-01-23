package io.smallrye.faulttolerance.core.circuit.breaker;

import static io.smallrye.faulttolerance.core.Invocation.invocation;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import java.util.concurrent.Future;

import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.junit.Before;
import org.junit.Test;

import io.smallrye.faulttolerance.core.InvocationContext;
import io.smallrye.faulttolerance.core.stopwatch.TestStopwatch;
import io.smallrye.faulttolerance.core.util.SetOfThrowables;
import io.smallrye.faulttolerance.core.util.TestException;

public class FutureCircuitBreakerTest {
    private static final SetOfThrowables testException = SetOfThrowables.create(Collections.singletonList(TestException.class));

    private TestStopwatch stopwatch;

    @Before
    public void setUp() {
        stopwatch = new TestStopwatch();
    }

    @Test
    public void test1() throws Exception {
        CircuitBreaker<Future<String>> cb = new CircuitBreaker<>(invocation(),
                "test invocation", testException, SetOfThrowables.EMPTY,
                1000, 4, 0.5, 2, stopwatch, null);

        // circuit breaker is closed
        assertThat(cb.apply(new InvocationContext<>(() -> completedFuture("foobar1"))).get()).isEqualTo("foobar1");
        assertThat(cb.apply(new InvocationContext<>(() -> completedFuture("foobar2"))).get()).isEqualTo("foobar2");
        assertThatThrownBy(() -> cb.apply(new InvocationContext<>(() -> {
            throw new RuntimeException();
        }))).isExactlyInstanceOf(RuntimeException.class); // treated as success
        assertThat(cb.apply(new InvocationContext<>(() -> completedFuture("foobar3"))).get()).isEqualTo("foobar3");
        assertThat(cb.apply(new InvocationContext<>(() -> completedFuture("foobar4"))).get()).isEqualTo("foobar4");
        assertThat(cb.apply(new InvocationContext<>(() -> completedFuture("foobar5"))).get()).isEqualTo("foobar5");
        assertThatThrownBy(() -> cb.apply(new InvocationContext<>(TestException::doThrow)))
                .isExactlyInstanceOf(TestException.class);
        assertThat(cb.apply(new InvocationContext<>(() -> completedFuture("foobar6"))).get()).isEqualTo("foobar6");
        assertThatThrownBy(() -> cb.apply(new InvocationContext<>(TestException::doThrow)))
                .isExactlyInstanceOf(TestException.class);
        // circuit breaker is open
        assertThatThrownBy(() -> cb.apply(new InvocationContext<>(() -> completedFuture("ignored"))))
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(() -> cb.apply(new InvocationContext<>(() -> completedFuture("ignored"))))
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(() -> cb.apply(new InvocationContext<>(() -> completedFuture("ignored"))))
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(() -> cb.apply(new InvocationContext<>(() -> completedFuture("ignored"))))
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(() -> cb.apply(new InvocationContext<>(() -> completedFuture("ignored"))))
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        stopwatch.setCurrentValue(1500);
        Future<String> foobar7 = cb.apply(new InvocationContext<>(() -> completedFuture("foobar7")));
        assertThat(foobar7.get()).isEqualTo("foobar7");

        // circuit breaker is half-open
        assertThatThrownBy(() -> cb.apply(new InvocationContext<>(TestException::doThrow)))
                .isExactlyInstanceOf(TestException.class);
        // circuit breaker is open
        stopwatch.setCurrentValue(0);
        assertThatThrownBy(() -> cb.apply(new InvocationContext<>(() -> completedFuture("ignored"))))
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(() -> cb.apply(new InvocationContext<>(() -> completedFuture("ignored"))))
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(() -> cb.apply(new InvocationContext<>(() -> completedFuture("ignored"))))
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        stopwatch.setCurrentValue(1500);
        Future<String> foobar8 = cb.apply(new InvocationContext<>(() -> completedFuture("foobar8")));
        assertThat(foobar8.get()).isEqualTo("foobar8");
        // circuit breaker is half-open
        Future<String> foobar9 = cb.apply(new InvocationContext<>(() -> completedFuture("foobar9")));
        assertThat(foobar9.get()).isEqualTo("foobar9");
        // circuit breaker is closed
        Future<String> foobar10 = cb.apply(new InvocationContext<>(() -> completedFuture("foobar10")));
        assertThat(foobar10.get()).isEqualTo("foobar10");
    }

    @Test
    public void test2() throws Exception {
        CircuitBreaker<Future<String>> cb = new CircuitBreaker<>(invocation(),
                "test invocation", testException, SetOfThrowables.EMPTY,
                1000, 4, 0.5, 2, stopwatch, null);

        // circuit breaker is closed
        Future<String> foobar1 = cb.apply(new InvocationContext<>(() -> completedFuture("foobar1")));
        assertThat(foobar1.get()).isEqualTo("foobar1");
        assertThatThrownBy(() -> cb.apply(new InvocationContext<>(TestException::doThrow)))
                .isExactlyInstanceOf(TestException.class);
        assertThatThrownBy(() -> cb.apply(new InvocationContext<>(TestException::doThrow)))
                .isExactlyInstanceOf(TestException.class);
        Future<String> foobar2 = cb.apply(new InvocationContext<>(() -> completedFuture("foobar2")));
        assertThat(foobar2.get()).isEqualTo("foobar2");
        // circuit breaker is open
        assertThatThrownBy(() -> cb.apply(new InvocationContext<>(() -> completedFuture("ignored"))))
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(() -> cb.apply(new InvocationContext<>(() -> completedFuture("ignored"))))
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(() -> cb.apply(new InvocationContext<>(() -> completedFuture("ignored"))))
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(() -> cb.apply(new InvocationContext<>(() -> completedFuture("ignored"))))
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(() -> cb.apply(new InvocationContext<>(() -> completedFuture("ignored"))))
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        stopwatch.setCurrentValue(1500);
        assertThat(cb.apply(new InvocationContext<>(() -> completedFuture("foobar3"))).get()).isEqualTo("foobar3");
        // circuit breaker is half-open
        assertThat(cb.apply(new InvocationContext<>(() -> completedFuture("foobar4"))).get()).isEqualTo("foobar4");
        // circuit breaker is closed
        assertThat(cb.apply(new InvocationContext<>(() -> completedFuture("foobar5"))).get()).isEqualTo("foobar5");
    }
}
