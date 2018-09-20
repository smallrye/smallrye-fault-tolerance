package io.smallrye.faulttolerance.metadata;

import static org.junit.Assert.assertEquals;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.smallrye.faulttolerance.TestArchive;

/**
 * See also https://github.com/smallrye/smallrye-fault-tolerance/issues/20
 *
 * @author Martin Kouba
 */
@RunWith(Arquillian.class)
public class RetryOnSubclassOverrideTest {

    @Deployment
    public static JavaArchive createTestArchive() {
        return TestArchive.createBase(RetryOnSubclassOverrideTest.class).addPackage(RetryOnSubclassOverrideTest.class.getPackage());
    }

    @Test
    public void testRetryOverriden(HelloService helloService) {
        BaseService.COUNTER.set(0);
        assertEquals("ok", helloService.retry());
        // 1 + 4 retries
        assertEquals(5, BaseService.COUNTER.get());
    }

}
