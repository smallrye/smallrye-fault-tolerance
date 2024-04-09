package io.smallrye.faulttolerance.retry.when.result;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceException;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
public class RetryWhenResultTest {
    @Test
    public void matchingResult(RetryWhenResultMatchesService service) {
        assertThatThrownBy(() -> {
            service.hello();
        }).isExactlyInstanceOf(FaultToleranceException.class);
        assertThat(service.getAttempts()).hasValue(4);
    }

    @Test
    public void nonMatchingResult(RetryWhenResultDoesNotMatchService service) {
        assertThat(service.hello()).isEqualTo("hello");
        assertThat(service.getAttempts()).hasValue(1);
    }
}
