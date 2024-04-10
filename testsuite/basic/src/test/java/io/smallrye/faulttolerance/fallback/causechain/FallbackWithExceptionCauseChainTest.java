package io.smallrye.faulttolerance.fallback.causechain;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;
import io.smallrye.faulttolerance.util.WithSystemProperty;

@WithSystemProperty(key = "smallrye.faulttolerance.mp-compatibility", value = "false")
@FaultToleranceBasicTest
public class FallbackWithExceptionCauseChainTest {
    @Test
    public void bothSkipOnAndApplyOn(FallbackWithBothSkipOnAndApplyOn bean) {
        assertThatCode(() -> bean.hello(new RuntimeException()))
                .isExactlyInstanceOf(RuntimeException.class);
        assertThatCode(() -> bean.hello(new RuntimeException(new IOException())))
                .doesNotThrowAnyException();
        assertThatCode(() -> bean.hello(new RuntimeException(new ExpectedOutcomeException())))
                .isExactlyInstanceOf(RuntimeException.class);

        assertThatCode(() -> bean.hello(new Exception()))
                .isExactlyInstanceOf(Exception.class);
        assertThatCode(() -> bean.hello(new Exception(new IOException())))
                .doesNotThrowAnyException();
        assertThatCode(() -> bean.hello(new Exception(new ExpectedOutcomeException())))
                .isExactlyInstanceOf(Exception.class);

        assertThatCode(() -> bean.hello(new IOException()))
                .doesNotThrowAnyException();
        assertThatCode(() -> bean.hello(new IOException(new Exception())))
                .doesNotThrowAnyException();
        assertThatCode(() -> bean.hello(new IOException(new ExpectedOutcomeException())))
                .doesNotThrowAnyException();

        assertThatCode(() -> bean.hello(new ExpectedOutcomeException()))
                .isExactlyInstanceOf(ExpectedOutcomeException.class);
        assertThatCode(() -> bean.hello(new ExpectedOutcomeException(new Exception())))
                .isExactlyInstanceOf(ExpectedOutcomeException.class);
        assertThatCode(() -> bean.hello(new ExpectedOutcomeException(new IOException())))
                .isExactlyInstanceOf(ExpectedOutcomeException.class);
    }

    @Test
    public void skipOn(FallbackWithSkipOn bean) {
        assertThatCode(() -> bean.hello(new RuntimeException()))
                .doesNotThrowAnyException();
        assertThatCode(() -> bean.hello(new RuntimeException(new IOException())))
                .doesNotThrowAnyException();
        assertThatCode(() -> bean.hello(new RuntimeException(new ExpectedOutcomeException())))
                .isExactlyInstanceOf(RuntimeException.class);

        assertThatCode(() -> bean.hello(new Exception()))
                .doesNotThrowAnyException();
        assertThatCode(() -> bean.hello(new Exception(new IOException())))
                .doesNotThrowAnyException();
        assertThatCode(() -> bean.hello(new Exception(new ExpectedOutcomeException())))
                .isExactlyInstanceOf(Exception.class);

        assertThatCode(() -> bean.hello(new IOException()))
                .doesNotThrowAnyException();
        assertThatCode(() -> bean.hello(new IOException(new Exception())))
                .doesNotThrowAnyException();
        assertThatCode(() -> bean.hello(new IOException(new ExpectedOutcomeException())))
                .isExactlyInstanceOf(IOException.class);

        assertThatCode(() -> bean.hello(new ExpectedOutcomeException()))
                .isExactlyInstanceOf(ExpectedOutcomeException.class);
        assertThatCode(() -> bean.hello(new ExpectedOutcomeException(new Exception())))
                .isExactlyInstanceOf(ExpectedOutcomeException.class);
        assertThatCode(() -> bean.hello(new ExpectedOutcomeException(new IOException())))
                .isExactlyInstanceOf(ExpectedOutcomeException.class);
    }

    @Test
    public void applyOn(FallbackWithApplyOn bean) {
        assertThatCode(() -> bean.hello(new RuntimeException()))
                .isExactlyInstanceOf(RuntimeException.class);
        assertThatCode(() -> bean.hello(new RuntimeException(new IOException())))
                .doesNotThrowAnyException();
        assertThatCode(() -> bean.hello(new RuntimeException(new ExpectedOutcomeException())))
                .isExactlyInstanceOf(RuntimeException.class);

        assertThatCode(() -> bean.hello(new Exception()))
                .isExactlyInstanceOf(Exception.class);
        assertThatCode(() -> bean.hello(new Exception(new IOException())))
                .doesNotThrowAnyException();
        assertThatCode(() -> bean.hello(new Exception(new ExpectedOutcomeException())))
                .isExactlyInstanceOf(Exception.class);

        assertThatCode(() -> bean.hello(new IOException()))
                .doesNotThrowAnyException();
        assertThatCode(() -> bean.hello(new IOException(new Exception())))
                .doesNotThrowAnyException();
        assertThatCode(() -> bean.hello(new IOException(new ExpectedOutcomeException())))
                .doesNotThrowAnyException();

        assertThatCode(() -> bean.hello(new ExpectedOutcomeException()))
                .isExactlyInstanceOf(ExpectedOutcomeException.class);
        assertThatCode(() -> bean.hello(new ExpectedOutcomeException(new Exception())))
                .isExactlyInstanceOf(ExpectedOutcomeException.class);
        assertThatCode(() -> bean.hello(new ExpectedOutcomeException(new IOException())))
                .doesNotThrowAnyException();
    }
}
