package io.smallrye.faulttolerance.retry.beforeretry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
public class BeforeRetryMethodTest {
    @Test
    public void test(BeforeRetryMethodService service) {
        assertThrows(IllegalArgumentException.class, service::hello);
        assertThat(BeforeRetryMethodService.ids)
                .hasSize(3)
                .containsExactly(1, 2, 3);
    }
}
