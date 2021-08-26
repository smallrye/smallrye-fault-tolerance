package io.smallrye.faulttolerance.before.retry;

import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
public class BeforeRetryTest {

    @Inject
    private BeforeRetryTestBean beforeRetryTestBean;

    @AfterEach
    public void cleanUp() {
        beforeRetryTestBean.reset();
    }

    @Test
    public void call() {
        String value = beforeRetryTestBean.call();
        assertThat(value).isEqualTo("call 2");
        assertThat(beforeRetryTestBean.getBeforeRetryRuns()).isEqualTo(2);
    }
}