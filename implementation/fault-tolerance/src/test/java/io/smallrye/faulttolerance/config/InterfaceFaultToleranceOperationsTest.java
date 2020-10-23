package io.smallrye.faulttolerance.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.junit.jupiter.api.Test;

public class InterfaceFaultToleranceOperationsTest {
    @Test
    public void testInterfaceMethods() throws NoSuchMethodException, SecurityException {
        FaultToleranceOperation ping = FaultToleranceOperation.of(Proxy.class, Proxy.class.getMethod("ping"));
        assertThat(ping.isValid()).isTrue();
        CircuitBreakerConfig circuitBreaker = ping.getCircuitBreaker();
        assertThat(circuitBreaker).isNotNull();
        assertThat((Integer) circuitBreaker.get(CircuitBreakerConfig.REQUEST_VOLUME_THRESHOLD)).isEqualTo(2);

        FaultToleranceOperation pong = FaultToleranceOperation.of(Proxy.class, Proxy.class.getMethod("pong"));
        assertThat(pong.isValid()).isTrue();
        RetryConfig retry = pong.getRetry();
        assertThat(retry).isNotNull();
        assertThat(pong.isAsync()).isFalse();
        assertThat((Long) retry.get(RetryConfig.DELAY)).isEqualTo(1000);
        circuitBreaker = pong.getCircuitBreaker();
        assertThat(circuitBreaker).isNotNull();
        assertThat((Integer) circuitBreaker.get(CircuitBreakerConfig.REQUEST_VOLUME_THRESHOLD)).isEqualTo(2);
    }

    @CircuitBreaker(requestVolumeThreshold = 2)
    interface Proxy {
        void ping();

        @Retry(delay = 1000)
        int pong();
    }
}
