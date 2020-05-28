package io.smallrye.faulttolerance.async.circuitbreaker.state;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.smallrye.faulttolerance.TestArchive;
import io.smallrye.faulttolerance.api.CircuitBreakerState;

@RunWith(Arquillian.class)
public class CircuitBreakerStateTest {
    @Deployment
    public static JavaArchive createTestArchive() {
        return TestArchive.createBase(CircuitBreakerStateTest.class)
                .addPackage(CircuitBreakerStateTest.class.getPackage());
    }

    @Test
    public void testAsyncCircuitBreaker(HelloService helloService, HelloService.CB cb) {
        for (int i = 0; i < HelloService.THRESHOLD; i++) {
            assertThrows(IOException.class, () -> {
                helloService.hello(new IOException());
            });
        }

        assertEquals(CircuitBreakerState.OPEN, cb.state);

        await().atMost(HelloService.DELAY * 2, TimeUnit.MILLISECONDS)
                .ignoreException(CircuitBreakerOpenException.class)
                .untilAsserted(() -> {
                    assertEquals(HelloService.OK, helloService.hello(null));
                });

        assertEquals(CircuitBreakerState.CLOSED, cb.state);
    }
}
