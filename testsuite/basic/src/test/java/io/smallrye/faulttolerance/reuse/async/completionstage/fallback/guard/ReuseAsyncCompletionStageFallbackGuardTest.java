package io.smallrye.faulttolerance.reuse.async.completionstage.fallback.guard;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
@AddBeanClasses(MyFaultTolerance.class)
public class ReuseAsyncCompletionStageFallbackGuardTest {
    @Test
    public void test(MyService service) {
        assertThat(service.hello())
                .succeedsWithin(1, TimeUnit.SECONDS)
                .isEqualTo("better fallback");
        assertThat(MyService.COUNTER).hasValue(3);
    }
}
