package io.smallrye.faulttolerance.stereotype;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.IntStream;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.api.CircuitBreakerMaintenance;
import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
public class StereotypeFallbackOrderingTest {

    @Inject
    StereotypeService service;

    @Inject
    SameLevelService sameLevelService;

    @Inject
    NestedStereotypeService nestedStereotypeService;

    @Inject
    CircuitBreakerMaintenance maintenance;

    @BeforeEach
    void setUp() {
        service.resetCallCount();
        sameLevelService.resetCallCount();
        nestedStereotypeService.resetCallCount();
        maintenance.resetAll();
    }

    @Test
    public void retryIteratesBeforeFallback_whenAnnotationsCrossClassAndMethodLevels() {
        service.work();

        assertThat(service.getCallCount().get()).isEqualTo(3);
    }

    @Test
    public void circuitBreakerAccumulatesFailures_whenFallbackIsAtMethodLevel() {
        openCircuitBreaker();

        service.resetCallCount();
        service.work();

        assertThat(service.getCallCount().get()).isZero();
    }

    @Test
    public void retryIteratesBeforeFallback_whenAllAnnotationsAreAtMethodLevel() {
        sameLevelService.work();

        assertThat(sameLevelService.getCallCount().get()).isEqualTo(3);
    }

    @Test
    public void retryIteratesBeforeFallback_whenAnnotationComesFromNestedStereotype() {
        nestedStereotypeService.work();

        assertThat(nestedStereotypeService.getCallCount().get()).isEqualTo(3);
    }

    private void openCircuitBreaker() {
        IntStream.range(0, 3).forEach(ignored -> {
            service.resetCallCount();
            service.work();
        });
    }
}
