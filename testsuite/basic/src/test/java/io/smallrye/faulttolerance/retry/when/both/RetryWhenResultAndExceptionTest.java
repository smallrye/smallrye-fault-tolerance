package io.smallrye.faulttolerance.retry.when.both;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
public class RetryWhenResultAndExceptionTest {
    @Test
    public void test(RetryWhenResultAndExceptionService service) {
        assertThat(service.hello()).isEqualTo("hello");
        assertThat(service.getAttempts()).hasValue(3);
    }
}
