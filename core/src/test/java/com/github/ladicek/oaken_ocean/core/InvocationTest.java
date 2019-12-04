package com.github.ladicek.oaken_ocean.core;

import static com.github.ladicek.oaken_ocean.core.Invocation.invocation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.Test;

import com.github.ladicek.oaken_ocean.core.util.TestException;

public class InvocationTest {
    @Test
    public void identicalResult() throws Exception {
        assertThat(invocation().apply(new SimpleInvocationContext<>(() -> "foobar"))).isEqualTo("foobar");
    }

    @Test
    public void identicalException() {
        assertThatThrownBy(() -> invocation().apply(new SimpleInvocationContext<>(TestException::doThrow)))
                .isExactlyInstanceOf(TestException.class);
    }
}
