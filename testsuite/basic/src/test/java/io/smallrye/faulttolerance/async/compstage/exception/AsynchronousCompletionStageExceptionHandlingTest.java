package io.smallrye.faulttolerance.async.compstage.exception;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.ExecutionException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.smallrye.faulttolerance.TestArchive;

@RunWith(Arquillian.class)
public class AsynchronousCompletionStageExceptionHandlingTest {
    @Deployment
    public static JavaArchive createTestArchive() {
        return TestArchive.createBase(AsynchronousCompletionStageExceptionHandlingTest.class)
                .addPackage(AsynchronousCompletionStageExceptionHandlingTest.class.getPackage());
    }

    @Test
    public void test(AsyncHelloService helloService) throws InterruptedException, ExecutionException {
        assertEquals("hello fallback", helloService.hello().toCompletableFuture().get());
        assertEquals(5, AsyncHelloService.COUNTER.get());
    }
}
