package io.smallrye.faulttolerance.retry.computed.delay;

import static org.assertj.core.api.Assertions.assertThat;

import org.jboss.weld.junit.jupiter.auto.AddBeanClasses;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
@AddBeanClasses(MyComputedDelayDependency.class)
public class ComputedDelayTest {
    @BeforeEach
    public void setUp() {
        MyComputedDelayHandler.dependencyIds.clear();
        ComputedDelayService.invocationTimes.clear();
        ComputedDelayService.attempts.set(0);
    }

    @Test
    public void testComputedDelay(ComputedDelayService service) {
        service.retryWithComputedDelay();

        assertThat(ComputedDelayService.attempts.get()).isEqualTo(4);
        assertThat(MyComputedDelayHandler.dependencyIds).hasSize(3);

        long delay1 = ComputedDelayService.invocationTimes.get(1) - ComputedDelayService.invocationTimes.get(0);
        long delay2 = ComputedDelayService.invocationTimes.get(2) - ComputedDelayService.invocationTimes.get(1);
        long delay3 = ComputedDelayService.invocationTimes.get(3) - ComputedDelayService.invocationTimes.get(2);
        assertThat(delay1).isGreaterThanOrEqualTo(100); // should be 150ms
        assertThat(delay2).isGreaterThanOrEqualTo(100); // should be 150ms
        assertThat(delay3).isGreaterThanOrEqualTo(100); // should be 150ms
    }

    @Test
    public void testComputedDelayFallbackToExponential(ComputedDelayService service) {
        service.retryWithFallbackToExponential();

        assertThat(ComputedDelayService.attempts.get()).isEqualTo(4);
        assertThat(MyComputedDelayHandler.dependencyIds).hasSize(3);

        long delay1 = ComputedDelayService.invocationTimes.get(1) - ComputedDelayService.invocationTimes.get(0);
        long delay2 = ComputedDelayService.invocationTimes.get(2) - ComputedDelayService.invocationTimes.get(1);
        long delay3 = ComputedDelayService.invocationTimes.get(3) - ComputedDelayService.invocationTimes.get(2);
        assertThat(delay1).isGreaterThanOrEqualTo(30); // should be 50ms
        assertThat(delay2).isGreaterThanOrEqualTo(70); // should be 100ms
        assertThat(delay3).isGreaterThanOrEqualTo(150); // should be 200ms
    }

    @Test
    public void testComputedDelayNonPositiveDelay(ComputedDelayService service) {
        service.retryWithNegativeDelay();

        assertThat(ComputedDelayService.attempts.get()).isEqualTo(4);
        assertThat(MyComputedDelayHandler.dependencyIds).hasSize(3);

        long delay1 = ComputedDelayService.invocationTimes.get(1) - ComputedDelayService.invocationTimes.get(0);
        long delay2 = ComputedDelayService.invocationTimes.get(2) - ComputedDelayService.invocationTimes.get(1);
        long delay3 = ComputedDelayService.invocationTimes.get(3) - ComputedDelayService.invocationTimes.get(2);
        assertThat(delay1).isGreaterThanOrEqualTo(30); // should be 50ms
        assertThat(delay2).isGreaterThanOrEqualTo(30); // should be 50ms
        assertThat(delay3).isGreaterThanOrEqualTo(30); // should be 50ms
    }

    @Test
    public void testComputedDelayExceptionInHandler(ComputedDelayService service) {
        service.retryWithExceptionInHandler();

        assertThat(ComputedDelayService.attempts.get()).isEqualTo(4);
        assertThat(MyComputedDelayHandler.dependencyIds).hasSize(3);

        long delay1 = ComputedDelayService.invocationTimes.get(1) - ComputedDelayService.invocationTimes.get(0);
        long delay2 = ComputedDelayService.invocationTimes.get(2) - ComputedDelayService.invocationTimes.get(1);
        long delay3 = ComputedDelayService.invocationTimes.get(3) - ComputedDelayService.invocationTimes.get(2);
        assertThat(delay1).isGreaterThanOrEqualTo(30); // should be 50ms
        assertThat(delay2).isGreaterThanOrEqualTo(30); // should be 50ms
        assertThat(delay3).isGreaterThanOrEqualTo(30); // should be 50ms
    }
}
