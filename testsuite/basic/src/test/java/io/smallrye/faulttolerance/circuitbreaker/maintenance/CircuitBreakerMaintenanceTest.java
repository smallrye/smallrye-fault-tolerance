package io.smallrye.faulttolerance.circuitbreaker.maintenance;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.smallrye.faulttolerance.TestArchive;
import io.smallrye.faulttolerance.api.CircuitBreakerMaintenance;
import io.smallrye.faulttolerance.api.CircuitBreakerState;

@RunWith(Arquillian.class)
public class CircuitBreakerMaintenanceTest {
    @Deployment
    public static JavaArchive createTestArchive() {
        return TestArchive.createBase(CircuitBreakerMaintenanceTest.class)
                .addPackage(CircuitBreakerMaintenanceTest.class.getPackage());
    }

    @Inject
    private HelloService helloService;

    @Inject
    private CircuitBreakerMaintenance cb;

    @Before
    public void reset() {
        cb.resetAll();
    }

    @Test
    public void readCircuitBreakerState() {
        assertEquals(CircuitBreakerState.CLOSED, cb.currentState("hello"));

        for (int i = 0; i < HelloService.THRESHOLD; i++) {
            assertThrows(IOException.class, () -> {
                helloService.hello(new IOException());
            });
        }

        assertEquals(CircuitBreakerState.OPEN, cb.currentState("hello"));

        await().atMost(HelloService.DELAY * 2, TimeUnit.MILLISECONDS)
                .ignoreException(CircuitBreakerOpenException.class)
                .untilAsserted(() -> {
                    assertEquals(HelloService.OK, helloService.hello(null));
                });

        assertEquals(CircuitBreakerState.CLOSED, cb.currentState("hello"));
    }

    @Test
    public void resetCircuitBreaker() throws Exception {
        testCircuitBreakerReset(() -> {
            cb.reset("hello");
        });
    }

    @Test
    public void resetAllCircuitBreakers() throws Exception {
        testCircuitBreakerReset(() -> {
            cb.resetAll();
        });
    }

    private void testCircuitBreakerReset(Runnable resetFunction) throws Exception {
        assertEquals(CircuitBreakerState.CLOSED, cb.currentState("hello"));

        for (int i = 0; i < HelloService.THRESHOLD; i++) {
            assertThrows(IOException.class, () -> {
                helloService.hello(new IOException());
            });
        }

        assertEquals(CircuitBreakerState.OPEN, cb.currentState("hello"));

        assertThrows(CircuitBreakerOpenException.class, () -> {
            helloService.hello(null);
        });

        resetFunction.run();

        assertEquals(CircuitBreakerState.CLOSED, cb.currentState("hello"));

        assertEquals(HelloService.OK, helloService.hello(null));
    }

    @Test
    public void undefinedCircuitBreaker() {
        assertThrows(IllegalArgumentException.class, () -> {
            cb.currentState("undefined");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            cb.reset("undefined");
        });
    }
}
