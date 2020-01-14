package io.smallrye.faulttolerance.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.junit.Test;

public class InterfaceFaultToleranceOperationsTest {

    @Test
    public void testInterfaceMethods() throws NoSuchMethodException, SecurityException {
        FaultToleranceOperation ping = FaultToleranceOperation.of(Proxy.class, Proxy.class.getMethod("ping"));
        assertTrue(ping.isValid());
        CircuitBreakerConfig circuitBreaker = ping.getCircuitBreaker();
        assertNotNull(circuitBreaker);
        assertEquals(Integer.valueOf(2), (Integer) circuitBreaker.get(CircuitBreakerConfig.REQUEST_VOLUME_THRESHOLD));

        FaultToleranceOperation pong = FaultToleranceOperation.of(Proxy.class, Proxy.class.getMethod("pong"));
        assertTrue(pong.isValid());
        RetryConfig retry = pong.getRetry();
        assertNotNull(retry);
        assertFalse(pong.isAsync());
        assertEquals(Long.valueOf(1000), (Long) retry.get(RetryConfig.DELAY));
        circuitBreaker = pong.getCircuitBreaker();
        assertNotNull(circuitBreaker);
        assertEquals(Integer.valueOf(2), (Integer) circuitBreaker.get(CircuitBreakerConfig.REQUEST_VOLUME_THRESHOLD));
    }

    @CircuitBreaker(requestVolumeThreshold = 2)
    interface Proxy {

        void ping();

        @Retry(delay = 1000)
        int pong();

    }

}
