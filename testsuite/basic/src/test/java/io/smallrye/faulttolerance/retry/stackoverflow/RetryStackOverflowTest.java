package io.smallrye.faulttolerance.retry.stackoverflow;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
public class RetryStackOverflowTest {
    @Test
    public void test(MyService service) {
        assertEquals("fallback", service.hello());
    }
}
