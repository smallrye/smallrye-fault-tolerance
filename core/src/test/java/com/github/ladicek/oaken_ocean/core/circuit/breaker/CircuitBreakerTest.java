package com.github.ladicek.oaken_ocean.core.circuit.breaker;

import com.github.ladicek.oaken_ocean.core.stopwatch.TestStopwatch;
import com.github.ladicek.oaken_ocean.core.util.SetOfThrowables;
import com.github.ladicek.oaken_ocean.core.util.TestException;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static com.github.ladicek.oaken_ocean.core.Invocation.invocation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CircuitBreakerTest {
    private static final SetOfThrowables testException = SetOfThrowables.create(Collections.singletonList(TestException.class));

    private TestStopwatch stopwatch;

    @Before
    public void setUp() {
        stopwatch = new TestStopwatch();
    }

    @Test
    public void test1() throws Exception {
        CircuitBreaker<String> cb = new CircuitBreaker<>(invocation(), "test invocation", testException,
                1000, 4, 0.5, 2, stopwatch);

        // circuit breaker is closed
        assertThat(cb.apply(() -> "foobar1")).isEqualTo("foobar1");
        assertThat(cb.apply(() -> "foobar2")).isEqualTo("foobar2");
        assertThatThrownBy(() -> cb.apply(() -> { throw new RuntimeException(); })).isExactlyInstanceOf(RuntimeException.class); // treated as success
        assertThat(cb.apply(() -> "foobar3")).isEqualTo("foobar3");
        assertThat(cb.apply(() -> "foobar4")).isEqualTo("foobar4");
        assertThat(cb.apply(() -> "foobar5")).isEqualTo("foobar5");
        assertThatThrownBy(() -> cb.apply(TestException::doThrow)).isExactlyInstanceOf(TestException.class);
        assertThat(cb.apply(() -> "foobar6")).isEqualTo("foobar6");
        assertThatThrownBy(() -> cb.apply(TestException::doThrow)).isExactlyInstanceOf(TestException.class);
        // circuit breaker is open
        assertThatThrownBy(() -> cb.apply(() -> "ignored")).isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(() -> cb.apply(() -> "ignored")).isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(() -> cb.apply(() -> "ignored")).isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(() -> cb.apply(() -> "ignored")).isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(() -> cb.apply(() -> "ignored")).isExactlyInstanceOf(CircuitBreakerOpenException.class);
        stopwatch.setCurrentValue(1500);
        assertThat(cb.apply(() -> "foobar7")).isEqualTo("foobar7");
        // circuit breaker is half-open
        assertThatThrownBy(() -> cb.apply(TestException::doThrow)).isExactlyInstanceOf(TestException.class);
        // circuit breaker is open
        stopwatch.setCurrentValue(0);
        assertThatThrownBy(() -> cb.apply(() -> "ignored")).isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(() -> cb.apply(() -> "ignored")).isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(() -> cb.apply(() -> "ignored")).isExactlyInstanceOf(CircuitBreakerOpenException.class);
        stopwatch.setCurrentValue(1500);
        assertThat(cb.apply(() -> "foobar8")).isEqualTo("foobar8");
        // circuit breaker is half-open
        assertThat(cb.apply(() -> "foobar9")).isEqualTo("foobar9");
        // circuit breaker is closed
        assertThat(cb.apply(() -> "foobar10")).isEqualTo("foobar10");
    }

    @Test
    public void test2() throws Exception {
        CircuitBreaker<String> cb = new CircuitBreaker<>(invocation(), "test invocation", testException,
                1000, 4, 0.5, 2, stopwatch);

        // circuit breaker is closed
        assertThat(cb.apply(() -> "foobar1")).isEqualTo("foobar1");
        assertThatThrownBy(() -> cb.apply(TestException::doThrow)).isExactlyInstanceOf(TestException.class);
        assertThatThrownBy(() -> cb.apply(TestException::doThrow)).isExactlyInstanceOf(TestException.class);
        assertThat(cb.apply(() -> "foobar2")).isEqualTo("foobar2");
        // circuit breaker is open
        assertThatThrownBy(() -> cb.apply(() -> "ignored")).isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(() -> cb.apply(() -> "ignored")).isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(() -> cb.apply(() -> "ignored")).isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(() -> cb.apply(() -> "ignored")).isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(() -> cb.apply(() -> "ignored")).isExactlyInstanceOf(CircuitBreakerOpenException.class);
        stopwatch.setCurrentValue(1500);
        assertThat(cb.apply(() -> "foobar3")).isEqualTo("foobar3");
        // circuit breaker is half-open
        assertThat(cb.apply(() -> "foobar4")).isEqualTo("foobar4");
        // circuit breaker is closed
        assertThat(cb.apply(() -> "foobar5")).isEqualTo("foobar5");
    }
}
