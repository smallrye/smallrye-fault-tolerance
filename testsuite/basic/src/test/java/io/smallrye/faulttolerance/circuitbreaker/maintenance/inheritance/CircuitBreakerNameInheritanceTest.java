package io.smallrye.faulttolerance.circuitbreaker.maintenance.inheritance;

import static org.junit.Assert.assertNotNull;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.smallrye.faulttolerance.TestArchive;
import io.smallrye.faulttolerance.api.CircuitBreakerMaintenance;

@RunWith(Arquillian.class)
public class CircuitBreakerNameInheritanceTest {
    @Deployment
    public static JavaArchive createTestArchive() {
        return TestArchive.createBase(CircuitBreakerNameInheritanceTest.class)
                .addPackage(CircuitBreakerNameInheritanceTest.class.getPackage());
    }

    @Inject
    private CircuitBreakerMaintenance cb;

    @Test
    public void deploysWithoutError() {
        assertNotNull(cb);
    }
}
