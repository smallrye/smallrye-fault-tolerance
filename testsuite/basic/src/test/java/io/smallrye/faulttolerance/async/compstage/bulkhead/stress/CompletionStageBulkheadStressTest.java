package io.smallrye.faulttolerance.async.compstage.bulkhead.stress;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.smallrye.faulttolerance.TestArchive;

@RunWith(Arquillian.class)
public class CompletionStageBulkheadStressTest {
    @Deployment
    public static JavaArchive createTestArchive() {
        return TestArchive.createBase(CompletionStageBulkheadStressTest.class)
                .addPackage(CompletionStageBulkheadStressTest.class.getPackage());
    }

    @Test
    public void stressTest(BulkheadService service) throws InterruptedException, ExecutionException {
        for (int i = 0; i < 50; i++) {
            service.reset();
            iteration(service);
        }
    }

    private void iteration(BulkheadService service) throws InterruptedException, ExecutionException {
        List<CompletionStage<String>> futures = new ArrayList<>();
        for (int i = 0; i < BulkheadService.BULKHEAD_SIZE + BulkheadService.BULKHEAD_QUEUE_SIZE; i++) {
            futures.add(service.hello());
        }

        Set<String> results = new HashSet<>();
        for (CompletionStage<String> future : futures) {
            results.add(future.toCompletableFuture().get());
        }

        assertEquals(BulkheadService.BULKHEAD_SIZE + BulkheadService.BULKHEAD_QUEUE_SIZE, results.size());

        for (int i = 0; i < BulkheadService.BULKHEAD_SIZE + BulkheadService.BULKHEAD_QUEUE_SIZE; i++) {
            assertTrue(results.contains("" + i));
        }
    }

    @Test
    public void stressTestWithWaiting(BulkheadService service) throws InterruptedException, ExecutionException {
        for (int i = 0; i < 50; i++) {
            service.reset();
            iterationWithWaiting(service);
        }
    }

    private void iterationWithWaiting(BulkheadService service) throws InterruptedException, ExecutionException {
        CountDownLatch startLatch = new CountDownLatch(BulkheadService.BULKHEAD_SIZE);
        CountDownLatch endLatch = new CountDownLatch(1);

        List<CompletionStage<String>> futures = new ArrayList<>();
        for (int i = 0; i < BulkheadService.BULKHEAD_SIZE + BulkheadService.BULKHEAD_QUEUE_SIZE; i++) {
            futures.add(service.helloWithWaiting(startLatch, endLatch));
        }

        startLatch.await();

        for (int i = 0; i < BulkheadService.BULKHEAD_SIZE * 2; i++) {
            ExecutionException exception = assertThrows(ExecutionException.class, () -> {
                service.helloWithWaiting(null, null).toCompletableFuture().get();
            });
            assertTrue(exception.getCause() instanceof BulkheadException);
        }

        endLatch.countDown();

        Set<String> results = new HashSet<>();
        for (CompletionStage<String> future : futures) {
            results.add(future.toCompletableFuture().get());
        }

        assertEquals(BulkheadService.BULKHEAD_SIZE + BulkheadService.BULKHEAD_QUEUE_SIZE, results.size());

        for (int i = 0; i < BulkheadService.BULKHEAD_SIZE + BulkheadService.BULKHEAD_QUEUE_SIZE; i++) {
            assertTrue(results.contains("" + i));
        }
    }
}
