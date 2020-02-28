package io.smallrye.faulttolerance.fallback.varying;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.smallrye.faulttolerance.TestArchive;

@RunWith(Arquillian.class)
public class VaryingFallbackTest {
    @Deployment
    public static JavaArchive createTestArchive() {
        return TestArchive.createBase(VaryingFallbackTest.class)
                .addPackage(VaryingFallbackTest.class.getPackage());
    }

    @Inject
    HelloService service;

    @Test
    public void sync() throws IOException {
        assertEquals("Hello 1", service.hello(1));
        assertEquals("Hello 2", service.hello(2));
        assertEquals("Hello 3", service.hello(3));
    }

    @Test
    public void completionStageFailingSync() throws IOException, ExecutionException, InterruptedException {
        assertEquals("Hello 1", service.helloCompletionStageFailingSync(1).toCompletableFuture().get());
        assertEquals("Hello 2", service.helloCompletionStageFailingSync(2).toCompletableFuture().get());
        assertEquals("Hello 3", service.helloCompletionStageFailingSync(3).toCompletableFuture().get());
    }

    @Test
    public void completionStageFailingAsync() throws IOException, ExecutionException, InterruptedException {
        assertEquals("Hello 1", service.helloCompletionStageFailingAsync(1).toCompletableFuture().get());
        assertEquals("Hello 2", service.helloCompletionStageFailingAsync(2).toCompletableFuture().get());
        assertEquals("Hello 3", service.helloCompletionStageFailingAsync(3).toCompletableFuture().get());
    }

    @Test
    public void futureFailingSync() throws IOException, ExecutionException, InterruptedException {
        assertEquals("Hello 1", service.helloFutureFailingSync(1).get());
        assertEquals("Hello 2", service.helloFutureFailingSync(2).get());
        assertEquals("Hello 3", service.helloFutureFailingSync(3).get());
    }

    @Test
    public void futureFailingAsync() throws IOException, InterruptedException, ExecutionException {
        try {
            service.helloFutureFailingAsync(1).get();
            fail("Expected IOException");
        } catch (ExecutionException e) {
            if (!(e.getCause() instanceof IOException)) {
                throw e;
            }
        }

        try {
            service.helloFutureFailingAsync(2).get();
            fail("Expected IOException");
        } catch (ExecutionException e) {
            if (!(e.getCause() instanceof IOException)) {
                throw e;
            }
        }

        try {
            service.helloFutureFailingAsync(3).get();
            fail("Expected IOException");
        } catch (ExecutionException e) {
            if (!(e.getCause() instanceof IOException)) {
                throw e;
            }
        }
    }
}
