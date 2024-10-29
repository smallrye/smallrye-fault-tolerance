package io.smallrye.faulttolerance.core;

import static io.smallrye.faulttolerance.core.FaultToleranceContextUtil.sync;
import static io.smallrye.faulttolerance.core.Invocation.invocation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.core.util.TestException;

public class InvocationTest {
    @Test
    public void sameResult() throws Throwable {
        assertThat(invocation().apply(sync(() -> "foobar")).awaitBlocking()).isEqualTo("foobar");
    }

    @Test
    public void sameError() {
        assertThatThrownBy(invocation().apply(sync(TestException::doThrow))::awaitBlocking)
                .isExactlyInstanceOf(TestException.class);
    }
}
