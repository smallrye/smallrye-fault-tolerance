package io.smallrye.faulttolerance.reuse.async.uni;

import static org.assertj.core.api.Assertions.assertThat;

import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
@AddBeanClasses(MyFaultTolerance.class)
public class ReuseAsyncUniTest {
    @Test
    public void test(MyService service) {
        assertThat(service.hello().await().indefinitely()).isEqualTo("fallback");
    }
}
