package io.smallrye.faulttolerance.reuse.config.guard.circuit.breaker.disable;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.CompletionException;

import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;
import io.smallrye.faulttolerance.util.WithSystemProperty;

@FaultToleranceBasicTest
@AddBeanClasses(MyFaultTolerance.class)
@WithSystemProperty(key = "smallrye.faulttolerance.\"my-fault-tolerance\".circuit-breaker.enabled", value = "false")
public class ReuseGuardConfigCircuitBreakerDisableTest {
    @Test
    public void test(MyService service) throws InterruptedException {
        for (int i = 0; i < 20; i++) {
            assertThatThrownBy(service::hello).isExactlyInstanceOf(IllegalStateException.class);
            assertThatThrownBy(service.helloCompletionStage().toCompletableFuture()::join)
                    .isExactlyInstanceOf(CompletionException.class)
                    .hasCauseExactlyInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(service.helloUni().subscribeAsCompletionStage()::join)
                    .isExactlyInstanceOf(CompletionException.class)
                    .hasCauseExactlyInstanceOf(IllegalArgumentException.class);
        }
    }
}
