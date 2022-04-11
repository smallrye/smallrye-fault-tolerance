package io.smallrye.faulttolerance.programmatic;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.Callable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.api.FaultTolerance;
import io.smallrye.faulttolerance.core.util.TestException;
import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;
import io.smallrye.faulttolerance.util.WithSystemProperty;

// we boot a new Weld container for each test, so the config property will be read again
// this is impossible for the standalone implementation (unless we forked the JVM for each test)
@FaultToleranceBasicTest
@WithSystemProperty(key = "MP_Fault_Tolerance_NonFallback_Enabled", value = "false")
public class CdiSkipFaultToleranceTest {
    private int counter;

    @BeforeEach
    public void setUp() {
        counter = 0;
    }

    @Test
    public void skipFaultTolerance() throws Exception {
        Callable<String> guarded = FaultTolerance.createCallable(this::action)
                .withRetry().maxRetries(3).done()
                .withFallback().handler(this::fallback).done()
                .build();

        assertThat(guarded.call()).isEqualTo("fallback");
        assertThat(counter).isEqualTo(1); // 1 initial invocation, no retries
    }

    public String action() throws TestException {
        counter++;
        throw new TestException();
    }

    public String fallback() {
        return "fallback";
    }
}
