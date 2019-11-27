package io.smallrye.faulttolerance.tck;

import org.eclipse.microprofile.fault.tolerance.tck.CircuitBreakerExceptionHierarchyTest;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.event.suite.Before;

import com.netflix.hystrix.Hystrix;

public class HystrixCleanupObserver {
    /**
     * {@code CircuitBreakerExceptionHierarchyTest} expects that circuit breakers follow the lifecycle of the bean.
     * However, that isn't specified, and there's no specific TCK test for it. In this implementation,
     * all circuit breakers are in fact singletons, because Hystrix caches them in a {@code static Map}.
     * To work around the test expectation, we simply clean that Hystrix {@code Map} before each test in that class.
     *
     * @see <a href="https://github.com/eclipse/microprofile-fault-tolerance/issues/479">#479</a>
     */
    public void cleanup(@Observes Before event) {
        if (event.getTestClass().getJavaClass().equals(CircuitBreakerExceptionHierarchyTest.class)) {
            Hystrix.reset();
        }
    }
}
