package io.smallrye.faulttolerance.retry.when.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
public class RetryWhenExceptionTest {
    @Test
    public void matchingException(RetryWhenExceptionMatchesService service) {
        assertThatThrownBy(() -> {
            service.hello();
        }).isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(service.getAttempts()).hasValue(4);
    }

    @Test
    public void nonMatchingException(RetryWhenExceptionDoesNotMatchService service) {
        assertThatThrownBy(() -> {
            service.hello();
        }).isExactlyInstanceOf(IllegalStateException.class);
        assertThat(service.getAttempts()).hasValue(1);
    }
}
