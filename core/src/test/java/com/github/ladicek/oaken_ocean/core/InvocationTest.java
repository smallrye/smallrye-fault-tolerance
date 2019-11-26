package com.github.ladicek.oaken_ocean.core;

import com.github.ladicek.oaken_ocean.core.util.TestException;
import org.junit.Test;

import static com.github.ladicek.oaken_ocean.core.Invocation.invocation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class InvocationTest {
    @Test
    public void identicalResult() throws Exception {
        assertThat(invocation().apply(() -> "foobar")).isEqualTo("foobar");
    }

    @Test
    public void identicalException() {
        assertThatThrownBy(() -> invocation().apply(TestException::doThrow)).isExactlyInstanceOf(TestException.class);
    }
}
