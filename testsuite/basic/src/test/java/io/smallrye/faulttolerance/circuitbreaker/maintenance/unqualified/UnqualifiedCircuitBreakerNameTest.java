package io.smallrye.faulttolerance.circuitbreaker.maintenance.unqualified;

import javax.enterprise.inject.spi.DefinitionException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.ShouldThrowException;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.smallrye.faulttolerance.TestArchive;

@RunWith(Arquillian.class)
public class UnqualifiedCircuitBreakerNameTest {
    @Deployment
    @ShouldThrowException(DefinitionException.class)
    public static JavaArchive createTestArchive() {
        return TestArchive.createBase(UnqualifiedCircuitBreakerNameTest.class)
                .addPackage(UnqualifiedCircuitBreakerNameTest.class.getPackage());
    }

    @Test
    public void ignored() {
    }
}
