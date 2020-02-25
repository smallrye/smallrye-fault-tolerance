package io.smallrye.faulttolerance.core;

import static io.smallrye.faulttolerance.core.Invocation.invocation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.Test;

import io.smallrye.faulttolerance.core.util.TestException;

public class GeneralMetricsRecorderTest {
    @Test
    public void successfulInvocation() throws Exception {
        MockGeneralMetrics metrics = new MockGeneralMetrics();

        GeneralMetricsRecorder<String> recorder = new GeneralMetricsRecorder<>(invocation(), metrics);
        assertThat(recorder.apply(new InvocationContext<>(() -> "foobar"))).isEqualTo("foobar");

        assertThat(metrics.invoked).isEqualTo(1);
        assertThat(metrics.failed).isEqualTo(0);
    }

    @Test
    public void failingInvocation() {
        MockGeneralMetrics metrics = new MockGeneralMetrics();

        GeneralMetricsRecorder<Void> recorder = new GeneralMetricsRecorder<>(invocation(), metrics);
        assertThatThrownBy(() -> recorder.apply(new InvocationContext<>(TestException::doThrow)))
                .isExactlyInstanceOf(TestException.class);

        assertThat(metrics.invoked).isEqualTo(1);
        assertThat(metrics.failed).isEqualTo(1);
    }

    private static class MockGeneralMetrics implements GeneralMetrics {
        int invoked;
        int failed;

        @Override
        public void invoked() {
            invoked++;
        }

        @Override
        public void failed() {
            failed++;
        }
    }
}
