package io.smallrye.faulttolerance.retry.beforeretry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
@AddBeanClasses(MyDependency.class)
public class BeforeRetryHandlerTest {
    @Test
    public void test(BeforeRetryHandlerService service) {
        assertThrows(IllegalArgumentException.class, service::hello);
        assertThat(BeforeRetryHandlerService.ids)
                .hasSize(3)
                .containsExactly(1, 2, 3);
    }
}
