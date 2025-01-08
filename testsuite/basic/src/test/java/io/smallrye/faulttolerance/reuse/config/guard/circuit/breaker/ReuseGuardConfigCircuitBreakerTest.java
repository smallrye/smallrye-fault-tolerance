package io.smallrye.faulttolerance.reuse.config.guard.circuit.breaker;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.CompletionException;

import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;
import io.smallrye.faulttolerance.util.WithSystemProperty;

@FaultToleranceBasicTest
@AddBeanClasses(MyFaultTolerance.class)
@WithSystemProperty(key = "smallrye.faulttolerance.\"my-fault-tolerance\".circuit-breaker.request-volume-threshold", value = "3")
@WithSystemProperty(key = "smallrye.faulttolerance.\"my-fault-tolerance\".circuit-breaker.delay", value = "1")
@WithSystemProperty(key = "smallrye.faulttolerance.\"my-fault-tolerance\".circuit-breaker.delay-unit", value = "seconds")
@WithSystemProperty(key = "smallrye.faulttolerance.\"my-fault-tolerance\".circuit-breaker.fail-on", value = "java.lang.IllegalArgumentException")
@WithSystemProperty(key = "smallrye.faulttolerance.\"my-fault-tolerance\".circuit-breaker.skip-on", value = "java.lang.IllegalStateException")
public class ReuseGuardConfigCircuitBreakerTest {
    @Test
    public void test(MyService service) throws InterruptedException {
        assertThatThrownBy(service::hello).isExactlyInstanceOf(IllegalStateException.class);
        assertThatThrownBy(service.helloCompletionStage().toCompletableFuture()::join)
                .isExactlyInstanceOf(CompletionException.class)
                .hasCauseExactlyInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(service.helloUni().subscribeAsCompletionStage()::join)
                .isExactlyInstanceOf(CompletionException.class)
                .hasCauseExactlyInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(service::hello).isExactlyInstanceOf(CircuitBreakerOpenException.class);

        Thread.sleep(2000);

        assertThatThrownBy(service.helloCompletionStage().toCompletableFuture()::join)
                .isExactlyInstanceOf(CompletionException.class)
                .hasCauseExactlyInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(service::hello).isExactlyInstanceOf(CircuitBreakerOpenException.class);
    }
}
